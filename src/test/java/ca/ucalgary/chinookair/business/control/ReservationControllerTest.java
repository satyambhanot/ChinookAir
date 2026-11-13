package ca.ucalgary.chinookair.business.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Administrator;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.FlightAgent;
import ca.ucalgary.chinookair.business.entity.Payment;
import ca.ucalgary.chinookair.business.entity.PaymentMethodType;
import ca.ucalgary.chinookair.business.entity.PaymentStatus;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.ReservationStatus;
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
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReservationControllerTest {

    private static final String HASH = "pbkdf2$1$AAAA$BBBB";
    private static final String MASKED = "**** **** **** 1111";
    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private Connection db;
    private TestWorld world;
    private DaoFactory daos;
    private ReservationController controller;
    private Customer jo;
    private Customer bo;
    private FlightAgent agent;
    private Administrator admin;
    private Flight flightA;
    private Flight flightB;
    private Flight flightC;
    private int numbers;

    @BeforeEach
    void open() {
        db = TestDatabase.inMemory();
        world = new TestWorld(db);
        daos = DaoFactory.create(db);
        controller = new ReservationController(daos.reservationDao(), daos.paymentDao(), daos.flightDao(),
                daos.seatDao(), CLOCK);
        jo = daos.customerDao().findById(1).orElseThrow();
        bo = daos.customerDao().save(new Customer(null, "bwong", HASH, "Bo Wong", "bo@x.ca", null));
        agent = (FlightAgent) daos.userDao().save(new FlightAgent(null, "agent1", HASH));
        admin = (Administrator) daos.userDao().save(new Administrator(null, "admin", HASH));
        flightA = flight("AC100", "200.00", LocalDateTime.of(2026, 12, 2, 8, 0));
        flightB = flight("AC200", "200.00", LocalDateTime.of(2026, 12, 3, 8, 0));
        flightC = flight("AC300", "250.00", LocalDateTime.of(2026, 12, 4, 8, 0));
    }

    @AfterEach
    void close() throws SQLException {
        db.close();
    }

    // ---- fixtures

    private Flight flight(String number, String basePrice, LocalDateTime departure) {
        Flight made = world.flight(number, world.yycYvr, world.ac, world.small, departure);
        Flight saved = daos.flightDao().save(new Flight(null, made.flightNumber(), made.route(), made.airline(),
                made.aircraft(), made.departure(), made.arrival(), new BigDecimal(basePrice)));
        daos.seatDao().createSeats(saved.id(), SeatLayout.generate(world.small));
        return saved;
    }

    private Reservation reservation(Customer customer, Flight on, String seat, String price,
                                    ReservationStatus status, int minute) {
        Reservation r = Reservation.restore(null, "CA00000" + (++numbers), customer, on, seat,
                seat == null ? null : new BigDecimal(price), LocalDateTime.of(2026, 11, 20, 9, minute), status);
        return daos.reservationDao().save(r);
    }

    private Reservation confirmed(Customer customer, Flight on, String seat, String price) {
        return reservation(customer, on, seat, price, ReservationStatus.CONFIRMED, 0);
    }

    private Payment approvedPayment(Reservation r, String amount) {
        Payment p = new Payment(r.id(), new BigDecimal(amount), PaymentMethodType.CREDIT, MASKED,
                LocalDateTime.of(2026, 11, 20, 9, 1));
        p.approve();
        return daos.paymentDao().save(p);
    }

    private Reservation stored(Reservation r) {
        return daos.reservationDao().findById(r.id()).orElseThrow();
    }

    private PaymentStatus paymentStatus(Payment p) {
        return daos.paymentDao().findById(p.id()).orElseThrow().status();
    }

    // ---- can cancel

    @Test
    void aConfirmedOrPendingReservationOfAFutureFlightCanBeCancelled() {
        assertTrue(controller.canCancel(confirmed(jo, flightA, "1A", "300.00")));
        assertTrue(controller.canCancel(
                reservation(jo, flightA, "1B", "300.00", ReservationStatus.PENDING_PAYMENT, 0)));
    }

    @Test
    void aCancelledOrCreatedReservationCannotBeCancelled() {
        assertFalse(controller.canCancel(reservation(jo, flightA, "1A", "300.00", ReservationStatus.CANCELLED, 0)));
        assertFalse(controller.canCancel(reservation(jo, flightA, null, null, ReservationStatus.CREATED, 0)));
    }

    @Test
    void aReservationOfAFlightThatLeftCannotBeCancelled() {
        Reservation r = confirmed(jo, flightA, "1A", "300.00");
        ReservationController late = new ReservationController(daos.reservationDao(), daos.paymentDao(),
                daos.flightDao(), daos.seatDao(), Clock.fixed(
                        LocalDateTime.of(2026, 12, 2, 8, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
        assertFalse(late.canCancel(r));
    }

    // ---- cancel

    @Test
    void cancellingAConfirmedReservationRefundsThePaymentAndFreesTheSeat() {
        Reservation r = confirmed(jo, flightA, "1A", "300.00");
        Payment p = approvedPayment(r, "300.00");
        assertEquals(11, daos.seatDao().countAvailable(flightA.id()));

        Reservation result = controller.cancel(jo, r.id());

        assertEquals(ReservationStatus.CANCELLED, result.status());
        assertEquals(ReservationStatus.CANCELLED, stored(r).status());
        assertEquals(PaymentStatus.REFUNDED, paymentStatus(p));
        assertEquals(12, daos.seatDao().countAvailable(flightA.id()));
    }

    @Test
    void cancellingAPendingReservationNeedsNoRefund() {
        Reservation r = reservation(jo, flightA, "1A", "300.00", ReservationStatus.PENDING_PAYMENT, 0);
        controller.cancel(jo, r.id());
        assertEquals(ReservationStatus.CANCELLED, stored(r).status());
        assertTrue(daos.paymentDao().findByReservation(r.id()).isEmpty());
    }

    @Test
    void aDeclinedPaymentIsNotRefundedAndEveryApprovedOneIs() {
        Reservation r = confirmed(jo, flightA, "1A", "300.00");
        Payment declined = new Payment(r.id(), new BigDecimal("300.00"), PaymentMethodType.CREDIT, MASKED,
                LocalDateTime.of(2026, 11, 20, 9, 0));
        declined.decline("Card declined");
        daos.paymentDao().save(declined);
        Payment first = approvedPayment(r, "300.00");
        Payment second = approvedPayment(r, "50.00");

        controller.cancel(agent, r.id());

        assertEquals(PaymentStatus.DECLINED, paymentStatus(declined));
        assertEquals(PaymentStatus.REFUNDED, paymentStatus(first));
        assertEquals(PaymentStatus.REFUNDED, paymentStatus(second));
    }

    @Test
    void cancellingAfterDepartureIsRefused() {
        Reservation r = confirmed(jo, flightA, "1A", "300.00");
        Payment p = approvedPayment(r, "300.00");
        ReservationController late = new ReservationController(daos.reservationDao(), daos.paymentDao(),
                daos.flightDao(), daos.seatDao(), Clock.fixed(
                        LocalDateTime.of(2026, 12, 2, 8, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> late.cancel(jo, r.id()));
        assertTrue(e.getMessage().contains("already departed"), e.getMessage());
        assertEquals(ReservationStatus.CONFIRMED, stored(r).status());
        assertEquals(PaymentStatus.APPROVED, paymentStatus(p));
    }

    @Test
    void aCustomerCannotCancelAnotherCustomersReservationButAnAgentCan() {
        Reservation r = confirmed(jo, flightA, "1A", "300.00");
        assertThrows(AccessDeniedException.class, () -> controller.cancel(bo, r.id()));
        assertEquals(ReservationStatus.CONFIRMED, stored(r).status());
        assertEquals(ReservationStatus.CANCELLED, controller.cancel(agent, r.id()).status());
    }

    @Test
    void anAdministratorAndNobodyCanCancel() {
        Reservation r = confirmed(jo, flightA, "1A", "300.00");
        assertThrows(AccessDeniedException.class, () -> controller.cancel(admin, r.id()));
        assertThrows(AccessDeniedException.class, () -> controller.cancel(null, r.id()));
    }

    @Test
    void aCancelledReservationCannotBeCancelledTwiceAndAnUnknownOneIsAnError() {
        Reservation r = confirmed(jo, flightA, "1A", "300.00");
        controller.cancel(jo, r.id());
        assertThrows(IllegalStateException.class, () -> controller.cancel(jo, r.id()));
        assertThrows(IllegalArgumentException.class, () -> controller.cancel(jo, 999));
    }

    @Test
    void aCancelledSeatCanBeReservedAgain() {
        Reservation r = confirmed(jo, flightA, "1A", "300.00");
        controller.cancel(jo, r.id());
        assertEquals("1A", reservation(bo, flightA, "1A", "300.00", ReservationStatus.CONFIRMED, 5).seatNo());
    }

    // ---- modify

    @Test
    void movingToAFlightWithTheSamePriceKeepsItConfirmedAndKeepsThePayment() {
        Reservation r = confirmed(jo, flightA, "3A", "200.00");
        Payment p = approvedPayment(r, "200.00");

        Reservation result = controller.modify(agent, r.id(), flightB.id(), "3C");

        assertEquals(ReservationStatus.CONFIRMED, result.status());
        Reservation moved = stored(r);
        assertEquals(flightB.id(), moved.flight().id());
        assertEquals("3C", moved.seatNo());
        assertEquals(new BigDecimal("200.00"), moved.price());
        assertEquals(PaymentStatus.APPROVED, paymentStatus(p));
        // the old seat is free again
        assertEquals(12, daos.seatDao().countAvailable(flightA.id()));
    }

    @Test
    void movingToADearerFlightRefundsTheOldPaymentAndWaitsForANewOne() {
        Reservation r = confirmed(jo, flightA, "3A", "200.00");
        Payment p = approvedPayment(r, "200.00");

        Reservation result = controller.modify(agent, r.id(), flightC.id(), "3C");

        assertEquals(ReservationStatus.PENDING_PAYMENT, result.status());
        Reservation moved = stored(r);
        assertEquals(ReservationStatus.PENDING_PAYMENT, moved.status());
        assertEquals(new BigDecimal("250.00"), moved.price());
        assertEquals(flightC.id(), moved.flight().id());
        assertEquals(PaymentStatus.REFUNDED, paymentStatus(p));
    }

    @Test
    void aBusinessSeatOnTheNewFlightIsPricedWithTheBusinessFactor() {
        Reservation r = confirmed(jo, flightA, "3A", "200.00");
        approvedPayment(r, "200.00");
        Reservation result = controller.modify(agent, r.id(), flightC.id(), "1A");
        assertEquals(new BigDecimal("375.00"), result.price());
    }

    @Test
    void aTakenSeatIsRefusedAndNothingChanges() {
        reservation(bo, flightB, "3C", "200.00", ReservationStatus.CONFIRMED, 5);
        Reservation r = confirmed(jo, flightA, "3A", "200.00");
        Payment p = approvedPayment(r, "200.00");

        assertThrows(SeatUnavailableException.class, () -> controller.modify(agent, r.id(), flightB.id(), "3C"));

        Reservation unchanged = stored(r);
        assertEquals(flightA.id(), unchanged.flight().id());
        assertEquals("3A", unchanged.seatNo());
        assertEquals(ReservationStatus.CONFIRMED, unchanged.status());
        assertEquals(PaymentStatus.APPROVED, paymentStatus(p));
    }

    @Test
    void aTakenSeatOnADearerFlightDoesNotRefundEither() {
        reservation(bo, flightC, "3C", "250.00", ReservationStatus.PENDING_PAYMENT, 5);
        Reservation r = confirmed(jo, flightA, "3A", "200.00");
        Payment p = approvedPayment(r, "200.00");
        assertThrows(SeatUnavailableException.class, () -> controller.modify(agent, r.id(), flightC.id(), "3C"));
        assertEquals(PaymentStatus.APPROVED, paymentStatus(p));
        assertEquals(ReservationStatus.CONFIRMED, stored(r).status());
    }

    @Test
    void onlyAnAgentMayModify() {
        Reservation r = confirmed(jo, flightA, "3A", "200.00");
        assertThrows(AccessDeniedException.class, () -> controller.modify(jo, r.id(), flightB.id(), "3C"));
        assertThrows(AccessDeniedException.class, () -> controller.modify(admin, r.id(), flightB.id(), "3C"));
        assertThrows(AccessDeniedException.class, () -> controller.modify(null, r.id(), flightB.id(), "3C"));
        assertEquals(flightA.id(), stored(r).flight().id());
    }

    @Test
    void onlyAConfirmedReservationCanBeModified() {
        Reservation pending = reservation(jo, flightA, "3A", "200.00", ReservationStatus.PENDING_PAYMENT, 0);
        Reservation cancelled = reservation(jo, flightA, "3B", "200.00", ReservationStatus.CANCELLED, 1);
        assertThrows(IllegalStateException.class, () -> controller.modify(agent, pending.id(), flightB.id(), "3C"));
        assertThrows(IllegalStateException.class, () -> controller.modify(agent, cancelled.id(), flightB.id(), "3C"));
    }

    @Test
    void theNewFlightAndSeatMustBeRealAndInTheFuture() {
        Reservation r = confirmed(jo, flightA, "3A", "200.00");
        assertThrows(IllegalArgumentException.class, () -> controller.modify(agent, r.id(), 999, "3C"));
        IllegalArgumentException noSeat = assertThrows(IllegalArgumentException.class,
                () -> controller.modify(agent, r.id(), flightB.id(), "99Z"));
        assertTrue(noSeat.getMessage().contains("does not exist on this flight"), noSeat.getMessage());
        assertThrows(IllegalArgumentException.class, () -> controller.modify(agent, r.id(), flightB.id(), null));

        Flight gone = flight("AC400", "200.00", LocalDateTime.of(2026, 11, 30, 8, 0));
        IllegalStateException departed = assertThrows(IllegalStateException.class,
                () -> controller.modify(agent, r.id(), gone.id(), "3C"));
        assertTrue(departed.getMessage().contains("already departed"), departed.getMessage());

        daos.flightDao().archive(flightB.id());
        assertThrows(IllegalStateException.class, () -> controller.modify(agent, r.id(), flightB.id(), "3C"));
        assertEquals(flightA.id(), stored(r).flight().id());
    }

    // ---- history

    @Test
    void historyIsNewestFirst() {
        Reservation older = reservation(jo, flightA, "1A", "300.00", ReservationStatus.PENDING_PAYMENT, 0);
        Reservation newer = reservation(jo, flightB, "1A", "300.00", ReservationStatus.PENDING_PAYMENT, 30);
        reservation(bo, flightA, "1B", "300.00", ReservationStatus.PENDING_PAYMENT, 15);
        List<Reservation> history = controller.history(jo, jo.id());
        assertEquals(List.of(newer.id(), older.id()), history.stream().map(Reservation::id).toList());
    }

    @Test
    void aConfirmedReservationOfAFlightThatLeftIsMarkedCompleted() {
        Flight yesterday = flight("AC500", "200.00", LocalDateTime.of(2026, 11, 30, 8, 0));
        Reservation past = confirmed(jo, yesterday, "1A", "300.00");
        Reservation future = confirmed(jo, flightA, "1A", "300.00");
        Reservation unpaid = reservation(jo, yesterday, "1B", "300.00", ReservationStatus.PENDING_PAYMENT, 5);

        List<Reservation> history = controller.history(jo, jo.id());

        assertEquals(3, history.size());
        assertEquals(ReservationStatus.COMPLETED, stored(past).status());
        assertEquals(ReservationStatus.CONFIRMED, stored(future).status());
        assertEquals(ReservationStatus.PENDING_PAYMENT, stored(unpaid).status());
        assertTrue(history.stream().anyMatch(r -> r.id().equals(past.id())
                && r.status() == ReservationStatus.COMPLETED));
    }

    @Test
    void aCompletedReservationCanNoLongerBeCancelled() {
        Flight yesterday = flight("AC500", "200.00", LocalDateTime.of(2026, 11, 30, 8, 0));
        Reservation past = confirmed(jo, yesterday, "1A", "300.00");
        controller.history(jo, jo.id());
        assertThrows(IllegalStateException.class, () -> controller.cancel(jo, past.id()));
    }

    @Test
    void whoMaySeeAHistory() {
        confirmed(jo, flightA, "1A", "300.00");
        assertThrows(AccessDeniedException.class, () -> controller.history(bo, jo.id()));
        assertEquals(1, controller.history(agent, jo.id()).size());
        assertEquals(1, controller.history(jo, jo.id()).size());
        assertThrows(AccessDeniedException.class, () -> controller.history(admin, jo.id()));
        assertThrows(AccessDeniedException.class, () -> controller.history(null, jo.id()));
        assertTrue(controller.history(agent, bo.id()).isEmpty());
    }
}
