package ca.ucalgary.chinookair.business.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AirlineTest {

    @Test
    void codeIsStoredInUppercase() {
        assertEquals("AC", new Airline("ac", "Air Canada").code());
        assertEquals("WS", new Airline(" Ws ", "WestJet").code());
    }

    @Test
    void codeMustBeTwoLetters() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Airline("A", "Air"));
        assertEquals("Airline code must be 2 letters", e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> new Airline("ABC", "Air"));
        assertThrows(IllegalArgumentException.class, () -> new Airline("A1", "Air"));
        assertThrows(IllegalArgumentException.class, () -> new Airline(null, "Air"));
    }

    @Test
    void nameMustNotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Airline("AC", " "));
        assertThrows(IllegalArgumentException.class, () -> new Airline("AC", null));
    }

    @Test
    void equalAirlinesAreEqual() {
        assertEquals(new Airline("ac", "Air Canada"), new Airline("AC", "Air Canada"));
        assertEquals(new Airline("ac", "Air Canada").hashCode(), new Airline("AC", "Air Canada").hashCode());
    }
}
