package ca.ucalgary.chinookair.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.SeatLayout;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqliteFlightDaoTest {

    private static final LocalDate DAY = LocalDate.of(2026, 12, 1);

    private Connection db;
    private TestWorld world;
    private FlightDao dao;

    @BeforeEach
    void open() {
        db = TestDatabase.inMemory();
        world = new TestWorld(db);
        dao = new SqliteFlightDao(db);
    }

    @AfterEach
    void close() throws SQLException {
        db.close();
    }

    private static LocalDateTime at(int day, int hour, int minute) {
        return LocalDateTime.of(2026, 12, day, hour, minute);
    }

    /** Three YYC to YVR flights on 12-01 (AC 08:00, WS 12:00, AC 18:00), one to YEG and one on 12-02. */
    private void saveTheTestFlights() {
        dao.save(world.flight("AC100", world.yycYvr, world.ac, world.big, at(1, 18, 0)));
        dao.save(world.flight("AC101", world.yycYvr, world.ac, world.big, at(1, 8, 0)));
        dao.save(world.flight("WS200", world.yycYvr, world.ws, world.big, at(1, 12, 0)));
        dao.save(world.flight("AC300", world.yycYeg, world.ac, world.small, at(1, 9, 0)));
        dao.save(world.flight("AC102", world.yycYvr, world.ac, world.big, at(2, 8, 0)));
    }

    private static List<String> numbers(List<Flight> flights) {
        return flights.stream().map(Flight::flightNumber).toList();
    }

    // ---- save, find, update

    @Test
    void aSavedFlightGetsAnIdAndIsRebuiltInFull() {
        Flight saved = dao.save(world.flight("AC123", world.yycYvr, world.ac, world.big, at(1, 8, 0)));
        assertNotNull(saved.id());
        Flight found = dao.findById(saved.id()).orElseThrow();
        assertEquals(saved, found);
        assertEquals("Vancouver", found.route().destination().city());
        assertEquals("Air Canada", found.airline().name());
        assertEquals(180, found.aircraft().seatCount());
        assertEquals(new BigDecimal("200.00"), found.basePrice());
        assertEquals(at(1, 10, 0), found.arrival());
    }

    @Test
    void anUnknownIdGivesAnEmptyResult() {
        assertTrue(dao.findById(99).isEmpty());
    }

    @Test
    void theFlightNumberMustBeNew() {
        dao.save(world.flight("AC123", world.yycYvr, world.ac, world.big, at(1, 8, 0)));
        assertThrows(DataAccessException.class,
                () -> dao.save(world.flight("AC123", world.yycYvr, world.ac, world.big, at(2, 8, 0))));
    }

    @Test
    void aFlightNeedsASavedRouteAndAircraft() {
        Flight unsavedRoute = new Flight(null, "AC999", TestFixtures.route("YYC", "YVR"),
                world.ac, world.big, at(1, 8, 0), at(1, 10, 0), new BigDecimal("100.00"));
        assertThrows(IllegalArgumentException.class, () -> dao.save(unsavedRoute));
        Flight saved = dao.save(world.flight("AC123", world.yycYvr, world.ac, world.big, at(1, 8, 0)));
        assertThrows(IllegalArgumentException.class, () -> dao.save(saved));
    }

    @Test
    void existsByFlightNumberIsTrueOnlyAfterSaving() {
        assertFalse(dao.existsByFlightNumber("AC123"));
        dao.save(world.flight("AC123", world.yycYvr, world.ac, world.big, at(1, 8, 0)));
        assertTrue(dao.existsByFlightNumber("AC123"));
        assertTrue(dao.existsByFlightNumber("ac123"));
        assertFalse(dao.existsByFlightNumber("AC124"));
        assertFalse(dao.existsByFlightNumber(null));
    }

    @Test
    void updateChangesTheFlightButKeepsItsId() {
        Flight saved = dao.save(world.flight("AC123", world.yycYvr, world.ac, world.big, at(1, 8, 0)));
        Flight changed = new Flight(saved.id(), "WS555", world.yycYeg, world.ws, world.small, at(3, 9, 30),
                at(3, 11, 0), new BigDecimal("150.50"));
        dao.update(changed);
        assertEquals(changed, dao.findById(saved.id()).orElseThrow());
        assertFalse(dao.existsByFlightNumber("AC123"));
    }

    @Test
    void updateNeedsASavedFlight() {
        Flight notSaved = world.flight("AC123", world.yycYvr, world.ac, world.big, at(1, 8, 0));
        assertThrows(IllegalArgumentException.class, () -> dao.update(notSaved));
        Flight ghost = new Flight(99L, "AC123", world.yycYvr, world.ac, world.big, at(1, 8, 0), at(1, 10, 0),
                new BigDecimal("100.00"));
        assertThrows(DataAccessException.class, () -> dao.update(ghost));
    }

    // ---- search

    @Test
    void searchReturnsTheFlightsOfTheDayEarliestFirst() {
        saveTheTestFlights();
        assertEquals(List.of("AC101", "WS200", "AC100"), numbers(dao.search("YYC", "YVR", DAY, null)));
    }

    @Test
    void searchCanFilterByAirline() {
        saveTheTestFlights();
        assertEquals(List.of("AC101", "AC100"), numbers(dao.search("YYC", "YVR", DAY, "AC")));
        assertTrue(dao.search("YYC", "YVR", DAY, "PD").isEmpty());
        assertEquals(3, dao.search("YYC", "YVR", DAY, " ").size());
        assertEquals(List.of("WS200"), numbers(dao.search("YYC", "YVR", DAY, "ws")));
    }

    @Test
    void searchKnowsTheRouteAndItsDirection() {
        saveTheTestFlights();
        assertEquals(List.of("AC300"), numbers(dao.search("YYC", "YEG", DAY, null)));
        assertTrue(dao.search("YVR", "YYC", DAY, null).isEmpty());
        assertEquals(3, dao.search("yyc", " yvr ", DAY, null).size());
    }

    @Test
    void searchOnADayWithoutFlightsIsEmpty() {
        saveTheTestFlights();
        assertTrue(dao.search("YYC", "YVR", LocalDate.of(2026, 12, 3), null).isEmpty());
        assertEquals(List.of("AC102"), numbers(dao.search("YYC", "YVR", LocalDate.of(2026, 12, 2), null)));
    }

    @Test
    void theEdgesOfTheDayBelongToTheRightDay() {
        dao.save(world.flight("AC201", world.yycYvr, world.ac, world.big, at(1, 23, 59)));
        dao.save(world.flight("AC202", world.yycYvr, world.ac, world.big, at(2, 0, 0)));
        dao.save(world.flight("AC203", world.yycYvr, world.ac, world.big, at(1, 0, 0)));
        assertEquals(List.of("AC203", "AC201"), numbers(dao.search("YYC", "YVR", DAY, null)));
        assertEquals(List.of("AC202"), numbers(dao.search("YYC", "YVR", DAY.plusDays(1), null)));
    }

    @Test
    void searchNeedsItsThreeMainInputs() {
        assertThrows(IllegalArgumentException.class, () -> dao.search(null, "YVR", DAY, null));
        assertThrows(IllegalArgumentException.class, () -> dao.search("YYC", null, DAY, null));
        assertThrows(IllegalArgumentException.class, () -> dao.search("YYC", "YVR", null, null));
    }

    // ---- schedules and aircraft

    @Test
    void findDepartingBetweenIncludesTheStartAndExcludesTheEnd() {
        saveTheTestFlights();
        assertEquals(List.of("AC101", "AC300", "WS200"), numbers(dao.findDepartingBetween(at(1, 8, 0), at(1, 18, 0))));
        assertEquals(List.of("AC101", "AC300", "WS200", "AC100", "AC102"),
                numbers(dao.findDepartingBetween(at(1, 0, 0), at(3, 0, 0))));
        assertTrue(dao.findDepartingBetween(at(5, 0, 0), at(6, 0, 0)).isEmpty());
    }

    @Test
    void findByAircraftListsOnlyThatAircraftEarliestFirst() {
        saveTheTestFlights();
        assertEquals(List.of("AC300"), numbers(dao.findByAircraft(world.small.id())));
        assertEquals(List.of("AC101", "WS200", "AC100", "AC102"), numbers(dao.findByAircraft(world.big.id())));
    }

    // ---- delete and archive

    @Test
    void deleteRemovesAFlightWithoutReservationsAndItsSeats() {
        Flight saved = dao.save(world.flight("AC123", world.yycYvr, world.ac, world.small, at(1, 8, 0)));
        new SqliteSeatDao(db).createSeats(saved.id(), SeatLayout.generate(world.small));
        dao.delete(saved.id());
        assertTrue(dao.findById(saved.id()).isEmpty());
        assertEquals(0, new SqliteSeatDao(db).findByFlight(saved.id()).size());
    }

    @Test
    void deleteIsRefusedWhileAReservationExists() {
        Flight saved = dao.save(world.flight("AC123", world.yycYvr, world.ac, world.small, at(1, 8, 0)));
        new SqliteSeatDao(db).createSeats(saved.id(), SeatLayout.generate(world.small));
        world.reserve("CA000001", saved.id(), "1A", "CANCELLED");
        assertThrows(DataAccessException.class, () -> dao.delete(saved.id()));
        assertTrue(dao.findById(saved.id()).isPresent());
        assertThrows(DataAccessException.class, () -> dao.delete(99));
    }

    @Test
    void anArchivedFlightIsHiddenFromSearchesButStillFound() {
        saveTheTestFlights();
        long id = dao.search("YYC", "YVR", DAY, "WS").get(0).id();
        new SqliteSeatDao(db).createSeats(id, SeatLayout.generate(world.small));
        world.reserve("CA000001", id, "1A", "CONFIRMED");

        dao.archive(id);

        assertEquals(List.of("AC101", "AC100"), numbers(dao.search("YYC", "YVR", DAY, null)));
        assertFalse(numbers(dao.findDepartingBetween(at(1, 0, 0), at(3, 0, 0))).contains("WS200"));
        assertFalse(numbers(dao.findByAircraft(world.big.id())).contains("WS200"));
        assertEquals("WS200", dao.findById(id).orElseThrow().flightNumber());
        assertTrue(dao.existsByFlightNumber("WS200"));
        assertEquals(1, new SqliteSeatDao(db).findByFlight(id).stream().filter(s -> !s.available()).count());
        assertThrows(DataAccessException.class, () -> dao.archive(99));
    }
}
