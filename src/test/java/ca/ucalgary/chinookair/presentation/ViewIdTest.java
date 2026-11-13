package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ViewIdTest {

    @Test
    void theTitleIsTheNameInWords() {
        assertEquals("Login", ViewId.LOGIN.title());
        assertEquals("Flight search", ViewId.FLIGHT_SEARCH.title());
        assertEquals("My reservations", ViewId.MY_RESERVATIONS.title());
        assertEquals("Aircraft management", ViewId.AIRCRAFT_MANAGEMENT.title());
    }

    @Test
    void everyScreenHasATitle() {
        for (ViewId view : ViewId.values()) {
            assertEquals(false, view.title().isBlank());
        }
    }
}
