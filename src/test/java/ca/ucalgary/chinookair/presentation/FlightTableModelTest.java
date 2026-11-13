package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.ucalgary.chinookair.business.control.FlightOffer;
import ca.ucalgary.chinookair.business.entity.Aircraft;
import ca.ucalgary.chinookair.business.entity.Airline;
import ca.ucalgary.chinookair.business.entity.Airport;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Route;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FlightTableModelTest {

    private FlightTableModel model;

    private static FlightOffer offer(String number, int seatsLeft, String price) {
        Route route = new Route(1L, new Airport("YYC", "Calgary"), new Airport("YVR", "Vancouver"));
        Flight flight = new Flight(1L, number, route, new Airline("AC", "Air Canada"),
                new Aircraft(1L, "Boeing 737", 25, 6), LocalDateTime.of(2026, 12, 1, 12, 0),
                LocalDateTime.of(2026, 12, 1, 13, 30), new BigDecimal(price));
        return new FlightOffer(flight, seatsLeft, new BigDecimal(price));
    }

    @BeforeEach
    void newModel() {
        model = new FlightTableModel();
    }

    @Test
    void anEmptyModelHasNoRowsButHasTheColumns() {
        assertEquals(0, model.getRowCount());
        assertEquals(7, model.getColumnCount());
        assertEquals("Flight", model.getColumnName(0));
        assertEquals("From", model.getColumnName(6));
    }

    @Test
    void aRowShowsTheFlightInWords() {
        model.setOffers(List.of(offer("AC123", 150, "200.00")));
        assertEquals("AC123", model.getValueAt(0, 0));
        assertEquals("Air Canada", model.getValueAt(0, 1));
        assertEquals("YYC → YVR", model.getValueAt(0, 2));
        assertEquals("2026-12-01 12:00", model.getValueAt(0, 3));
        assertEquals("2026-12-01 13:30", model.getValueAt(0, 4));
        assertEquals(150, model.getValueAt(0, 5));
        assertEquals("$200.00", model.getValueAt(0, 6));
    }

    @Test
    void thePriceKeepsItsTwoDecimals() {
        model.setOffers(List.of(offer("AC123", 1, "199.90")));
        assertEquals("$199.90", model.getValueAt(0, 6));
    }

    @Test
    void theRowCountIsTheNumberOfOffersAndSettingNewOnesReplacesTheOld() {
        model.setOffers(List.of(offer("AC1", 1, "100.00"), offer("AC2", 2, "110.00")));
        assertEquals(2, model.getRowCount());
        model.setOffers(List.of(offer("AC3", 3, "120.00")));
        assertEquals(1, model.getRowCount());
        assertEquals("AC3", model.offerAt(0).flight().flightNumber());
    }

    @Test
    void anUnknownColumnIsRejected() {
        model.setOffers(List.of(offer("AC123", 1, "100.00")));
        assertThrows(IndexOutOfBoundsException.class, () -> model.getValueAt(0, 7));
    }
}
