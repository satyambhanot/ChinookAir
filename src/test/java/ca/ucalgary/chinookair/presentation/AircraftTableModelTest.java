package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.ucalgary.chinookair.business.entity.Aircraft;
import java.util.List;
import org.junit.jupiter.api.Test;

class AircraftTableModelTest {

    @Test
    void anAircraftShowsItsLayoutAndTheComputedSeatCount() {
        AircraftTableModel model = new AircraftTableModel();
        model.setAircraft(List.of(new Aircraft(1L, "E190", 25, 4)));
        assertEquals(1, model.getRowCount());
        assertEquals(4, model.getColumnCount());
        assertEquals("E190", model.getValueAt(0, 0));
        assertEquals(25, model.getValueAt(0, 1));
        assertEquals(4, model.getValueAt(0, 2));
        assertEquals(100, model.getValueAt(0, 3));
        assertEquals("Seats", model.getColumnName(3));
    }

    @Test
    void anUnknownColumnIsRejected() {
        AircraftTableModel model = new AircraftTableModel();
        model.setAircraft(List.of(new Aircraft(1L, "E190", 25, 4)));
        assertThrows(IndexOutOfBoundsException.class, () -> model.getValueAt(0, 4));
    }
}
