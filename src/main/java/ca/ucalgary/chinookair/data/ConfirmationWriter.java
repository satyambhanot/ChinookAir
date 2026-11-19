package ca.ucalgary.chinookair.data;

import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Payment;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.Seat;
import ca.ucalgary.chinookair.business.entity.SeatLayout;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;

/**
 * Writes the booking confirmation of a reservation as a small text file, one file for each
 * confirmation number. This is the file access of the data layer.
 */
public class ConfirmationWriter {

    private static final DateTimeFormatter DEPARTURE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Path directory;

    /** Writes to the default folder, ~/.chinookair/confirmations. */
    public ConfirmationWriter() {
        this(Paths.get(System.getProperty("user.home"), ".chinookair", "confirmations"));
    }

    public ConfirmationWriter(Path directory) {
        if (directory == null) {
            throw new IllegalArgumentException("A folder is required");
        }
        this.directory = directory;
    }

    /**
     * Writes {@code <confirmationNumber>.txt}, replacing the file of an earlier write for the same
     * reservation, for example after the reservation was modified. The full card number is never
     * in the file because the payment only knows the masked card.
     *
     * @return the file that was written
     * @throws IllegalArgumentException   if the reservation has no seat or the payment is for another reservation
     * @throws ConfirmationWriteException if the folder or the file cannot be written
     */
    public Path write(Reservation reservation, Payment payment) {
        if (reservation == null || payment == null) {
            throw new IllegalArgumentException("A reservation and a payment are required");
        }
        if (reservation.seatNo() == null) {
            throw new IllegalArgumentException("The reservation has no seat yet");
        }
        if (!payment.reservationId().equals(reservation.id())) {
            throw new IllegalArgumentException("That payment belongs to another reservation");
        }
        String number = reservation.confirmationNumber();
        // the number becomes a file name, so nothing like ../ may get through
        if (!number.matches("[A-Za-z0-9]+")) {
            throw new ConfirmationWriteException("The confirmation number " + number + " cannot be a file name");
        }
        String text = content(reservation, payment);
        Path target = directory.resolve(number + ".txt");
        Path temporary = directory.resolve(number + ".txt.tmp");
        try {
            Files.createDirectories(directory);
            // write the whole file first, so a crash never leaves half a confirmation behind
            Files.writeString(temporary, text, StandardCharsets.UTF_8);
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            deleteQuietly(temporary);
            throw new ConfirmationWriteException("Could not write the confirmation file " + target, e);
        }
        return target;
    }

    private static String content(Reservation reservation, Payment payment) {
        Flight flight = reservation.flight();
        String lineEnd = System.lineSeparator();
        return "Confirmation: " + reservation.confirmationNumber() + lineEnd
                + "Passenger: " + reservation.customer().name() + lineEnd
                + "Flight: " + flight.flightNumber() + " (" + flight.airline().name() + ")" + lineEnd
                + "Route: " + flight.route().origin().code() + " (" + flight.route().origin().city() + ") to "
                + flight.route().destination().code() + " (" + flight.route().destination().city() + ")" + lineEnd
                + "Departure: " + DEPARTURE.format(flight.departure()) + lineEnd
                + "Seat: " + reservation.seatNo() + " (" + seatClass(flight, reservation.seatNo()) + ")" + lineEnd
                + "Amount paid: " + payment.amount().setScale(2, RoundingMode.HALF_UP).toPlainString() + lineEnd
                + "Card: " + payment.maskedCard() + lineEnd
                + "Payment status: " + payment.status() + lineEnd;
    }

    private static String seatClass(Flight flight, String seatNo) {
        return SeatLayout.generate(flight.aircraft()).stream().filter(s -> s.number().equals(seatNo))
                .map(Seat::seatClass).findFirst()
                .orElseThrow(() -> new ConfirmationWriteException("Seat " + seatNo + " is not on the aircraft"))
                .name();
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            // the real problem is already being reported
        }
    }
}
