package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class FormatTest {

    private static final LocalDateTime MORNING = LocalDateTime.of(2026, 12, 2, 7, 0);

    @Test
    void aDayIsWrittenWithItsWeekday() {
        assertEquals("Wed, Dec 2", Format.date(LocalDate.of(2026, 12, 2)));
        assertEquals("Sun, Jan 31", Format.date(LocalDate.of(2027, 1, 31)));
    }

    @Test
    void timesUseTheTwelveHourClockWithoutALeadingZero() {
        assertEquals("7:00 AM", Format.time(MORNING));
        assertEquals("12:00 PM", Format.time(MORNING.withHour(12)));
        assertEquals("12:05 AM", Format.time(MORNING.withHour(0).withMinute(5)));
        assertEquals("1:05 PM", Format.time(MORNING.withHour(13).withMinute(5)));
        assertEquals("11:59 PM", Format.time(MORNING.withHour(23).withMinute(59)));
    }

    @Test
    void aDayAndATimeAreJoinedByADot() {
        assertEquals("Wed, Dec 2 · 7:00 AM", Format.dateTime(MORNING));
        assertEquals("Wed, Dec 2, 2026 · 7:00 AM", Format.dateTimeWithYear(MORNING));
    }

    @Test
    void theComputersLanguageDoesNotChangeTheWords() {
        Locale before = Locale.getDefault();
        try {
            Locale.setDefault(Locale.FRANCE);
            assertEquals("Wed, Dec 2 · 7:00 AM", Format.dateTime(MORNING));
        } finally {
            Locale.setDefault(before);
        }
    }

    @Test
    void inputFieldsKeepTheStrictFormThatTheParserReadsBack() {
        assertEquals("2026-12-02 07:00", Format.input(MORNING));
        assertEquals(MORNING, InputParser.dateTime(Format.input(MORNING)));
    }

    @Test
    void moneyKeepsItsTwoDecimals() {
        assertEquals("$184.00", Format.money(new BigDecimal("184.00")));
        assertEquals("$1000.50", Format.money(new BigDecimal("1000.50")));
    }
}
