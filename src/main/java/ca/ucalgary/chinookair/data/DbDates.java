package ca.ucalgary.chinookair.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Dates and times go into the database as ISO-8601 text with seconds, like
 * 2026-12-01T08:00:00. All of them have the same length and layout, so
 * comparing them as text is the same as comparing them as times.
 */
final class DbDates {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private DbDates() {
    }

    static String format(LocalDateTime time) {
        return FORMAT.format(time);
    }

    static LocalDateTime parse(String text) {
        return LocalDateTime.parse(text, FORMAT);
    }

    /** The first moment of the day. */
    static String startOf(LocalDate day) {
        return format(day.atStartOfDay());
    }
}
