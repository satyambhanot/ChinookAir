package ca.ucalgary.chinookair.business.entity;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PaymentDetailsTest {

    // December 2026
    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private static PaymentDetails details(String name, String number, String expiry) {
        return new PaymentDetails(name, number, expiry);
    }

    private static String messageOf(PaymentDetails d) {
        return assertThrows(IllegalArgumentException.class, () -> d.validate(CLOCK)).getMessage();
    }

    @Test
    void goodDetailsPassEvenWithSpaces() {
        assertDoesNotThrow(() -> details("Jo Smith", "4111 1111 1111 1111", "12/30").validate(CLOCK));
        assertDoesNotThrow(() -> details("Jo Smith", "4111111111111111", "12/30").validate(CLOCK));
    }

    @Test
    void cardNumberMustBeSixteenDigits() {
        assertTrue(messageOf(details("Jo Smith", "411111111111111", "12/30")).contains("16 digits"));
        assertTrue(messageOf(details("Jo Smith", "41111111111111111", "12/30")).contains("16 digits"));
        assertTrue(messageOf(details("Jo Smith", "4111 1111 1111 111a", "12/30")).contains("16 digits"));
        assertTrue(messageOf(details("Jo Smith", null, "12/30")).contains("16 digits"));
    }

    @Test
    void anExpiredCardIsRefused() {
        assertTrue(messageOf(details("Jo Smith", "4111111111111111", "01/20")).contains("expired"));
        assertTrue(messageOf(details("Jo Smith", "4111111111111111", "11/26")).contains("expired"));
    }

    @Test
    void theCurrentMonthIsStillValid() {
        assertDoesNotThrow(() -> details("Jo Smith", "4111111111111111", "12/26").validate(CLOCK));
    }

    @Test
    void expiryMustLookLikeMonthSlashYear() {
        assertTrue(messageOf(details("Jo Smith", "4111111111111111", "13/30")).contains("MM/yy"));
        assertTrue(messageOf(details("Jo Smith", "4111111111111111", "1230")).contains("MM/yy"));
        assertTrue(messageOf(details("Jo Smith", "4111111111111111", "00/30")).contains("MM/yy"));
        assertTrue(messageOf(details("Jo Smith", "4111111111111111", null)).contains("MM/yy"));
    }

    @Test
    void nameMustNotBeBlank() {
        assertTrue(messageOf(details(" ", "4111111111111111", "12/30")).contains("name"));
        assertTrue(messageOf(details(null, "4111111111111111", "12/30")).contains("name"));
    }

    @Test
    void theCardIsMaskedToTheLastFourDigits() {
        PaymentDetails d = details("Jo Smith", "4111 1111 1111 1234", "12/30");
        assertEquals("1234", d.last4());
        assertEquals("**** **** **** 1234", d.maskedCard());
        assertEquals("4111111111111234", d.digits());
    }

    @Test
    void toStringNeverShowsTheFullNumber() {
        PaymentDetails d = details("Jo Smith", "4111 1111 1111 1234", "12/30");
        assertFalse(d.toString().contains("4111"));
        assertTrue(d.toString().contains("1234"));
        assertDoesNotThrow(() -> details("Jo", "12", "12/30").toString());
        assertDoesNotThrow(() -> details("Jo", null, null).toString());
    }
}
