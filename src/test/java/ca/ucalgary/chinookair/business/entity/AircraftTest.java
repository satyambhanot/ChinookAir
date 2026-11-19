package ca.ucalgary.chinookair.business.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AircraftTest {

    @Test
    void seatCountIsRowsTimesSeatsPerRow() {
        assertEquals(180, new Aircraft(null, "A320", 30, 6).seatCount());
        assertEquals(1, new Aircraft(null, "Tiny", 1, 1).seatCount());
        assertEquals(360, new Aircraft(null, "Big", 60, 6).seatCount());
    }

    @Test
    void rowsMustBeBetweenOneAndSixty() {
        assertThrows(IllegalArgumentException.class, () -> new Aircraft(null, "A320", 0, 6));
        assertThrows(IllegalArgumentException.class, () -> new Aircraft(null, "A320", 61, 6));
        assertThrows(IllegalArgumentException.class, () -> new Aircraft(null, "A320", -1, 6));
    }

    @Test
    void seatsPerRowMustBeBetweenOneAndSix() {
        assertThrows(IllegalArgumentException.class, () -> new Aircraft(null, "A320", 30, 7));
        assertThrows(IllegalArgumentException.class, () -> new Aircraft(null, "A320", 30, 0));
    }

    @Test
    void modelMustNotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Aircraft(null, " ", 30, 6));
        assertThrows(IllegalArgumentException.class, () -> new Aircraft(null, null, 30, 6));
    }

    @Test
    void modelIsTrimmed() {
        assertEquals("A320", new Aircraft(null, " A320 ", 30, 6).model());
    }
}
