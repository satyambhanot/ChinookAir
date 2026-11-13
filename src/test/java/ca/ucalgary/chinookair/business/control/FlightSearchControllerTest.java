package ca.ucalgary.chinookair.business.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Airline;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.SeatLayout;
import ca.ucalgary.chinookair.data.DaoFactory;
import ca.ucalgary.chinookair.data.TestDatabase;
import ca.ucalgary.chinookair.data.TestWorld;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FlightSearchControllerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 12, 1);
    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private Connection db;
    private TestWorld world;
    private DaoFactory daos;
    private FlightSearchController controller;

    @BeforeEach
    void open() {
        db = TestDatabase.inMemory();
        world = new TestWorld(db);
        daos = DaoFactory.create(db);
        controller = new FlightSearchController(daos.flightDao(), daos.seatDao(), CLOCK);
        // three flights on the 1st (08:00 has already left, 12:00 and 18:00 have not), one on the 2nd
        saveFlight("AC100", world.ac, 1, 8);
        saveFlight("WS200", world.ws, 1, 12);
        saveFlight("AC300", world.ac, 1, 18);
        saveFlight("AC400", world.ac, 2, 8);
    }

    @AfterEach
    void close() throws SQLException {
        db.close();
    }

    private Flight saveFlight(String number, Airline airline, int day,
                              int hour) {
        Flight flight = daos.flightDao().save(world.flight(number, world.yycYvr, airline, world.small,
                LocalDateTime.of(2026, 12, day, hour, 0)));
        daos.seatDao().createSeats(flight.id(), SeatLayout.generate(world.small));
        return flight;
    }

    private static SearchCriteria search(String origin, String destination, LocalDate date, String airline) {
        return new SearchCriteria(origin, destination, date, airline);
    }

    private static List<String> numbers(List<FlightOffer> offers) {
        return offers.stream().map(o -> o.flight().flightNumber()).toList();
    }

    private String messageOf(SearchCriteria criteria) {
        return assertThrows(IllegalArgumentException.class, () -> controller.search(criteria)).getMessage();
    }

    // ---- search results

    @Test
    void flightsThatAlreadyLeftTodayAreLeftOut() {
        List<FlightOffer> offers = controller.search(search("YYC", "YVR", TODAY, null));
        assertEquals(List.of("WS200", "AC300"), numbers(offers));
    }

    @Test
    void lowercaseAndSpacesGiveTheSameResult() {
        assertEquals(numbers(controller.search(search("YYC", "YVR", TODAY, null))),
                numbers(controller.search(search(" yyc ", "yvr", TODAY, null))));
    }

    @Test
    void anotherDayHasItsOwnFlights() {
        assertEquals(List.of("AC400"), numbers(controller.search(search("YYC", "YVR", TODAY.plusDays(1), null))));
        assertTrue(controller.search(search("YYC", "YVR", TODAY.plusDays(5), null)).isEmpty());
    }

    @Test
    void theAirlineFilterKeepsOnlyThatAirline() {
        assertEquals(List.of("AC300"), numbers(controller.search(search("YYC", "YVR", TODAY, "AC"))));
        assertEquals(List.of("AC300"), numbers(controller.search(search("YYC", "YVR", TODAY, " ac "))));
        assertTrue(controller.search(search("YYC", "YVR", TODAY, "PD")).isEmpty());
        assertEquals(2, controller.search(search("YYC", "YVR", TODAY, "")).size());
    }

    @Test
    void theOtherDirectionAndOtherRoutesAreNotMixedIn() {
        assertTrue(controller.search(search("YVR", "YYC", TODAY, null)).isEmpty());
        assertTrue(controller.search(search("YYC", "YEG", TODAY, null)).isEmpty());
    }

    @Test
    void anOfferShowsTheSeatsLeftAndTheEconomyPrice() {
        FlightOffer offer = controller.search(search("YYC", "YVR", TODAY, "AC")).get(0);
        assertEquals(12, offer.seatsLeft());
        assertEquals(new BigDecimal("200.00"), offer.fromPrice());
        assertEquals("AC300", offer.flight().flightNumber());
    }

    @Test
    void seatsLeftDropsWhenASeatIsReserved() {
        long id = controller.search(search("YYC", "YVR", TODAY, "AC")).get(0).flight().id();
        world.reserve("CA000001", id, "1A", "CONFIRMED");
        assertEquals(11, controller.search(search("YYC", "YVR", TODAY, "AC")).get(0).seatsLeft());
        assertEquals(daos.seatDao().countAvailable(id), controller.details(id).seatsLeft());
    }

    @Test
    void aCancelledReservationDoesNotTakeASeat() {
        long id = controller.search(search("YYC", "YVR", TODAY, "AC")).get(0).flight().id();
        world.reserve("CA000001", id, "1A", "CANCELLED");
        assertEquals(12, controller.search(search("YYC", "YVR", TODAY, "AC")).get(0).seatsLeft());
    }

    @Test
    void aFullFlightIsStillListedWithNoSeatsLeft() {
        long id = controller.search(search("YYC", "YVR", TODAY, "AC")).get(0).flight().id();
        String[] seats = {"1A", "1B", "1C", "1D", "2A", "2B", "2C", "2D", "3A", "3B", "3C", "3D"};
        for (int i = 0; i < seats.length; i++) {
            world.reserve("CA0000" + (10 + i), id, seats[i], "CONFIRMED");
        }
        assertEquals(0, controller.search(search("YYC", "YVR", TODAY, "AC")).get(0).seatsLeft());
    }

    @Test
    void theFromPriceKeepsTheCentsOfTheBasePrice() {
        Flight cheap = daos.flightDao().save(new Flight(null, "PD500", world.yycYeg, world.pd, world.small,
                LocalDateTime.of(2026, 12, 1, 15, 0), LocalDateTime.of(2026, 12, 1, 16, 0), new BigDecimal("199.99")));
        daos.seatDao().createSeats(cheap.id(), SeatLayout.generate(world.small));
        assertEquals(new BigDecimal("199.99"), controller.search(search("YYC", "YEG", TODAY, null)).get(0).fromPrice());
    }

    // ---- validation

    @Test
    void originAndDestinationMustDiffer() {
        assertTrue(messageOf(search("YYC", "YYC", TODAY, null)).contains("must be different"));
        assertTrue(messageOf(search("yyc", " YYC", TODAY, null)).contains("must be different"));
    }

    @Test
    void theDateCannotBeInThePastButTodayIsFine() {
        assertTrue(messageOf(search("YYC", "YVR", LocalDate.of(2026, 11, 30), null)).contains("cannot be in the past"));
        assertEquals(2, controller.search(search("YYC", "YVR", TODAY, null)).size());
    }

    @Test
    void theAirportCodesNeedThreeLetters() {
        assertTrue(messageOf(search("YY", "YVR", TODAY, null)).contains("3 letters"));
        assertTrue(messageOf(search("YYC", "YVRX", TODAY, null)).contains("3 letters"));
        assertTrue(messageOf(search("Y1C", "YVR", TODAY, null)).contains("3 letters"));
    }

    @Test
    void originAndDestinationAreRequired() {
        assertTrue(messageOf(search(null, "YVR", TODAY, null)).contains("Origin is required"));
        assertTrue(messageOf(search("  ", "YVR", TODAY, null)).contains("Origin is required"));
        assertTrue(messageOf(search("YYC", null, TODAY, null)).contains("Destination is required"));
    }

    @Test
    void theDateIsRequired() {
        assertTrue(messageOf(search("YYC", "YVR", null, null)).contains("Date is required"));
    }

    @Test
    void theAirlineCodeNeedsTwoLetters() {
        assertTrue(messageOf(search("YYC", "YVR", TODAY, "AIR")).contains("2 letters"));
        assertTrue(messageOf(search("YYC", "YVR", TODAY, "A1")).contains("2 letters"));
    }

    @Test
    void criteriaAreRequired() {
        assertThrows(IllegalArgumentException.class, () -> controller.search(null));
    }

    // ---- details

    @Test
    void detailsGivesOneFlightWithItsSeatsAndPrice() {
        long id = daos.flightDao().search("YYC", "YVR", TODAY, "WS").get(0).id();
        FlightOffer offer = controller.details(id);
        assertEquals("WS200", offer.flight().flightNumber());
        assertEquals(12, offer.seatsLeft());
        assertEquals(new BigDecimal("200.00"), offer.fromPrice());
    }

    @Test
    void detailsOfAnUnknownFlightIsAnError() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> controller.details(999));
        assertEquals("That flight does not exist", e.getMessage());
    }
}
