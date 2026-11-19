package ca.ucalgary.chinookair.business.control;

import ca.ucalgary.chinookair.business.entity.Payment;
import ca.ucalgary.chinookair.business.entity.Reservation;
import java.nio.file.Path;

/**
 * What came out of a payment attempt. The confirmation file is null when the payment was declined
 * or the file could not be written; the message then says what happened.
 */
public record PaymentResult(Payment payment, Reservation reservation, Path confirmationFile, String message) {
}
