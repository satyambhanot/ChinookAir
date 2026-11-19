package ca.ucalgary.chinookair.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Payment;
import ca.ucalgary.chinookair.business.entity.PaymentDetails;
import ca.ucalgary.chinookair.business.entity.PaymentMethodType;
import ca.ucalgary.chinookair.business.entity.PaymentStatus;
import ca.ucalgary.chinookair.business.entity.SeatLayout;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqlitePaymentDaoTest {

    private static final String MASKED = "**** **** **** 1111";

    private Connection db;
    private TestWorld world;
    private PaymentDao dao;
    private long joReservation;
    private long joSecondReservation;
    private long boReservation;

    @BeforeEach
    void open() {
        db = TestDatabase.inMemory();
        world = new TestWorld(db);
        dao = new SqlitePaymentDao(db);
        Customer bo = new SqliteCustomerDao(db).save(
                new Customer(null, "bwong", "pbkdf2$1$AAAA$BBBB", "Bo Wong", "bo@x.ca", null));
        Flight flight = new SqliteFlightDao(db).save(world.flight("AC123", world.yycYvr, world.ac, world.small,
                LocalDateTime.of(2026, 12, 1, 8, 0)));
        new SqliteSeatDao(db).createSeats(flight.id(), SeatLayout.generate(world.small));
        joReservation = world.reserve("CA000001", flight.id(), "1A", "PENDING_PAYMENT");
        joSecondReservation = world.reserve("CA000002", flight.id(), "1B", "PENDING_PAYMENT");
        boReservation = world.reserveFor(bo.id(), "CA000003", flight.id(), "2A", "PENDING_PAYMENT");
    }

    @AfterEach
    void close() throws SQLException {
        db.close();
    }

    private static Payment payment(long reservationId, String amount, int minute) {
        return new Payment(reservationId, new BigDecimal(amount), PaymentMethodType.CREDIT, MASKED,
                LocalDateTime.of(2026, 11, 20, 9, minute));
    }

    @Test
    void anAmountComesBackExactlyAsSaved() {
        Payment saved = dao.save(payment(joReservation, "249.99", 0));
        assertNotNull(saved.id());
        Payment found = dao.findById(saved.id()).orElseThrow();
        assertEquals(new BigDecimal("249.99"), found.amount());
        assertEquals(PaymentMethodType.CREDIT, found.type());
        assertEquals(PaymentStatus.PENDING, found.status());
        assertEquals(MASKED, found.maskedCard());
        assertEquals(joReservation, found.reservationId());
        assertEquals(LocalDateTime.of(2026, 11, 20, 9, 0), found.createdAt());
        assertNull(found.message());
    }

    @Test
    void awkwardAmountsSurviveTheTripToTheDatabase() {
        for (String amount : new String[] {"0.01", "0.10", "19.99", "1000.00", "4999.99", "5000.01", "12345.67"}) {
            Payment saved = dao.save(payment(joReservation, amount, 0));
            assertEquals(new BigDecimal(amount), dao.findById(saved.id()).orElseThrow().amount(), amount);
        }
    }

    @Test
    void anUnknownPaymentGivesAnEmptyResult() {
        assertTrue(dao.findById(99).isEmpty());
        assertTrue(dao.findByReservation(99).isEmpty());
        assertTrue(dao.findByCustomer(99).isEmpty());
    }

    @Test
    void updatePersistsTheNewStatusAndMessage() {
        Payment p = dao.save(payment(joReservation, "100.00", 0));
        p.approve();
        dao.update(p);
        Payment found = dao.findById(p.id()).orElseThrow();
        assertEquals(PaymentStatus.APPROVED, found.status());
        assertEquals("Approved", found.message());

        p.refund();
        dao.update(p);
        assertEquals(PaymentStatus.REFUNDED, dao.findById(p.id()).orElseThrow().status());
    }

    @Test
    void aDeclinedPaymentKeepsItsReason() {
        Payment p = dao.save(payment(joReservation, "100.00", 0));
        p.decline("Card declined");
        dao.update(p);
        Payment found = dao.findById(p.id()).orElseThrow();
        assertEquals(PaymentStatus.DECLINED, found.status());
        assertEquals("Card declined", found.message());
    }

    @Test
    void aReloadedPaymentStillFollowsTheStateRules() {
        Payment p = dao.save(payment(joReservation, "100.00", 0));
        p.decline("Card declined");
        dao.update(p);
        Payment found = dao.findById(p.id()).orElseThrow();
        assertThrows(IllegalStateException.class, found::refund);
    }

    @Test
    void findByReservationReturnsTheAttemptsInOrder() {
        Payment declined = dao.save(payment(joReservation, "100.00", 0));
        declined.decline("Card declined");
        dao.update(declined);
        Payment approved = dao.save(payment(joReservation, "100.00", 5));
        approved.approve();
        dao.update(approved);
        dao.save(payment(joSecondReservation, "50.00", 2));

        List<Payment> found = dao.findByReservation(joReservation);
        assertEquals(List.of(declined.id(), approved.id()), found.stream().map(Payment::id).toList());
        assertEquals(PaymentStatus.DECLINED, found.get(0).status());
        assertEquals(PaymentStatus.APPROVED, found.get(1).status());
    }

    @Test
    void findByCustomerSpansTheirReservationsNewestFirstAndOnlyTheirs() {
        Payment first = dao.save(payment(joReservation, "100.00", 0));
        Payment second = dao.save(payment(joSecondReservation, "200.00", 10));
        dao.save(payment(boReservation, "300.00", 5));

        List<Payment> jo = dao.findByCustomer(1);
        assertEquals(List.of(second.id(), first.id()), jo.stream().map(Payment::id).toList());
        assertEquals(1, dao.findByCustomer(2).size());
    }

    @Test
    void aPaymentNeedsAReservationThatExists() {
        assertThrows(DataAccessException.class, () -> dao.save(payment(999, "100.00", 0)));
    }

    @Test
    void aPaymentThatIsAlreadySavedCannotBeSavedAgain() {
        Payment saved = dao.save(payment(joReservation, "100.00", 0));
        assertThrows(IllegalArgumentException.class, () -> dao.save(saved));
    }

    @Test
    void updateNeedsASavedPayment() {
        assertThrows(IllegalArgumentException.class, () -> dao.update(payment(joReservation, "100.00", 0)));
        Payment ghost = Payment.restore(99L, joReservation, new BigDecimal("1.00"), PaymentMethodType.CREDIT,
                PaymentStatus.PENDING, MASKED, LocalDateTime.of(2026, 11, 20, 9, 0), null);
        assertThrows(DataAccessException.class, () -> dao.update(ghost));
    }

    @Test
    void onlyTheMaskedCardIsInTheRow() throws SQLException {
        PaymentDetails details = new PaymentDetails("Jo Smith", "4111 1111 1111 1111", "12/30");
        Payment p = new Payment(joReservation, new BigDecimal("249.99"), PaymentMethodType.CREDIT,
                details.maskedCard(), LocalDateTime.of(2026, 11, 20, 9, 0));
        p.approve();
        dao.save(p);
        dao.update(p);

        try (Statement st = db.createStatement();
             ResultSet rs = st.executeQuery("SELECT masked_card, message, amount_cents || method || status "
                     + "|| created_at AS rest FROM payment")) {
            assertTrue(rs.next());
            assertEquals("**** **** **** 1111", rs.getString("masked_card"));
            String everything = rs.getString("masked_card") + rs.getString("message") + rs.getString("rest");
            assertFalse(everything.matches(".*\\d{16}.*"), everything);
            assertFalse(everything.contains("4111"));
            assertFalse(rs.next());
        }
    }
}
