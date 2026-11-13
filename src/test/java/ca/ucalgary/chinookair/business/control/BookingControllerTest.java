package ca.ucalgary.chinookair.business.control;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Administrator;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.FlightAgent;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.ReservationStatus;
import ca.ucalgary.chinookair.business.entity.Seat;
import ca.ucalgary.chinookair.business.entity.SeatLayout;
import ca.ucalgary.chinookair.business.entity.SeatUnavailableException;
import ca.ucalgary.chinookair.data.DaoFactory;
import ca.ucalgary.chinookair.data.TestDatabase;
import ca.ucalgary.chinookair.data.TestWorld;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BookingControllerTest {

    private static final String HASH = "pbkdf2$1$AAAA$BBBB";
    private static final Clock CLOCK = clockAt(LocalDateTime.of(2026, 12, 1, 9, 0));

    private Connection db;
    private DaoFactory daos;
    private BookingController controller;
    private Customer jo;
    private Customer bo;
    private FlightAgent agent;
    private Administrator admin;
    private Flight flight;

    private static Clock clockAt(LocalDateTime time) {
        return Clock.fixed(time.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    }

    @BeforeEach
    void open() {
        db = TestDatabase.inMemory();
        TestWorld world = new TestWorld(db);
        daos = DaoFactory.create(db);
        controller = controllerAt(CLOCK, new RandomConfirmationNumberGenerator(new Random(1)));
        jo = daos.customerDao().findById(1).orElseThrow();
        bo = daos.customerDao().save(new Customer(null, "bwong", HASH, "Bo Wong", "bo@x.ca", null));
        agent = (FlightAgent) daos.userDao().save(new FlightAgent(null, "agent1", HASH));
        admin = (Administrator) daos.userDao().save(new Administrator(null, "admin", HASH));
        // base price 200.00, a 3x4 aircraft, departing tomorrow at 08:00
        flight = daos.flightDao().save(world.flight("AC123", world.yycYvr, world.ac, world.small,
                LocalDateTime.of(2026, 12, 2, 8, 0)));
        daos.seatDao().createSeats(flight.id(), SeatLayout.generate(world.small));
    }

    @AfterEach
    void close() throws SQLException {
        db.close();
    }

    private BookingController controllerAt(Clock clock, ConfirmationNumberGenerator generator) {
        return new BookingController(daos.flightDao(), daos.seatDao(), daos.reservationDao(), generator, clock);
    }

    private Reservation start(Customer customer) {
        return controller.startReservation(customer, customer, flight.id());
    }

    // ---- starting a reservation

    @Test
    void aNewReservationIsCreatedWithoutASeatAndWithAConfirmationNumber() {
        Reservation r = start(jo);
        assertNotNull(r.id());
        assertEquals(ReservationStatus.CREATED, r.status());
        assertNull(r.seatNo());
        assertTrue(r.confirmationNumber().matches("^CA[A-Z0-9]{6}$"));
        assertEquals(LocalDateTime.of(2026, 12, 1, 9, 0), r.createdAt());
        assertEquals(jo.id(), daos.reservationDao().findById(r.id()).orElseThrow().customer().id());
    }

    @Test
    void aCustomerCanReserveOnlyForThemselves() {
        assertThrows(AccessDeniedException.class, () -> controller.startReservation(jo, bo, flight.id()));
        assertDoesNotThrow(() -> controller.startReservation(jo, jo, flight.id()));
    }

    @Test
    void anAgentCanReserveForAnyCustomer() {
        Reservation r = controller.startReservation(agent, bo, flight.id());
        assertEquals(bo.id(), r.customer().id());
    }

    @Test
    void anAdministratorCannotReserveAtAll() {
        assertThrows(AccessDeniedException.class, () -> controller.startReservation(admin, jo, flight.id()));
        assertThrows(AccessDeniedException.class, () -> controller.startReservation(null, jo, flight.id()));
    }

    @Test
    void aSavedCustomerIsRequired() {
        Customer unsaved = new Customer(null, "newbie", HASH, "New Bie", "n@x.ca", null);
        assertThrows(IllegalArgumentException.class, () -> controller.startReservation(agent, unsaved, flight.id()));
        assertThrows(IllegalArgumentException.class, () -> controller.startReservation(agent, null, flight.id()));
    }

    @Test
    void aFlightThatAlreadyDepartedCannotBeReserved() {
        BookingController late = controllerAt(clockAt(LocalDateTime.of(2026, 12, 2, 8, 0)),
                new RandomConfirmationNumberGenerator(new Random(1)));
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> late.startReservation(jo, jo, flight.id()));
        assertTrue(e.getMessage().contains("already departed"), e.getMessage());
    }

    @Test
    void anUnknownOrArchivedFlightCannotBeReserved() {
        assertThrows(IllegalArgumentException.class, () -> controller.startReservation(jo, jo, 999));
        daos.flightDao().archive(flight.id());
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> start(jo));
        assertTrue(e.getMessage().contains("no longer available"), e.getMessage());
    }

    @Test
    void aClashingConfirmationNumberIsRetried() {
        Deque<String> numbers = new ArrayDeque<>(List.of("CAAAAAAA", "CAAAAAAA", "CAAAAAAA", "CABBBBBB"));
        BookingController c = controllerAt(CLOCK, numbers::poll);
        assertEquals("CAAAAAAA", c.startReservation(jo, jo, flight.id()).confirmationNumber());
        // the next two draws clash with the first reservation, the fourth is free
        assertEquals("CABBBBBB", c.startReservation(bo, bo, flight.id()).confirmationNumber());
    }

    @Test
    void givesUpAfterFiveClashes() {
        controller.startReservation(jo, jo, flight.id());
        String taken = daos.reservationDao().findByCustomer(jo.id()).get(0).confirmationNumber();
        BookingController stuck = controllerAt(CLOCK, () -> taken);
        assertThrows(IllegalStateException.class, () -> stuck.startReservation(bo, bo, flight.id()));
        assertTrue(daos.reservationDao().findByCustomer(bo.id()).isEmpty());
    }

    // ---- available seats

    @Test
    void availableSeatsShrinkWhenASeatIsTaken() {
        assertEquals(12, controller.availableSeats(flight.id()).size());
        Reservation r = start(jo);
        controller.selectSeat(r.id(), "2B");
        List<Seat> free = controller.availableSeats(flight.id());
        assertEquals(11, free.size());
        assertTrue(free.stream().noneMatch(s -> s.number().equals("2B")));
        assertEquals("1A", free.get(0).number());
    }

    @Test
    void availableSeatsOfAnUnknownFlightIsAnError() {
        assertThrows(IllegalArgumentException.class, () -> controller.availableSeats(999));
    }

    // ---- selecting a seat

    @Test
    void aBusinessSeatCostsOneAndAHalfTimesTheBase() {
        Reservation r = controller.selectSeat(start(jo).id(), "1A");
        assertEquals(new BigDecimal("300.00"), r.price());
        assertEquals("1A", r.seatNo());
        assertEquals(ReservationStatus.PENDING_PAYMENT, r.status());
    }

    @Test
    void anEconomySeatCostsTheBase() {
        Reservation r = controller.selectSeat(start(jo).id(), "3C");
        assertEquals(new BigDecimal("200.00"), r.price());
        assertEquals(ReservationStatus.PENDING_PAYMENT, r.status());
    }

    @Test
    void theChosenSeatIsSavedAndTheSeatNumberIsTidied() {
        Reservation r = controller.selectSeat(start(jo).id(), " 3c ");
        Reservation stored = daos.reservationDao().findById(r.id()).orElseThrow();
        assertEquals("3C", stored.seatNo());
        assertEquals(new BigDecimal("200.00"), stored.price());
        assertEquals(ReservationStatus.PENDING_PAYMENT, stored.status());
    }

    @Test
    void theSecondCustomerLosesTheRaceAndTheFirstKeepsTheSeat() {
        Reservation joRes = controller.selectSeat(start(jo).id(), "1A");
        Reservation boRes = start(bo);

        assertThrows(SeatUnavailableException.class, () -> controller.selectSeat(boRes.id(), "1A"));

        Reservation joStored = daos.reservationDao().findById(joRes.id()).orElseThrow();
        assertEquals("1A", joStored.seatNo());
        assertEquals(ReservationStatus.PENDING_PAYMENT, joStored.status());
        // the loser is untouched and can pick another seat
        Reservation boStored = daos.reservationDao().findById(boRes.id()).orElseThrow();
        assertEquals(ReservationStatus.CREATED, boStored.status());
        assertNull(boStored.seatNo());
        assertEquals("1B", controller.selectSeat(boRes.id(), "1B").seatNo());
    }

    @Test
    void aSeatThatIsNotOnTheFlightIsRefused() {
        Reservation r = start(jo);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> controller.selectSeat(r.id(), "99Z"));
        assertTrue(e.getMessage().contains("does not exist on this flight"), e.getMessage());
        assertEquals(ReservationStatus.CREATED, daos.reservationDao().findById(r.id()).orElseThrow().status());
        assertThrows(IllegalArgumentException.class, () -> controller.selectSeat(r.id(), null));
    }

    @Test
    void aReservationThatAlreadyHasASeatCannotPickAgain() {
        Reservation r = controller.selectSeat(start(jo).id(), "1A");
        assertThrows(IllegalStateException.class, () -> controller.selectSeat(r.id(), "1B"));
        assertEquals("1A", daos.reservationDao().findById(r.id()).orElseThrow().seatNo());
    }

    @Test
    void anUnknownReservationIsAnError() {
        assertThrows(IllegalArgumentException.class, () -> controller.selectSeat(999, "1A"));
        assertThrows(IllegalArgumentException.class, () -> controller.cancelPending(999));
    }

    @Test
    void aSeatCannotBePickedAfterTheFlightLeft() {
        Reservation r = start(jo);
        BookingController late = controllerAt(clockAt(LocalDateTime.of(2026, 12, 2, 8, 30)),
                new RandomConfirmationNumberGenerator(new Random(1)));
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> late.selectSeat(r.id(), "1A"));
        assertTrue(e.getMessage().contains("already departed"), e.getMessage());
    }

    // ---- giving up

    @Test
    void givingUpAPendingReservationFreesTheSeat() {
        Reservation r = controller.selectSeat(start(jo).id(), "1A");
        assertEquals(11, controller.availableSeats(flight.id()).size());
        controller.cancelPending(r.id());
        assertEquals(ReservationStatus.CANCELLED, daos.reservationDao().findById(r.id()).orElseThrow().status());
        assertEquals(12, controller.availableSeats(flight.id()).size());
        // and somebody else can now take that seat
        assertEquals("1A", controller.selectSeat(start(bo).id(), "1A").seatNo());
    }

    @Test
    void aReservationWithoutASeatCanBeGivenUpToo() {
        Reservation r = start(jo);
        controller.cancelPending(r.id());
        assertEquals(ReservationStatus.CANCELLED, daos.reservationDao().findById(r.id()).orElseThrow().status());
    }

    @Test
    void aPaidReservationCannotBeGivenUp() {
        Reservation r = controller.selectSeat(start(jo).id(), "1A");
        r.confirmPayment();
        daos.reservationDao().update(r);
        assertThrows(IllegalStateException.class, () -> controller.cancelPending(r.id()));
        assertEquals(ReservationStatus.CONFIRMED, daos.reservationDao().findById(r.id()).orElseThrow().status());
    }

    @Test
    void aCancelledReservationCannotBeGivenUpAgain() {
        Reservation r = start(jo);
        controller.cancelPending(r.id());
        assertThrows(IllegalStateException.class, () -> controller.cancelPending(r.id()));
    }
}
