package ca.ucalgary.chinookair.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Aircraft;
import ca.ucalgary.chinookair.business.entity.Airline;
import ca.ucalgary.chinookair.business.entity.Airport;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Payment;
import ca.ucalgary.chinookair.business.entity.PaymentDetails;
import ca.ucalgary.chinookair.business.entity.PaymentMethodType;
import ca.ucalgary.chinookair.business.entity.PaymentStatus;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.ReservationStatus;
import ca.ucalgary.chinookair.business.entity.Route;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfirmationWriterTest {

    private static final Flight FLIGHT = new Flight(1L, "AC123",
            new Route(1L, new Airport("YYC", "Calgary"), new Airport("YVR", "Vancouver")),
            new Airline("AC", "Air Canada"), new Aircraft(1L, "A320", 30, 6), LocalDateTime.of(2026, 12, 1, 8, 0),
            LocalDateTime.of(2026, 12, 1, 9, 30), new BigDecimal("200.00"));

    @TempDir
    Path folder;

    private static Customer customer(String name) {
        return new Customer(1L, "jsmith", "pbkdf2$1$AAAA$BBBB", name, "j@x.ca", null);
    }

    private static Reservation reservation(String number, String name, String seat, String price) {
        return Reservation.restore(5L, number, customer(name), FLIGHT, seat, new BigDecimal(price),
                LocalDateTime.of(2026, 11, 20, 9, 0), ReservationStatus.CONFIRMED);
    }

    private static Payment payment(String amount) {
        PaymentDetails details = new PaymentDetails("Jo Smith", "4111 1111 1111 1111", "12/30");
        Payment p = new Payment(5L, new BigDecimal(amount), PaymentMethodType.CREDIT, details.maskedCard(),
                LocalDateTime.of(2026, 11, 20, 9, 1));
        p.approve();
        return p;
    }

    private String read(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    @Test
    void theFileIsNamedAfterTheConfirmationNumberAndHoldsTheBooking() throws IOException {
        Path file = new ConfirmationWriter(folder).write(reservation("CA12AB34", "Jo Smith", "1A", "300.00"),
                payment("300.00"));
        assertEquals(folder.resolve("CA12AB34.txt"), file);
        String text = read(file);
        assertTrue(text.contains("Confirmation: CA12AB34"), text);
        assertTrue(text.contains("Passenger: Jo Smith"), text);
        assertTrue(text.contains("Flight: AC123 (Air Canada)"), text);
        assertTrue(text.contains("Route: YYC (Calgary) to YVR (Vancouver)"), text);
        assertTrue(text.contains("Departure: 2026-12-01 08:00"), text);
        assertTrue(text.contains("Seat: 1A (BUSINESS)"), text);
        assertTrue(text.contains("Amount paid: 300.00"), text);
        assertTrue(text.contains("Card: **** **** **** 1111"), text);
        assertTrue(text.contains("Payment status: APPROVED"), text);
    }

    @Test
    void theLinesComeInTheDocumentedOrder() throws IOException {
        Path file = new ConfirmationWriter(folder).write(reservation("CA12AB34", "Jo Smith", "1A", "300.00"),
                payment("300.00"));
        List<String> labels = read(file).lines().map(l -> l.substring(0, l.indexOf(':'))).toList();
        assertEquals(List.of("Confirmation", "Passenger", "Flight", "Route", "Departure", "Seat", "Amount paid",
                "Card", "Payment status"), labels);
    }

    @Test
    void anEconomySeatIsLabelledEconomy() throws IOException {
        Path file = new ConfirmationWriter(folder).write(reservation("CA12AB34", "Jo Smith", "12C", "200.00"),
                payment("200.00"));
        assertTrue(read(file).contains("Seat: 12C (ECONOMY)"));
    }

    @Test
    void theFullCardNumberIsNotInTheFile() throws IOException {
        Path file = new ConfirmationWriter(folder).write(reservation("CA12AB34", "Jo Smith", "1A", "300.00"),
                payment("300.00"));
        String text = read(file);
        assertFalse(text.contains("4111111111111111"));
        assertFalse(text.contains("4111 1111 1111 1111"));
        assertFalse(text.matches("(?s).*\\d{16}.*"), text);
    }

    @Test
    void writingAgainReplacesTheFileAndLeavesOneFile() throws IOException {
        ConfirmationWriter writer = new ConfirmationWriter(folder);
        writer.write(reservation("CA12AB34", "Jo Smith", "1A", "300.00"), payment("300.00"));
        Path file = writer.write(reservation("CA12AB34", "Jo Smith", "3C", "200.00"), payment("200.00"));
        String text = read(file);
        assertTrue(text.contains("Seat: 3C (ECONOMY)"), text);
        assertTrue(text.contains("Amount paid: 200.00"), text);
        assertFalse(text.contains("Seat: 1A"), text);
        try (Stream<Path> files = Files.list(folder)) {
            assertEquals(List.of("CA12AB34.txt"), files.map(p -> p.getFileName().toString()).toList());
        }
    }

    @Test
    void theFolderIsCreatedIfItIsMissing() {
        Path nested = folder.resolve("a").resolve("b");
        Path file = new ConfirmationWriter(nested).write(reservation("CA12AB34", "Jo Smith", "1A", "300.00"),
                payment("300.00"));
        assertTrue(Files.exists(file));
        assertEquals(nested, file.getParent());
    }

    @Test
    void accentedNamesSurviveAsUtf8() throws IOException {
        Path file = new ConfirmationWriter(folder).write(reservation("CA12AB34", "José Ñandú", "1A", "300.00"),
                payment("300.00"));
        assertTrue(read(file).contains("Passenger: José Ñandú"));
    }

    @Test
    void aFolderThatIsReallyAFileIsAWriteException() throws IOException {
        Path notAFolder = Files.createFile(folder.resolve("plain-file"));
        ConfirmationWriter writer = new ConfirmationWriter(notAFolder);
        assertThrows(ConfirmationWriteException.class,
                () -> writer.write(reservation("CA12AB34", "Jo Smith", "1A", "300.00"), payment("300.00")));
    }

    @Test
    void aFailureLeavesNoHalfWrittenFile() throws IOException {
        // a folder with the name of the target file makes the final move fail
        Files.createDirectories(folder.resolve("CA12AB34.txt").resolve("inner"));
        ConfirmationWriter writer = new ConfirmationWriter(folder);
        assertThrows(ConfirmationWriteException.class,
                () -> writer.write(reservation("CA12AB34", "Jo Smith", "1A", "300.00"), payment("300.00")));
        assertFalse(Files.exists(folder.resolve("CA12AB34.txt.tmp")));
    }

    @Test
    void aConfirmationNumberThatIsNotAFileNameIsRefused() {
        ConfirmationWriter writer = new ConfirmationWriter(folder);
        for (String bad : new String[] {"../evil", "CA/12", "CA 12", "CA.12"}) {
            assertThrows(ConfirmationWriteException.class,
                    () -> writer.write(reservation(bad, "Jo Smith", "1A", "300.00"), payment("300.00")), bad);
        }
    }

    @Test
    void theInputIsChecked() {
        ConfirmationWriter writer = new ConfirmationWriter(folder);
        Reservation noSeat = Reservation.restore(5L, "CA12AB34", customer("Jo Smith"), FLIGHT, null, null,
                LocalDateTime.of(2026, 11, 20, 9, 0), ReservationStatus.CREATED);
        assertThrows(IllegalArgumentException.class, () -> writer.write(noSeat, payment("300.00")));
        assertThrows(IllegalArgumentException.class, () -> writer.write(null, payment("300.00")));
        assertThrows(IllegalArgumentException.class,
                () -> writer.write(reservation("CA12AB34", "Jo Smith", "1A", "300.00"), null));
        Payment other = Payment.restore(1L, 99L, new BigDecimal("300.00"), PaymentMethodType.CREDIT,
                PaymentStatus.APPROVED, "**** **** **** 1111", LocalDateTime.of(2026, 11, 20, 9, 1), "Approved");
        assertThrows(IllegalArgumentException.class,
                () -> writer.write(reservation("CA12AB34", "Jo Smith", "1A", "300.00"), other));
        assertThrows(IllegalArgumentException.class, () -> new ConfirmationWriter(null));
    }
}
