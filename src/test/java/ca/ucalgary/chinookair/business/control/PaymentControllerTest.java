package ca.ucalgary.chinookair.business.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Administrator;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.FlightAgent;
import ca.ucalgary.chinookair.business.entity.Payment;
import ca.ucalgary.chinookair.business.entity.PaymentDetails;
import ca.ucalgary.chinookair.business.entity.PaymentMethodType;
import ca.ucalgary.chinookair.business.entity.PaymentStatus;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.ReservationStatus;
import ca.ucalgary.chinookair.business.entity.SeatLayout;
import ca.ucalgary.chinookair.data.ConfirmationWriter;
import ca.ucalgary.chinookair.data.DaoFactory;
import ca.ucalgary.chinookair.data.TestDatabase;
import ca.ucalgary.chinookair.data.TestWorld;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PaymentControllerTest {

    private static final String HASH = "pbkdf2$1$AAAA$BBBB";
    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    private static final PaymentDetails GOOD = new PaymentDetails("Jo Smith", "4111 1111 1111 1111", "12/30");
    private static final PaymentDetails DECLINED_CARD = new PaymentDetails("Jo Smith", "4111111111110000", "12/30");

    @TempDir
    Path folder;

    private Connection db;
    private DaoFactory daos;
    private PaymentController controller;
    private Customer jo;
    private Customer bo;
    private FlightAgent agent;
    private Administrator admin;
    private Flight flight;
    private int numbers;

    @BeforeEach
    void open() {
        db = TestDatabase.inMemory();
        TestWorld world = new TestWorld(db);
        daos = DaoFactory.create(db);
        controller = controllerWriting(folder);
        jo = daos.customerDao().findById(1).orElseThrow();
        bo = daos.customerDao().save(new Customer(null, "bwong", HASH, "Bo Wong", "bo@x.ca", null));
        agent = (FlightAgent) daos.userDao().save(new FlightAgent(null, "agent1", HASH));
        admin = (Administrator) daos.userDao().save(new Administrator(null, "admin", HASH));
        flight = daos.flightDao().save(world.flight("AC123", world.yycYvr, world.ac, world.small,
                LocalDateTime.of(2026, 12, 2, 8, 0)));
        daos.seatDao().createSeats(flight.id(), SeatLayout.generate(world.small));
    }

    @AfterEach
    void close() throws SQLException {
        db.close();
    }

    private PaymentController controllerWriting(Path directory) {
        return new PaymentController(daos.reservationDao(), daos.paymentDao(), new ConfirmationWriter(directory),
                CLOCK);
    }

    /** A reservation of the customer that waits for payment at the given price. */
    private Reservation pending(Customer customer, String seat, String price) {
        LocalDateTime created = LocalDateTime.of(2026, 11, 20, 9, 0);
        Reservation r = new Reservation("CA00000" + (++numbers), customer, flight, created);
        r.assignSeat(seat, new BigDecimal(price));
        return daos.reservationDao().save(r);
    }

    private ReservationStatus statusOf(Reservation r) {
        return daos.reservationDao().findById(r.id()).orElseThrow().status();
    }

    private List<String> filesInFolder() throws IOException {
        try (Stream<Path> files = Files.list(folder)) {
            return files.map(p -> p.getFileName().toString()).toList();
        }
    }

    // ---- approved

    @Test
    void aGoodCardApprovesThePaymentAndConfirmsTheReservation() throws IOException {
        Reservation r = pending(jo, "1A", "300.00");
        PaymentResult result = controller.pay(jo, r.id(), PaymentMethodType.CREDIT, GOOD);

        assertEquals(PaymentStatus.APPROVED, result.payment().status());
        assertEquals(new BigDecimal("300.00"), result.payment().amount());
        assertEquals(ReservationStatus.CONFIRMED, result.reservation().status());
        assertEquals(ReservationStatus.CONFIRMED, statusOf(r));
        assertEquals("Payment approved", result.message());
        assertNotNull(result.confirmationFile());
        assertTrue(Files.exists(result.confirmationFile()));
        assertEquals(List.of(r.confirmationNumber() + ".txt"), filesInFolder());
    }

    @Test
    void theApprovedPaymentIsSavedWithOnlyTheMaskedCard() {
        Reservation r = pending(jo, "1A", "300.00");
        controller.pay(jo, r.id(), PaymentMethodType.CREDIT, GOOD);
        Payment stored = daos.paymentDao().findByReservation(r.id()).get(0);
        assertEquals(PaymentStatus.APPROVED, stored.status());
        assertTrue(stored.maskedCard().endsWith("1111"));
        assertEquals("**** **** **** 1111", stored.maskedCard());
        assertFalse(stored.toString().contains("4111"));
        assertEquals(PaymentMethodType.CREDIT, stored.type());
    }

    @Test
    void theConfirmationFileHoldsTheBookingAndNoFullCardNumber() throws IOException {
        Reservation r = pending(jo, "1A", "300.00");
        PaymentResult result = controller.pay(jo, r.id(), PaymentMethodType.CREDIT, GOOD);
        String text = Files.readString(result.confirmationFile());
        assertTrue(text.contains("Seat: 1A (BUSINESS)"), text);
        assertTrue(text.contains("Amount paid: 300.00"), text);
        assertFalse(text.contains("4111111111111111"));
    }

    @Test
    void aDebitCardUpToTheLimitIsApproved() {
        Reservation r = pending(jo, "1A", "5000.00");
        assertEquals(PaymentStatus.APPROVED,
                controller.pay(jo, r.id(), PaymentMethodType.DEBIT, GOOD).payment().status());
    }

    // ---- declined

    @Test
    void aDeclinedCardLeavesTheReservationWaitingAndWritesNoFile() throws IOException {
        Reservation r = pending(jo, "1A", "300.00");
        PaymentResult result = controller.pay(jo, r.id(), PaymentMethodType.CREDIT, DECLINED_CARD);

        assertEquals(PaymentStatus.DECLINED, result.payment().status());
        assertEquals("Card declined", result.message());
        assertEquals("Card declined", result.payment().message());
        assertNull(result.confirmationFile());
        assertEquals(ReservationStatus.PENDING_PAYMENT, statusOf(r));
        assertTrue(filesInFolder().isEmpty());
        assertEquals(1, daos.paymentDao().findByReservation(r.id()).size());
    }

    @Test
    void aDeclineFollowedByAGoodCardGivesTwoPaymentsAndAConfirmedReservation() {
        Reservation r = pending(jo, "1A", "300.00");
        controller.pay(jo, r.id(), PaymentMethodType.CREDIT, DECLINED_CARD);
        controller.pay(jo, r.id(), PaymentMethodType.CREDIT, GOOD);

        List<Payment> payments = daos.paymentDao().findByReservation(r.id());
        assertEquals(2, payments.size());
        assertEquals(PaymentStatus.DECLINED, payments.get(0).status());
        assertEquals(PaymentStatus.APPROVED, payments.get(1).status());
        assertEquals(ReservationStatus.CONFIRMED, statusOf(r));
    }

    @Test
    void aDebitCardOverTheLimitIsDeclined() {
        Reservation r = pending(jo, "1A", "6000.00");
        PaymentResult result = controller.pay(jo, r.id(), PaymentMethodType.DEBIT, GOOD);
        assertEquals(PaymentStatus.DECLINED, result.payment().status());
        assertTrue(result.message().contains("5000.00"), result.message());
        assertEquals(ReservationStatus.PENDING_PAYMENT, statusOf(r));
    }

    @Test
    void aCreditCardHasNoLimit() {
        Reservation r = pending(jo, "1A", "6000.00");
        assertEquals(PaymentStatus.APPROVED,
                controller.pay(jo, r.id(), PaymentMethodType.CREDIT, GOOD).payment().status());
    }

    // ---- who and when

    @Test
    void aReservationThatIsNotWaitingForPaymentCannotBePaid() {
        Reservation confirmed = pending(jo, "1A", "300.00");
        controller.pay(jo, confirmed.id(), PaymentMethodType.CREDIT, GOOD);
        assertThrows(IllegalStateException.class,
                () -> controller.pay(jo, confirmed.id(), PaymentMethodType.CREDIT, GOOD));
        assertEquals(1, daos.paymentDao().findByReservation(confirmed.id()).size());

        Reservation noSeat = daos.reservationDao().save(
                new Reservation("CA9999999", jo, flight, LocalDateTime.of(2026, 11, 20, 9, 0)));
        assertThrows(IllegalStateException.class,
                () -> controller.pay(jo, noSeat.id(), PaymentMethodType.CREDIT, GOOD));

        Reservation cancelled = pending(jo, "2A", "300.00");
        cancelled.cancel();
        daos.reservationDao().update(cancelled);
        assertThrows(IllegalStateException.class,
                () -> controller.pay(jo, cancelled.id(), PaymentMethodType.CREDIT, GOOD));
    }

    @Test
    void aCustomerCannotPayForAnotherCustomersReservationButAnAgentCan() {
        Reservation r = pending(jo, "1A", "300.00");
        assertThrows(AccessDeniedException.class, () -> controller.pay(bo, r.id(), PaymentMethodType.CREDIT, GOOD));
        assertEquals(ReservationStatus.PENDING_PAYMENT, statusOf(r));
        assertTrue(daos.paymentDao().findByReservation(r.id()).isEmpty());

        assertEquals(PaymentStatus.APPROVED,
                controller.pay(agent, r.id(), PaymentMethodType.CREDIT, GOOD).payment().status());
    }

    @Test
    void anAdministratorAndNobodyCanPay() {
        Reservation r = pending(jo, "1A", "300.00");
        assertThrows(AccessDeniedException.class, () -> controller.pay(admin, r.id(), PaymentMethodType.CREDIT, GOOD));
        assertThrows(AccessDeniedException.class, () -> controller.pay(null, r.id(), PaymentMethodType.CREDIT, GOOD));
    }

    @Test
    void anUnknownReservationIsAnError() {
        assertThrows(IllegalArgumentException.class, () -> controller.pay(jo, 999, PaymentMethodType.CREDIT, GOOD));
    }

    // ---- invalid details

    @Test
    void invalidDetailsAreRefusedAndNothingIsSaved() {
        Reservation r = pending(jo, "1A", "300.00");
        PaymentDetails shortNumber = new PaymentDetails("Jo Smith", "411111111111111", "12/30");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> controller.pay(jo, r.id(), PaymentMethodType.CREDIT, shortNumber));
        assertTrue(e.getMessage().contains("16 digits"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> controller.pay(jo, r.id(), PaymentMethodType.CREDIT,
                new PaymentDetails("Jo Smith", "4111111111111111", "01/20")));
        assertThrows(IllegalArgumentException.class, () -> controller.pay(jo, r.id(), PaymentMethodType.CREDIT,
                new PaymentDetails(" ", "4111111111111111", "12/30")));

        assertTrue(daos.paymentDao().findByReservation(r.id()).isEmpty());
        assertEquals(ReservationStatus.PENDING_PAYMENT, statusOf(r));
    }

    @Test
    void theTypeAndTheDetailsAreRequired() {
        Reservation r = pending(jo, "1A", "300.00");
        assertThrows(IllegalArgumentException.class, () -> controller.pay(jo, r.id(), null, GOOD));
        assertThrows(IllegalArgumentException.class, () -> controller.pay(jo, r.id(), PaymentMethodType.CREDIT, null));
    }

    // ---- the file is not part of the payment

    @Test
    void aFileProblemOnlyAddsAWarningAndTheCustomerStaysPaid() throws IOException {
        Path notAFolder = Files.createFile(folder.resolve("plain-file"));
        PaymentController broken = controllerWriting(notAFolder);
        Reservation r = pending(jo, "1A", "300.00");

        PaymentResult result = broken.pay(jo, r.id(), PaymentMethodType.CREDIT, GOOD);

        assertEquals(PaymentStatus.APPROVED, result.payment().status());
        assertNull(result.confirmationFile());
        assertTrue(result.message().startsWith("Payment approved, but the confirmation file"), result.message());
        assertEquals(ReservationStatus.CONFIRMED, statusOf(r));
        assertEquals(PaymentStatus.APPROVED, daos.paymentDao().findByReservation(r.id()).get(0).status());
    }

    // ---- payment records

    @Test
    void aCustomerSeesTheirOwnPaymentsNewestFirst() {
        Reservation first = pending(jo, "1A", "300.00");
        Reservation second = pending(jo, "1B", "300.00");
        controller.pay(jo, first.id(), PaymentMethodType.CREDIT, GOOD);
        controller.pay(jo, second.id(), PaymentMethodType.CREDIT, GOOD);
        List<Payment> records = controller.recordsForCustomer(jo, jo.id());
        assertEquals(2, records.size());
        assertEquals(second.id(), records.get(0).reservationId());
    }

    @Test
    void recordsOfAnotherCustomerAreOnlyForAgents() {
        Reservation r = pending(jo, "1A", "300.00");
        controller.pay(jo, r.id(), PaymentMethodType.CREDIT, GOOD);
        assertThrows(AccessDeniedException.class, () -> controller.recordsForCustomer(bo, jo.id()));
        assertEquals(1, controller.recordsForCustomer(agent, jo.id()).size());
        assertThrows(AccessDeniedException.class, () -> controller.recordsForCustomer(admin, jo.id()));
        assertThrows(AccessDeniedException.class, () -> controller.recordsForCustomer(null, jo.id()));
        assertTrue(controller.recordsForCustomer(agent, bo.id()).isEmpty());
    }
}
