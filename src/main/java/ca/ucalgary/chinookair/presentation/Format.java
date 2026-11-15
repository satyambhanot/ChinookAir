package ca.ucalgary.chinookair.presentation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * How the screens write dates, times and money, in one place. What a person reads is friendly
 * ("Wed, Dec 2 · 7:00 AM"); what a person types into a field stays the strict form that
 * {@link InputParser} reads back ("2026-12-02 07:00").
 */
final class Format {

    private static final Locale LOCALE = Locale.ENGLISH;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE, MMM d", LOCALE);
    private static final DateTimeFormatter DAY_WITH_YEAR = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", LOCALE);
    private static final DateTimeFormatter SHORT_DAY = DateTimeFormatter.ofPattern("EEE d", LOCALE);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", LOCALE);
    private static final DateTimeFormatter INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String SEPARATOR = " · ";

    private Format() {
    }

    /** A day, like "Wed, Dec 2". */
    static String date(LocalDate date) {
        return date.format(DAY);
    }

    /** A day in a few characters, like "Wed 2". */
    static String shortDate(LocalDate date) {
        return date.format(SHORT_DAY);
    }

    /** A time of day, like "7:00 AM". */
    static String time(LocalDateTime when) {
        return when.format(TIME);
    }

    /** A day and a time, like "Wed, Dec 2 · 7:00 AM". */
    static String dateTime(LocalDateTime when) {
        return when.format(DAY) + SEPARATOR + when.format(TIME);
    }

    /** Like {@link #dateTime} with the year, for a summary people keep: "Wed, Dec 2, 2026 · 7:00 AM". */
    static String dateTimeWithYear(LocalDateTime when) {
        return when.format(DAY_WITH_YEAR) + SEPARATOR + when.format(TIME);
    }

    /** A date and time as the input fields hold it, like "2026-12-02 07:00". */
    static String input(LocalDateTime when) {
        return when.format(INPUT);
    }

    /** How long a flight takes, like "1h 30m", "45m" or "2h". */
    static String duration(LocalDateTime departure, LocalDateTime arrival) {
        long minutes = java.time.Duration.between(departure, arrival).toMinutes();
        long hours = minutes / 60;
        long rest = minutes % 60;
        if (hours == 0) {
            return rest + "m";
        }
        return rest == 0 ? hours + "h" : hours + "h " + rest + "m";
    }

    /** A seat and its class, like "5C (Economy)"; just the number when the seat is not on the plane. */
    static String seat(ca.ucalgary.chinookair.business.entity.Flight flight, String seatNo) {
        for (ca.ucalgary.chinookair.business.entity.Seat seat
                : ca.ucalgary.chinookair.business.entity.SeatLayout.generate(flight.aircraft())) {
            if (seat.number().equals(seatNo)) {
                return seatNo + " ("
                        + (seat.seatClass() == ca.ucalgary.chinookair.business.entity.SeatClass.BUSINESS
                                ? "Business" : "Economy") + ")";
            }
        }
        return seatNo;
    }

    /** An amount of money, like "$184.00". */
    static String money(BigDecimal amount) {
        return "$" + amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
