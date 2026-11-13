package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class InputParserTest {

    private static String messageOf(Runnable action) {
        return assertThrows(IllegalArgumentException.class, action::run).getMessage();
    }

    // ---- date

    @Test
    void aDateIsReadInIsoOrder() {
        assertEquals(LocalDate.of(2026, 12, 1), InputParser.date("2026-12-01"));
        assertEquals(LocalDate.of(2028, 2, 29), InputParser.date(" 2028-02-29 "));
    }

    @Test
    void aBadDateSaysHowToWriteIt() {
        for (String bad : new String[] {"2026-13-40", "12/01/2026", "2026-02-30", "2027-02-29", "", "  ", "tomorrow",
            "2026-1-1", null}) {
            assertTrue(messageOf(() -> InputParser.date(bad)).contains("yyyy-MM-dd"), String.valueOf(bad));
        }
    }

    // ---- date and time

    @Test
    void aDateAndTimeAreReadTogether() {
        assertEquals(LocalDateTime.of(2026, 12, 1, 8, 30), InputParser.dateTime("2026-12-01 08:30"));
        assertEquals(LocalDateTime.of(2026, 12, 1, 23, 59), InputParser.dateTime(" 2026-12-01 23:59 "));
        assertEquals(LocalDateTime.of(2026, 12, 1, 0, 0), InputParser.dateTime("2026-12-01 00:00"));
    }

    @Test
    void aBadDateAndTimeSaysHowToWriteIt() {
        for (String bad : new String[] {"2026-12-01", "2026-12-01 25:00", "2026-12-01 8:30", "2026-12-01T08:30", "",
            "08:30", null}) {
            assertTrue(messageOf(() -> InputParser.dateTime(bad)).contains("yyyy-MM-dd HH:mm"), String.valueOf(bad));
        }
    }

    // ---- money

    @Test
    void moneyKeepsTwoDecimals() {
        assertEquals(new BigDecimal("199.99"), InputParser.money("199.99"));
        assertEquals(new BigDecimal("200.00"), InputParser.money("200"));
        assertEquals(new BigDecimal("99.90"), InputParser.money(" 99.9 "));
        assertEquals(new BigDecimal("0.01"), InputParser.money("0.01"));
        assertEquals(new BigDecimal("200.00"), InputParser.money("200.000"));
    }

    @Test
    void moneyThatIsNotANumberSaysSo() {
        for (String bad : new String[] {"abc", "", "  ", "12,50", "1e3", "$5", "5.", ".5", null}) {
            assertTrue(messageOf(() -> InputParser.money(bad)).contains("number like 199.99"), String.valueOf(bad));
        }
    }

    @Test
    void moneyMustBeAboveZero() {
        assertTrue(messageOf(() -> InputParser.money("-5")).contains("greater than 0"));
        assertTrue(messageOf(() -> InputParser.money("0")).contains("greater than 0"));
        assertTrue(messageOf(() -> InputParser.money("0.00")).contains("greater than 0"));
    }

    @Test
    void moneyHasAtMostTwoDecimals() {
        assertTrue(messageOf(() -> InputParser.money("1.005")).contains("at most 2 decimals"));
    }

    @Test
    void theLabelNamesTheFieldInTheMessage() {
        assertTrue(messageOf(() -> InputParser.money("abc", "Base price")).startsWith("Base price must be"));
        assertTrue(messageOf(() -> InputParser.money("-1", "Base price")).startsWith("Base price must be"));
    }

    // ---- whole numbers

    @Test
    void aPositiveWholeNumberIsRead() {
        assertEquals(30, InputParser.positiveInt("30", "Rows"));
        assertEquals(6, InputParser.positiveInt(" 6 ", "Seats per row"));
    }

    @Test
    void aNumberThatIsNotPositiveNamesTheField() {
        assertTrue(messageOf(() -> InputParser.positiveInt("0", "Rows")).contains("Rows"));
        assertTrue(messageOf(() -> InputParser.positiveInt("-3", "Rows")).contains("greater than 0"));
    }

    @Test
    void textThatIsNotAWholeNumberNamesTheField() {
        for (String bad : new String[] {"abc", "", "2.5", "99999999999", null}) {
            String message = messageOf(() -> InputParser.positiveInt(bad, "Rows"));
            assertTrue(message.startsWith("Rows must be a whole number"), message);
        }
    }
}
