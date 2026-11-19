package ca.ucalgary.chinookair.business.entity;

import java.time.Clock;
import java.time.YearMonth;
import java.util.regex.Pattern;

/**
 * What the customer types on the payment screen. The full card number lives
 * here only until the payment is authorized: after that only the masked
 * number is kept, and toString never shows the full number.
 */
public record PaymentDetails(String cardholderName, String cardNumber, String expiry) {

    private static final Pattern EXPIRY = Pattern.compile("(0[1-9]|1[0-2])/(\\d{2})");

    /**
     * Checks the input; the messages are the ones the payment screen shows.
     *
     * @throws IllegalArgumentException if the name is blank, the card number is not 16 digits,
     *                                  or the expiry is not MM/yy or is before the current month
     */
    public void validate(Clock clock) {
        if (cardholderName == null || cardholderName.isBlank()) {
            throw new IllegalArgumentException("Cardholder name must not be blank");
        }
        if (!digits().matches("\\d{16}")) {
            throw new IllegalArgumentException("Card number must be 16 digits");
        }
        if (expiry == null || !EXPIRY.matcher(expiry.trim()).matches()) {
            throw new IllegalArgumentException("Expiry date must look like MM/yy");
        }
        String[] parts = expiry.trim().split("/");
        YearMonth expires = YearMonth.of(2000 + Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
        if (expires.isBefore(YearMonth.now(clock))) {
            throw new IllegalArgumentException("Card has expired");
        }
    }

    /** The card number without spaces. */
    public String digits() {
        return cardNumber == null ? "" : cardNumber.replace(" ", "");
    }

    /** The last four digits of the card. */
    public String last4() {
        String digits = digits();
        if (digits.length() < 4) {
            throw new IllegalArgumentException("Card number must be 16 digits");
        }
        return digits.substring(digits.length() - 4);
    }

    /** The only form of the card number that is stored or shown, like **** **** **** 1111. */
    public String maskedCard() {
        return "**** **** **** " + last4();
    }

    @Override
    public String toString() {
        // never throws, so it is safe in a log line even for a half-typed number
        String card = digits().length() >= 4 ? maskedCard() : "****";
        return "PaymentDetails[" + cardholderName + ", " + card + "]";
    }
}
