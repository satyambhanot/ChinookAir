package ca.ucalgary.chinookair.business.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PaymentTest {

    private static final String MASKED = "**** **** **** 1111";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 9, 0);

    private enum Action { APPROVE, DECLINE, REFUND }

    private static Payment payment() {
        return new Payment(7L, new BigDecimal("250.00"), PaymentMethodType.CREDIT, MASKED, NOW);
    }

    private static Payment in(PaymentStatus status) {
        return Payment.restore(1L, 7L, new BigDecimal("250.00"), PaymentMethodType.CREDIT, status, MASKED, NOW, "x");
    }

    private static void perform(Payment p, Action action) {
        switch (action) {
            case APPROVE:
                p.approve();
                break;
            case DECLINE:
                p.decline("Card declined");
                break;
            case REFUND:
                p.refund();
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    static Stream<Arguments> legalTransitions() {
        return Stream.of(
                Arguments.of(PaymentStatus.PENDING, Action.APPROVE, PaymentStatus.APPROVED),
                Arguments.of(PaymentStatus.PENDING, Action.DECLINE, PaymentStatus.DECLINED),
                Arguments.of(PaymentStatus.APPROVED, Action.REFUND, PaymentStatus.REFUNDED));
    }

    static Stream<Arguments> illegalTransitions() {
        return Stream.of(PaymentStatus.values())
                .flatMap(s -> Stream.of(Action.values()).map(a -> Arguments.of(s, a)))
                .filter(args -> legalTransitions().noneMatch(
                        l -> l.get()[0] == args.get()[0] && l.get()[1] == args.get()[1]));
    }

    @ParameterizedTest(name = "{0} + {1} gives {2}")
    @MethodSource("legalTransitions")
    void legalTransitionsReachTheNextStatus(PaymentStatus from, Action action, PaymentStatus to) {
        Payment p = in(from);
        perform(p, action);
        assertEquals(to, p.status());
    }

    @ParameterizedTest(name = "{0} + {1} is refused")
    @MethodSource("illegalTransitions")
    void everyOtherActionIsRefusedAndChangesNothing(PaymentStatus from, Action action) {
        Payment p = in(from);
        assertThrows(IllegalStateException.class, () -> perform(p, action));
        assertEquals(from, p.status());
        assertEquals("x", p.message());
    }

    @Test
    void aNewPaymentIsPendingWithoutAnIdOrAMessage() {
        Payment p = payment();
        assertEquals(PaymentStatus.PENDING, p.status());
        assertNull(p.id());
        assertNull(p.message());
        assertEquals(7L, p.reservationId());
        assertEquals(MASKED, p.maskedCard());
    }

    @Test
    void declineKeepsTheReason() {
        Payment p = payment();
        p.decline(" Card declined ");
        assertEquals("Card declined", p.message());
        assertThrows(IllegalArgumentException.class, () -> payment().decline(" "));
        assertThrows(IllegalArgumentException.class, () -> payment().decline(null));
    }

    @Test
    void approvedPaymentsCanBeRefundedOnce() {
        Payment p = payment();
        p.approve();
        p.refund();
        assertEquals(PaymentStatus.REFUNDED, p.status());
        assertThrows(IllegalStateException.class, p::refund);
    }

    @Test
    void onlyAMaskedCardCanBeStored() {
        BigDecimal amount = new BigDecimal("250.00");
        assertThrows(IllegalArgumentException.class,
                () -> new Payment(7L, amount, PaymentMethodType.CREDIT, "4111111111111111", NOW));
        assertThrows(IllegalArgumentException.class,
                () -> new Payment(7L, amount, PaymentMethodType.CREDIT, "**** 1111", NOW));
        assertThrows(IllegalArgumentException.class,
                () -> new Payment(7L, amount, PaymentMethodType.CREDIT, null, NOW));
    }

    @Test
    void aPaymentNeedsAReservationAndAPositiveAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> new Payment(null, new BigDecimal("1.00"), PaymentMethodType.CREDIT, MASKED, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> new Payment(7L, BigDecimal.ZERO, PaymentMethodType.CREDIT, MASKED, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> new Payment(7L, null, PaymentMethodType.CREDIT, MASKED, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> new Payment(7L, new BigDecimal("1.00"), null, MASKED, NOW));
    }

    @Test
    void theIdCanBeAssignedOnlyOnce() {
        Payment p = payment();
        p.assignId(3L);
        assertEquals(3L, p.id());
        assertThrows(IllegalStateException.class, () -> p.assignId(4L));
    }

    @Test
    void toStringShowsTheMaskedCardOnly() {
        PaymentDetails details = new PaymentDetails("Jo Smith", "4111 1111 1111 1111", "12/30");
        Payment p = new Payment(7L, new BigDecimal("250.00"), PaymentMethodType.CREDIT, details.maskedCard(), NOW);
        assertTrue(p.toString().contains(MASKED));
        assertFalse(p.toString().contains("4111"));
    }
}
