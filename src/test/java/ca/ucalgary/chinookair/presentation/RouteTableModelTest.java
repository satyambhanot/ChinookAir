package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.ucalgary.chinookair.business.entity.Airport;
import ca.ucalgary.chinookair.business.entity.Route;
import java.util.List;
import org.junit.jupiter.api.Test;

class RouteTableModelTest {

    private static Route route() {
        return new Route(1L, new Airport("YEG", "Edmonton"), new Airport("YOW", "Ottawa"));
    }

    @Test
    void aRouteShowsOriginAndDestinationWithTheirCities() {
        RouteTableModel model = new RouteTableModel();
        model.setRoutes(List.of(route()));
        assertEquals(1, model.getRowCount());
        assertEquals(2, model.getColumnCount());
        assertEquals("YEG - Edmonton", model.getValueAt(0, 0));
        assertEquals("YOW - Ottawa", model.getValueAt(0, 1));
        assertEquals("Origin", model.getColumnName(0));
        assertEquals("Destination", model.getColumnName(1));
    }

    @Test
    void anUnknownColumnIsRejected() {
        RouteTableModel model = new RouteTableModel();
        model.setRoutes(List.of(route()));
        assertThrows(IndexOutOfBoundsException.class, () -> model.getValueAt(0, 2));
    }
}
