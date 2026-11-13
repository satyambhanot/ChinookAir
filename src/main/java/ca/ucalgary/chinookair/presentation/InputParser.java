package ca.ucalgary.chinookair.presentation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * Turns the text of a form field into a value, or says in plain words what the field should look
 * like. It only parses; whether the value makes sense is for the controllers to decide.
 */
final class InputParser {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT);

    private InputParser() {
    }

    /** A day written like 2026-12-01. */
    static LocalDate date(String text) {
        try {
            return LocalDate.parse(clean(text), DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Date must look like yyyy-MM-dd, for example 2026-12-01");
        }
    }

    /** A day and a time written like 2026-12-01 08:30. */
    static LocalDateTime dateTime(String text) {
        try {
            return LocalDateTime.parse(clean(text), DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Date and time must look like yyyy-MM-dd HH:mm, for example 2026-12-01 08:30");
        }
    }

    /** An amount of money with at most 2 decimals, greater than 0. */
    static BigDecimal money(String text) {
        return money(text, "Amount");
    }

    static BigDecimal money(String text, String label) {
        String value = clean(text);
        if (!value.matches("-?\\d+(\\.\\d+)?")) {
            throw new IllegalArgumentException(label + " must be a number like 199.99");
        }
        BigDecimal amount = new BigDecimal(value);
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException(label + " must be greater than 0");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(label + " can have at most 2 decimals");
        }
        return amount.setScale(2);
    }

    /** A whole number above 0. The label names the field in the message. */
    static int positiveInt(String text, String label) {
        String value = clean(text);
        int number;
        try {
            number = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a whole number");
        }
        if (number <= 0) {
            throw new IllegalArgumentException(label + " must be greater than 0");
        }
        return number;
    }

    private static String clean(String text) {
        return text == null ? "" : text.trim();
    }
}
