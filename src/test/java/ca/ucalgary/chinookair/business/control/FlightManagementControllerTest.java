package ca.ucalgary.chinookair.business.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Administrator;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.FlightAgent;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.ReservationStatus;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FlightManagementControllerTest {

    private static final String HASH = "pbkdf2$1$AAAA$BBBB";
    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    private static final LocalDate DAY = LocalDate.of(2026, 12, 5);

    private Connection db;
    private TestWorld world;
    private DaoFactory daos;
    private FlightManagementController controller;
    private Administrator admin;
    private Customer jo;
    private FlightAgent agent;
    private int numbers;

    @BeforeEach
    void open() {
        db = TestDatabase.inMemory();
        world = new TestWorld(db);
        daos = DaoFactory.create(db);
        controller = new FlightManagementController(daos.flightDao(), daos.seatDao(), daos.routeDao(),
                daos.airlineDao(), daos.aircraftDao(), daos.reservationDao(), CLOCK);
        admin = (Administrator) daos.userDao().save(new Administrator(null, "admin", HASH));
        agent = (FlightAgent) daos.userDao().save(new FlightAgent(null, "agent1", HASH));
        jo = daos.customerDao().findById(1).orElseThrow();
    }

    @AfterEach
    void close() throws SQLException {
        db.close();
    }

    // ---- fixtures

    private FlightDetails details(String number, long aircraftId, int fromHour, int toHour) {
        return new FlightDetails(number, world.yycYvr.id(), "AC", aircraftId, DAY.atTime(fromHour, 0),
                DAY.atTime(toHour, 0), new BigDecimal("200.00"));
    }

    private Flight add(String number, long aircraftId, int fromHour, int toHour) {
        return controller.addFlight(admin, details(number, aircraftId, fromHour, toHour));
    }

    private void reserve(Flight flight, String seat, String status) {
        Reservation r = Reservation.restore(null, "CA00000" + (++numbers), jo, flight, seat,
                seat == null ? null : new BigDecimal("200.00"), LocalDateTime.of(2026, 11, 20, 9, 0),
                ReservationStatus.valueOf(status));
        daos.reservationDao().save(r);
    }

    private String messageOf(Runnable action) {
        return assertThrows(IllegalArgumentException.class, action::run).getMessage();
    }

    // ---- add

    @Test
    void theAdminAddsAFlightAndItsSeatsAreCreated() {
        Flight flight = add("AC123", world.small.id(), 8, 10);
        assertNotNull(flight.id());
        assertEquals("AC123", flight.flightNumber());
        assertEquals(12, daos.seatDao().findByFlight(flight.id()).size());
        assertEquals(12, daos.seatDao().countAvailable(flight.id()));
        assertEquals(1, daos.flightDao().search("YYC", "YVR", DAY, null).size());
        assertEquals("Vancouver", flight.route().destination().city());
    }

    @Test
    void theFlightNumberIsStoredInUppercase() {
        assertEquals("AC124", add("ac124", world.small.id(), 8, 10).flightNumber());
    }

    @Test
    void onlyAnAdministratorMayAdd() {
        FlightDetails d = details("AC123", world.small.id(), 8, 10);
        assertThrows(AccessDeniedException.class, () -> controller.addFlight(jo, d));
        assertThrows(AccessDeniedException.class, () -> controller.addFlight(agent, d));
        assertThrows(AccessDeniedException.class, () -> controller.addFlight(null, d));
        assertTrue(daos.flightDao().search("YYC", "YVR", DAY, null).isEmpty());
    }

    @Test
    void aSecondFlightWithTheSameNumberIsRefused() {
        add("AC123", world.small.id(), 8, 10);
        assertTrue(messageOf(() -> add("AC123", world.big.id(), 12, 14)).contains("Flight number already exists"));
        assertTrue(messageOf(() -> add("ac123", world.big.id(), 12, 14)).contains("Flight number already exists"));
    }

    @Test
    void theNumberAndPriceMustBeValid() {
        assertThrows(IllegalArgumentException.class, () -> add("A1", world.small.id(), 8, 10));
        assertThrows(IllegalArgumentException.class, () -> add("AC12345", world.small.id(), 8, 10));
        assertThrows(IllegalArgumentException.class, () -> controller.addFlight(admin, new FlightDetails("AC123",
                world.yycYvr.id(), "AC", world.small.id(), DAY.atTime(8, 0), DAY.atTime(10, 0), BigDecimal.ZERO)));
        assertThrows(IllegalArgumentException.class, () -> controller.addFlight(admin, null));
    }

    @Test
    void theArrivalMustBeAfterTheDeparture() {
        assertThrows(IllegalArgumentException.class, () -> add("AC123", world.small.id(), 10, 8));
        assertThrows(IllegalArgumentException.class, () -> add("AC123", world.small.id(), 10, 10));
    }

    @Test
    void theDepartureMustBeInTheFuture() {
        FlightDetails past = new FlightDetails("AC123", world.yycYvr.id(), "AC", world.small.id(),
                LocalDateTime.of(2026, 11, 30, 8, 0), LocalDateTime.of(2026, 11, 30, 10, 0), new BigDecimal("200"));
        assertTrue(messageOf(() -> controller.addFlight(admin, past)).contains("must be in the future"));
        FlightDetails now = new FlightDetails("AC123", world.yycYvr.id(), "AC", world.small.id(),
                LocalDateTime.of(2026, 12, 1, 9, 0), LocalDateTime.of(2026, 12, 1, 11, 0), new BigDecimal("200"));
        assertTrue(messageOf(() -> controller.addFlight(admin, now)).contains("must be in the future"));
    }

    @Test
    void anAircraftCannotBeOnTwoOverlappingFlights() {
        add("AC100", world.small.id(), 8, 10);
        assertTrue(messageOf(() -> add("AC101", world.small.id(), 9, 11)).contains("Aircraft is already assigned"));
        assertTrue(messageOf(() -> add("AC102", world.small.id(), 7, 9)).contains("Aircraft is already assigned"));
        assertTrue(messageOf(() -> add("AC103", world.small.id(), 7, 12)).contains("Aircraft is already assigned"));
    }

    @Test
    void touchingFlightsAndOtherAircraftAreAccepted() {
        add("AC100", world.small.id(), 8, 10);
        assertNotNull(add("AC101", world.small.id(), 10, 12).id());
        assertNotNull(add("AC102", world.big.id(), 9, 11).id());
        assertNotNull(add("AC103", world.small.id(), 6, 8).id());
    }

    @Test
    void aRemovedFlightNoLongerBlocksItsAircraft() {
        Flight old = add("AC100", world.small.id(), 8, 10);
        reserve(old, "1A", "CANCELLED");
        controller.removeFlight(admin, old.id());
        assertNotNull(add("AC101", world.small.id(), 9, 11).id());
    }

    @Test
    void theRouteTheAirlineAndTheAircraftMustExist() {
        assertTrue(messageOf(() -> controller.addFlight(admin, new FlightDetails("AC123", 999, "AC",
                world.small.id(), DAY.atTime(8, 0), DAY.atTime(10, 0), new BigDecimal("200"))))
                .contains("Route not found"));
        assertTrue(messageOf(() -> controller.addFlight(admin, new FlightDetails("AC123", world.yycYvr.id(), "ZZ",
                world.small.id(), DAY.atTime(8, 0), DAY.atTime(10, 0), new BigDecimal("200"))))
                .contains("Airline not found"));
        assertTrue(messageOf(() -> controller.addFlight(admin, new FlightDetails("AC123", world.yycYvr.id(), "AC",
                999, DAY.atTime(8, 0), DAY.atTime(10, 0), new BigDecimal("200")))).contains("Aircraft not found"));
    }

    // ---- remove

    @Test
    void aFlightWithAnActiveReservationCannotBeRemoved() {
        Flight flight = add("AC123", world.small.id(), 8, 10);
        reserve(flight, "1A", "CONFIRMED");
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> controller.removeFlight(admin, flight.id()));
        assertTrue(e.getMessage().contains("active reservations"), e.getMessage());
        assertTrue(daos.flightDao().findById(flight.id()).isPresent());
        assertFalse(daos.flightDao().isArchived(flight.id()));
    }

    @Test
    void aPendingReservationBlocksRemovalToo() {
        Flight flight = add("AC123", world.small.id(), 8, 10);
        reserve(flight, "1A", "PENDING_PAYMENT");
        assertThrows(IllegalStateException.class, () -> controller.removeFlight(admin, flight.id()));
    }

    @Test
    void aFlightWithOnlyACancelledReservationIsArchivedAndLeavesTheSearch() {
        Flight flight = add("AC123", world.small.id(), 8, 10);
        reserve(flight, "1A", "CANCELLED");
        controller.removeFlight(admin, flight.id());
        assertTrue(daos.flightDao().isArchived(flight.id()));
        assertTrue(daos.flightDao().findById(flight.id()).isPresent());
        assertTrue(daos.flightDao().search("YYC", "YVR", DAY, null).isEmpty());
        assertEquals(1, daos.reservationDao().findByFlight(flight.id()).size());
    }

    @Test
    void aFlightWithoutReservationsIsDeletedWithItsSeats() {
        Flight flight = add("AC123", world.small.id(), 8, 10);
        controller.removeFlight(admin, flight.id());
        assertTrue(daos.flightDao().findById(flight.id()).isEmpty());
        assertTrue(daos.seatDao().findByFlight(flight.id()).isEmpty());
        assertFalse(daos.flightDao().existsByFlightNumber("AC123"));
    }

    @Test
    void onlyAnAdministratorMayRemoveAndTheFlightMustExist() {
        Flight flight = add("AC123", world.small.id(), 8, 10);
        assertThrows(AccessDeniedException.class, () -> controller.removeFlight(jo, flight.id()));
        assertThrows(AccessDeniedException.class, () -> controller.removeFlight(agent, flight.id()));
        assertTrue(daos.flightDao().findById(flight.id()).isPresent());
        assertThrows(IllegalArgumentException.class, () -> controller.removeFlight(admin, 999));
    }

    // ---- update

    @Test
    void theBasePriceCanBeChanged() {
        Flight flight = add("AC123", world.small.id(), 8, 10);
        FlightDetails dearer = new FlightDetails("AC123", world.yycYvr.id(), "AC", world.small.id(),
                DAY.atTime(8, 0), DAY.atTime(10, 0), new BigDecimal("250.50"));
        Flight changed = controller.updateFlight(admin, flight.id(), dearer);
        assertEquals(new BigDecimal("250.50"), changed.basePrice());
        assertEquals(new BigDecimal("250.50"), daos.flightDao().findById(flight.id()).orElseThrow().basePrice());
    }

    @Test
    void thePriceAndTimesCanChangeEvenWithActiveReservations() {
        Flight flight = add("AC123", world.small.id(), 8, 10);
        reserve(flight, "1A", "CONFIRMED");
        FlightDetails later = new FlightDetails("AC123", world.yycYvr.id(), "AC", world.small.id(),
                DAY.atTime(9, 0), DAY.atTime(11, 0), new BigDecimal("210.00"));
        Flight changed = controller.updateFlight(admin, flight.id(), later);
        assertEquals(DAY.atTime(9, 0), changed.departure());
    }

    @Test
    void aFlightMayMoveWithinItsOwnOldSlot() {
        Flight flight = add("AC123", world.small.id(), 8, 10);
        // 09:00-11:00 overlaps its own old time, which must not count against it
        FlightDetails shifted = new FlightDetails("AC123", world.yycYvr.id(), "AC", world.small.id(),
                DAY.atTime(9, 0), DAY.atTime(11, 0), new BigDecimal("200.00"));
        assertEquals(DAY.atTime(9, 0), controller.updateFlight(admin, flight.id(), shifted).departure());
    }

    @Test
    void anUpdateStillChecksNumbersAndOverlapsAgainstOtherFlights() {
        Flight first = add("AC100", world.small.id(), 8, 10);
        add("AC200", world.small.id(), 12, 14);
        FlightDetails takenNumber = new FlightDetails("AC200", world.yycYvr.id(), "AC", world.small.id(),
                DAY.atTime(8, 0), DAY.atTime(10, 0), new BigDecimal("200.00"));
        assertTrue(messageOf(() -> controller.updateFlight(admin, first.id(), takenNumber))
                .contains("Flight number already exists"));
        FlightDetails clash = new FlightDetails("AC100", world.yycYvr.id(), "AC", world.small.id(),
                DAY.atTime(11, 0), DAY.atTime(13, 0), new BigDecimal("200.00"));
        assertTrue(messageOf(() -> controller.updateFlight(admin, first.id(), clash))
                .contains("Aircraft is already assigned"));
        FlightDetails renumbered = new FlightDetails("AC300", world.yycYvr.id(), "AC", world.small.id(),
                DAY.atTime(8, 0), DAY.atTime(10, 0), new BigDecimal("200.00"));
        assertEquals("AC300", controller.updateFlight(admin, first.id(), renumbered).flightNumber());
    }

    @Test
    void theAircraftCannotChangeWithAnActiveReservation() {
        Flight flight = add("AC123", world.small.id(), 8, 10);
        reserve(flight, "1A", "CONFIRMED");
        FlightDetails bigger = new FlightDetails("AC123", world.yycYvr.id(), "AC", world.big.id(),
                DAY.atTime(8, 0), DAY.atTime(10, 0), new BigDecimal("200.00"));
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> controller.updateFlight(admin, flight.id(), bigger));
        assertTrue(e.getMessage().contains("active reservations"), e.getMessage());
        assertEquals(world.small.id(), daos.flightDao().findById(flight.id()).orElseThrow().aircraft().id());
        assertEquals(12, daos.seatDao().findByFlight(flight.id()).size());
    }

    @Test
    void theRouteCannotChangeWithAnActiveReservation() {
        Flight flight = add("AC123", world.small.id(), 8, 10);
        reserve(flight, "1A", "PENDING_PAYMENT");
        FlightDetails other = new FlightDetails("AC123", world.yycYeg.id(), "AC", world.small.id(),
                DAY.atTime(8, 0), DAY.atTime(10, 0), new BigDecimal("200.00"));
        assertThrows(IllegalStateException.class, () -> controller.updateFlight(admin, flight.id(), other));
    }

    @Test
    void theAircraftCanChangeWhileThereAreNoReservationsAndTheSeatsFollow() {
        Flight flight = add("AC123", world.small.id(), 8, 10);
        FlightDetails bigger = new FlightDetails("AC123", world.yycYvr.id(), "AC", world.big.id(),
                DAY.atTime(8, 0), DAY.atTime(10, 0), new BigDecimal("200.00"));
        controller.updateFlight(admin, flight.id(), bigger);
        assertEquals(world.big.id(), daos.flightDao().findById(flight.id()).orElseThrow().aircraft().id());
        assertEquals(180, daos.seatDao().findByFlight(flight.id()).size());
    }

    @Test
    void theAircraftCannotChangeWhenOnlyOldReservationsExist() {
        Flight flight = add("AC123", world.small.id(), 8, 10);
        reserve(flight, "1A", "CANCELLED");
        FlightDetails bigger = new FlightDetails("AC123", world.yycYvr.id(), "AC", world.big.id(),
                DAY.atTime(8, 0), DAY.atTime(10, 0), new BigDecimal("200.00"));
        assertThrows(IllegalStateException.class, () -> controller.updateFlight(admin, flight.id(), bigger));
        assertEquals(12, daos.seatDao().findByFlight(flight.id()).size());
    }

    @Test
    void theRouteCanChangeWhenOnlyOldReservationsExist() {
        Flight flight = add("AC123", world.small.id(), 8, 10);
        reserve(flight, "1A", "CANCELLED");
        FlightDetails other = new FlightDetails("AC123", world.yycYeg.id(), "AC", world.small.id(),
                DAY.atTime(8, 0), DAY.atTime(10, 0), new BigDecimal("200.00"));
        assertEquals("Edmonton", controller.updateFlight(admin, flight.id(), other).route().destination().city());
    }

    @Test
    void updateIsForAnAdministratorOnTheFlightThatExistsAndIsNotRemoved() {
        Flight flight = add("AC123", world.small.id(), 8, 10);
        FlightDetails same = details("AC123", world.small.id(), 8, 10);
        assertThrows(AccessDeniedException.class, () -> controller.updateFlight(jo, flight.id(), same));
        assertThrows(AccessDeniedException.class, () -> controller.updateFlight(agent, flight.id(), same));
        assertThrows(IllegalArgumentException.class, () -> controller.updateFlight(admin, 999, same));

        reserve(flight, "1A", "CANCELLED");
        controller.removeFlight(admin, flight.id());
        assertThrows(IllegalStateException.class, () -> controller.updateFlight(admin, flight.id(), same));
    }

    @Test
    void anUpdateCannotMoveTheDepartureIntoThePast() {
        Flight flight = add("AC123", world.small.id(), 8, 10);
        FlightDetails past = new FlightDetails("AC123", world.yycYvr.id(), "AC", world.small.id(),
                LocalDateTime.of(2026, 11, 30, 8, 0), LocalDateTime.of(2026, 11, 30, 10, 0), new BigDecimal("200"));
        assertTrue(messageOf(() -> controller.updateFlight(admin, flight.id(), past))
                .contains("must be in the future"));
    }
}
