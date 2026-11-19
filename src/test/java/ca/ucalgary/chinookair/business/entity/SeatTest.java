package ca.ucalgary.chinookair.business.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SeatTest {

    @Test
    void numberIsStoredInUppercase() {
        assertEquals("12A", new Seat("12a", SeatClass.ECONOMY, true).number());
    }

    @Test
    void numberAndClassAreRequired() {
        assertThrows(IllegalArgumentException.class, () -> new Seat(" ", SeatClass.ECONOMY, true));
        assertThrows(IllegalArgumentException.class, () -> new Seat(null, SeatClass.ECONOMY, true));
        assertThrows(IllegalArgumentException.class, () -> new Seat("1A", null, true));
    }

    @Test
    void aSeatKnowsIfItIsAvailable() {
        assertFalse(new Seat("1A", SeatClass.BUSINESS, false).available());
    }
}
