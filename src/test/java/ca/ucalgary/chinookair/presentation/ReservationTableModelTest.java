package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.ucalgary.chinookair.business.entity.Aircraft;
import ca.ucalgary.chinookair.business.entity.Airline;
import ca.ucalgary.chinookair.business.entity.Airport;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.ReservationStatus;
import ca.ucalgary.chinookair.business.entity.Route;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReservationTableModelTest {

    private ReservationTableModel model;

    private static Reservation reservation(String seat, String price, ReservationStatus status) {
        Route route = new Route(1L, new Airport("YYC", "Calgary"), new Airport("YVR", "Vancouver"));
        Flight flight = new Flight(1L, "AC123", route, new Airline("AC", "Air Canada"),
                new Aircraft(1L, "Boeing 737", 25, 6), LocalDateTime.of(2026, 12, 1, 12, 0),
                LocalDateTime.of(2026, 12, 1, 13, 30), new BigDecimal("200.00"));
        Customer jo = new Customer(1L, "josmith", "pbkdf2$1$AAAA$BBBB", "Jo Smith", "jo@example.com", null);
        return Reservation.restore(1L, "CA7K2P9X", jo, flight, seat, price == null ? null : new BigDecimal(price),
                LocalDateTime.of(2026, 11, 20, 9, 0), status);
    }

    @BeforeEach
    void newModel() {
        model = new ReservationTableModel();
    }

    @Test
    void aConfirmedReservationShowsAllSevenValues() {
        model.setReservations(List.of(reservation("1A", "300.00", ReservationStatus.CONFIRMED)));
        assertEquals(1, model.getRowCount());
        assertEquals(7, model.getColumnCount());
        assertEquals("CA7K2P9X", model.getValueAt(0, 0));
        assertEquals("AC123", model.getValueAt(0, 1));
        assertEquals("YYC → YVR", model.getValueAt(0, 2));
        assertEquals("2026-12-01 12:00", model.getValueAt(0, 3));
        assertEquals("1A", model.getValueAt(0, 4));
        assertEquals("CONFIRMED", model.getValueAt(0, 5));
        assertEquals("$300.00", model.getValueAt(0, 6));
    }

    @Test
    void aReservationWithoutASeatShowsEmptySeatAndPrice() {
        model.setReservations(List.of(reservation(null, null, ReservationStatus.CREATED)));
        assertEquals("", model.getValueAt(0, 4));
        assertEquals("", model.getValueAt(0, 6));
    }

    @Test
    void theColumnNamesAreInOrder() {
        assertEquals("Confirmation", model.getColumnName(0));
        assertEquals("Status", model.getColumnName(5));
        assertEquals("Price", model.getColumnName(6));
    }

    @Test
    void anUnknownColumnIsRejected() {
        model.setReservations(List.of(reservation("1A", "300.00", ReservationStatus.CONFIRMED)));
        assertThrows(IndexOutOfBoundsException.class, () -> model.getValueAt(0, 7));
    }
}
