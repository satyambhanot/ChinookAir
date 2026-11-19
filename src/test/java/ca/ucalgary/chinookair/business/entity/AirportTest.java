package ca.ucalgary.chinookair.business.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AirportTest {

    @Test
    void codeIsStoredInUppercase() {
        assertEquals("YYC", new Airport("yyc", "Calgary").code());
        assertEquals("YVR", new Airport(" yVr ", "Vancouver").code());
    }

    @Test
    void codeMustBeThreeLetters() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new Airport("YY", "Calgary"));
        assertTrue(e.getMessage().contains("3 letters"));
        assertThrows(IllegalArgumentException.class, () -> new Airport("YYCC", "Calgary"));
        assertThrows(IllegalArgumentException.class, () -> new Airport("Y1C", "Calgary"));
        assertThrows(IllegalArgumentException.class, () -> new Airport(null, "Calgary"));
    }

    @Test
    void cityMustNotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Airport("YYC", " "));
        assertThrows(IllegalArgumentException.class, () -> new Airport("YYC", null));
    }

    @Test
    void equalAirportsAreEqual() {
        assertEquals(new Airport("yyc", "Calgary"), new Airport("YYC", "Calgary"));
    }
}
