package ca.ucalgary.chinookair.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.ReservationStatus;
import ca.ucalgary.chinookair.business.entity.SeatLayout;
import ca.ucalgary.chinookair.business.entity.SeatUnavailableException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqliteReservationDaoTest {

    private static final BigDecimal PRICE = new BigDecimal("200.00");

    private Connection db;
    private TestWorld world;
    private ReservationDao dao;
    private Customer jo;
    private Customer bo;
    private Flight flight;
    private Flight otherFlight;

    @BeforeEach
    void open() {
        db = TestDatabase.inMemory();
        world = new TestWorld(db);
        CustomerDao customers = new SqliteCustomerDao(db);
        FlightDao flights = new SqliteFlightDao(db);
        dao = new SqliteReservationDao(db, customers, flights);
        jo = customers.findById(1).orElseThrow();
        bo = customers.save(new Customer(null, "bwong", "pbkdf2$1$AAAA$BBBB", "Bo Wong", "bo@x.ca", null));
        SeatDao seats = new SqliteSeatDao(db);
        flight = flights.save(world.flight("AC123", world.yycYvr, world.ac, world.small,
                LocalDateTime.of(2026, 12, 1, 8, 0)));
        otherFlight = flights.save(world.flight("AC456", world.yycYvr, world.ac, world.small,
                LocalDateTime.of(2026, 12, 2, 8, 0)));
        seats.createSeats(flight.id(), SeatLayout.generate(world.small));
        seats.createSeats(otherFlight.id(), SeatLayout.generate(world.small));
    }

    @AfterEach
    void close() throws SQLException {
        db.close();
    }

    // ---- helpers

    private Reservation reservation(String number, Customer customer, Flight onFlight, String seat, int minute) {
        Reservation r = new Reservation(number, customer, onFlight, LocalDateTime.of(2026, 11, 20, 9, minute));
        if (seat != null) {
            r.assignSeat(seat, PRICE);
        }
        return r;
    }

    private Reservation pending(String number, Customer customer, String seat) {
        return reservation(number, customer, flight, seat, 0);
    }

    // ---- save and find

    @Test
    void aSavedReservationGetsAnIdAndComesBackWithItsSeatPriceAndStatus() {
        Reservation saved = dao.save(pending("CA12AB34", jo, "1A"));
        assertNotNull(saved.id());
        Reservation found = dao.findById(saved.id()).orElseThrow();
        assertEquals("1A", found.seatNo());
        assertEquals(PRICE, found.price());
        assertEquals(ReservationStatus.PENDING_PAYMENT, found.status());
        assertEquals("CA12AB34", found.confirmationNumber());
        assertEquals(LocalDateTime.of(2026, 11, 20, 9, 0), found.createdAt());
    }

    @Test
    void theCustomerAndTheFlightComeBackComplete() {
        Reservation saved = dao.save(pending("CA12AB34", jo, "1A"));
        Reservation found = dao.findById(saved.id()).orElseThrow();
        assertEquals("Jo Smith", found.customer().name());
        assertEquals(flight, found.flight());
        assertEquals("Vancouver", found.flight().route().destination().city());
    }

    @Test
    void aReservationWithoutASeatCanBeSaved() {
        Reservation saved = dao.save(reservation("CA00000A", jo, flight, null, 0));
        Reservation found = dao.findById(saved.id()).orElseThrow();
        assertEquals(ReservationStatus.CREATED, found.status());
        assertNull(found.seatNo());
        assertNull(found.price());
    }

    @Test
    void anUnknownReservationGivesAnEmptyResult() {
        assertTrue(dao.findById(99).isEmpty());
        assertTrue(dao.findByConfirmationNumber("CA000000").isEmpty());
        assertTrue(dao.findByConfirmationNumber(null).isEmpty());
    }

    @Test
    void theStateIsRebuiltSoTheRulesStillApply() {
        Reservation r = pending("CA12AB34", jo, "1A");
        r.confirmPayment();
        dao.save(r);
        Reservation found = dao.findById(r.id()).orElseThrow();
        assertEquals(ReservationStatus.CONFIRMED, found.status());
        assertThrows(IllegalStateException.class, () -> found.assignSeat("2A", PRICE));
        found.complete(Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
        assertEquals(ReservationStatus.COMPLETED, found.status());
        assertThrows(IllegalStateException.class, found::cancel);
    }

    @Test
    void aReservationThatIsAlreadySavedCannotBeSavedAgain() {
        Reservation saved = dao.save(pending("CA12AB34", jo, "1A"));
        assertThrows(IllegalArgumentException.class, () -> dao.save(saved));
    }

    @Test
    void theCustomerAndTheFlightMustBeSaved() {
        Customer unsaved = new Customer(null, "newbie", "pbkdf2$1$AAAA$BBBB", "New Bie", "n@x.ca", null);
        assertThrows(IllegalArgumentException.class, () -> dao.save(pending("CA12AB34", unsaved, "1A")));
    }

    // ---- the double-booking protection

    @Test
    void aSecondCustomerCannotTakeASeatThatIsPending() {
        dao.save(pending("CA000001", jo, "1A"));
        assertThrows(SeatUnavailableException.class, () -> dao.save(pending("CA000002", bo, "1A")));
        assertEquals(1, dao.findByFlight(flight.id()).size());
    }

    @Test
    void aSecondCustomerCannotTakeASeatThatIsConfirmed() {
        Reservation first = pending("CA000001", jo, "1A");
        first.confirmPayment();
        dao.save(first);
        assertThrows(SeatUnavailableException.class, () -> dao.save(pending("CA000002", bo, "1A")));
    }

    @Test
    void theSeatIsFreeAgainWhenTheFirstReservationIsCancelled() {
        Reservation first = dao.save(pending("CA000001", jo, "1A"));
        first.cancel();
        dao.update(first);
        Reservation second = dao.save(pending("CA000002", bo, "1A"));
        assertNotNull(second.id());
    }

    @Test
    void theSameSeatOnAnotherFlightIsNotAClash() {
        dao.save(pending("CA000001", jo, "1A"));
        Reservation other = reservation("CA000002", bo, otherFlight, "1A", 1);
        assertNotNull(dao.save(other).id());
    }

    @Test
    void aSeatThatIsNotOnTheFlightIsAPlainDatabaseError() {
        DataAccessException e = assertThrows(DataAccessException.class,
                () -> dao.save(pending("CA000001", jo, "99Z")));
        assertEquals(DataAccessException.class, e.getClass());
    }

    @Test
    void movingToATakenSeatIsRefusedAndNothingChanges() {
        dao.save(pending("CA000001", jo, "1A"));
        Reservation mine = pending("CA000002", bo, "1B");
        mine.confirmPayment();
        dao.save(mine);

        mine.changeFlight(flight, "1A", PRICE);
        assertThrows(SeatUnavailableException.class, () -> dao.update(mine));

        Reservation stored = dao.findById(mine.id()).orElseThrow();
        assertEquals("1B", stored.seatNo());
        assertEquals(ReservationStatus.CONFIRMED, stored.status());
    }

    // ---- update

    @Test
    void updateWritesTheNewStatusFlightSeatAndPrice() {
        Reservation r = pending("CA12AB34", jo, "1A");
        r.confirmPayment();
        dao.save(r);
        r.changeFlight(otherFlight, "3C", new BigDecimal("250.00"));
        dao.update(r);
        Reservation found = dao.findById(r.id()).orElseThrow();
        assertEquals(otherFlight, found.flight());
        assertEquals("3C", found.seatNo());
        assertEquals(new BigDecimal("250.00"), found.price());
        assertEquals(ReservationStatus.PENDING_PAYMENT, found.status());
    }

    @Test
    void updateNeedsASavedReservation() {
        assertThrows(IllegalArgumentException.class, () -> dao.update(pending("CA12AB34", jo, "1A")));
        Reservation ghost = Reservation.restore(99L, "CA99999Z", jo, flight, "1A", PRICE,
                LocalDateTime.of(2026, 11, 20, 9, 0), ReservationStatus.PENDING_PAYMENT);
        assertThrows(DataAccessException.class, () -> dao.update(ghost));
    }

    // ---- other lookups

    @Test
    void findByCustomerReturnsTheCustomersReservationsNewestFirst() {
        Reservation older = dao.save(reservation("CA000001", jo, flight, "1A", 0));
        Reservation newer = dao.save(reservation("CA000002", jo, otherFlight, "1B", 30));
        dao.save(reservation("CA000003", bo, flight, "2A", 15));
        List<Long> ids = dao.findByCustomer(jo.id()).stream().map(Reservation::id).toList();
        assertEquals(List.of(newer.id(), older.id()), ids);
        assertTrue(dao.findByCustomer(99).isEmpty());
    }

    @Test
    void findByFlightReturnsOnlyThatFlightOldestFirst() {
        Reservation a = dao.save(reservation("CA000001", jo, flight, "1A", 10));
        Reservation b = dao.save(reservation("CA000002", bo, flight, "1B", 5));
        dao.save(reservation("CA000003", bo, otherFlight, "1A", 0));
        List<Long> ids = dao.findByFlight(flight.id()).stream().map(Reservation::id).toList();
        assertEquals(List.of(b.id(), a.id()), ids);
    }

    @Test
    void findByConfirmationNumberFindsItIgnoringCase() {
        Reservation saved = dao.save(pending("CA12AB34", jo, "1A"));
        assertEquals(saved.id(), dao.findByConfirmationNumber("CA12AB34").orElseThrow().id());
        assertEquals(saved.id(), dao.findByConfirmationNumber(" ca12ab34 ").orElseThrow().id());
    }

    @Test
    void aConfirmationNumberCanOnlyBeUsedOnce() {
        dao.save(pending("CA12AB34", jo, "1A"));
        DataAccessException e = assertThrows(DataAccessException.class,
                () -> dao.save(pending("CA12AB34", bo, "2A")));
        assertEquals(DataAccessException.class, e.getClass());
        assertTrue(e.getMessage().contains("confirmation number"), e.getMessage());
    }

    @Test
    void countActiveIgnoresCancelledAndCompletedReservations() {
        Reservation cancelled = dao.save(pending("CA000001", jo, "1A"));
        cancelled.cancel();
        dao.update(cancelled);
        Reservation completed = pending("CA000002", jo, "1B");
        completed.confirmPayment();
        dao.save(completed);
        completed.complete(Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC));
        dao.update(completed);
        dao.save(pending("CA000003", bo, "2A"));
        Reservation confirmed = pending("CA000004", bo, "2B");
        confirmed.confirmPayment();
        dao.save(confirmed);
        dao.save(reservation("CA000005", bo, flight, null, 1));

        assertEquals(3, dao.countActiveByFlight(flight.id()));
        assertEquals(0, dao.countActiveByFlight(otherFlight.id()));
    }
}
