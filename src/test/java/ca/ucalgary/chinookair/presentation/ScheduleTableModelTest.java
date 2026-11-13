package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.ucalgary.chinookair.business.entity.Aircraft;
import ca.ucalgary.chinookair.business.entity.Airline;
import ca.ucalgary.chinookair.business.entity.Airport;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Route;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScheduleTableModelTest {

    private static Flight flight() {
        Route route = new Route(1L, new Airport("YYC", "Calgary"), new Airport("YVR", "Vancouver"));
        return new Flight(1L, "AC123", route, new Airline("AC", "Air Canada"), new Aircraft(1L, "Boeing 737", 25, 6),
                LocalDateTime.of(2026, 12, 1, 12, 0), LocalDateTime.of(2026, 12, 1, 13, 30), new BigDecimal("200.00"));
    }

    @Test
    void aFlightShowsAllFiveValues() {
        ScheduleTableModel model = new ScheduleTableModel();
        model.setFlights(List.of(flight()));
        assertEquals(1, model.getRowCount());
        assertEquals(5, model.getColumnCount());
        assertEquals("AC123", model.getValueAt(0, 0));
        assertEquals("YYC → YVR", model.getValueAt(0, 1));
        assertEquals("2026-12-01 12:00", model.getValueAt(0, 2));
        assertEquals("2026-12-01 13:30", model.getValueAt(0, 3));
        assertEquals("Boeing 737", model.getValueAt(0, 4));
        assertEquals("Arrives", model.getColumnName(3));
        assertEquals("AC123", model.flightAt(0).flightNumber());
    }

    @Test
    void anUnknownColumnIsRejected() {
        ScheduleTableModel model = new ScheduleTableModel();
        model.setFlights(List.of(flight()));
        assertThrows(IndexOutOfBoundsException.class, () -> model.getValueAt(0, 5));
    }
}
