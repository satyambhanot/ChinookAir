package ca.ucalgary.chinookair.business.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PromotionTest {

    @Test
    void titleAndDescriptionMustNotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Promotion(null, " ", "text"));
        assertThrows(IllegalArgumentException.class, () -> new Promotion(null, "Title", " "));
        assertThrows(IllegalArgumentException.class, () -> new Promotion(null, null, "text"));
        assertThrows(IllegalArgumentException.class, () -> new Promotion(null, "Title", null));
    }

    @Test
    void textIsTrimmed() {
        Promotion p = new Promotion(3L, " 10% off ", " Fly in March ");
        assertEquals("10% off", p.title());
        assertEquals("Fly in March", p.description());
        assertEquals(3L, p.id());
    }
}
