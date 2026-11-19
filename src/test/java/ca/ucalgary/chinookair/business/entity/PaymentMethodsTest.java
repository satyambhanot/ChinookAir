package ca.ucalgary.chinookair.business.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PaymentMethodsTest {

    private static PaymentDetails card(String number) {
        return new PaymentDetails("Jo Smith", number, "12/30");
    }

    private static AuthorizationResult pay(PaymentMethodType type, String number, String amount) {
        return PaymentMethods.forType(type).authorize(card(number), new BigDecimal(amount));
    }

    @Test
    void theFactoryGivesTheMethodOfTheCardType() {
        assertInstanceOf(CreditCardPayment.class, PaymentMethods.forType(PaymentMethodType.CREDIT));
        assertInstanceOf(DebitCardPayment.class, PaymentMethods.forType(PaymentMethodType.DEBIT));
        assertThrows(IllegalArgumentException.class, () -> PaymentMethods.forType(null));
    }

    @Test
    void creditCardIsApproved() {
        AuthorizationResult result = pay(PaymentMethodType.CREDIT, "4111 1111 1111 1111", "250.00");
        assertTrue(result.approved());
    }

    @Test
    void aCardEndingInFourZerosIsDeclined() {
        AuthorizationResult credit = pay(PaymentMethodType.CREDIT, "4111111111110000", "250.00");
        assertFalse(credit.approved());
        assertEquals("Card declined", credit.message());
        AuthorizationResult debit = pay(PaymentMethodType.DEBIT, "4111111111110000", "250.00");
        assertFalse(debit.approved());
        assertEquals("Card declined", debit.message());
    }

    @Test
    void creditHasNoAmountLimit() {
        assertTrue(pay(PaymentMethodType.CREDIT, "4111111111111111", "6000.00").approved());
        assertTrue(pay(PaymentMethodType.CREDIT, "4111111111111111", "50000.00").approved());
    }

    @Test
    void debitOverFiveThousandIsDeclined() {
        AuthorizationResult over = pay(PaymentMethodType.DEBIT, "4111111111111111", "6000.00");
        assertFalse(over.approved());
        assertTrue(over.message().contains("5000.00"));
        assertFalse(pay(PaymentMethodType.DEBIT, "4111111111111111", "5000.01").approved());
    }

    @Test
    void debitUpToFiveThousandIsApproved() {
        assertTrue(pay(PaymentMethodType.DEBIT, "4111111111111111", "5000.00").approved());
        assertTrue(pay(PaymentMethodType.DEBIT, "4111111111111111", "5000").approved());
        assertTrue(pay(PaymentMethodType.DEBIT, "4111111111111111", "250.00").approved());
    }
}
