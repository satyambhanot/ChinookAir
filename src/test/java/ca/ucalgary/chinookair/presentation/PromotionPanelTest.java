package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Promotion;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PromotionPanelTest {

    private static final Promotion PROMOTION =
            new Promotion(1L, "Spring Break Sale", "Book a March flight and save 15% on economy seats.");

    @Test
    void itShowsTheTitleAndTheDescription() {
        PromotionPanel panel = new PromotionPanel(PROMOTION, () -> { });
        assertEquals("Spring Break Sale", panel.titleText());
        assertEquals("Book a March flight and save 15% on economy seats.", panel.descriptionText());
    }

    @Test
    void theOkButtonTellsTheDialogToClose() {
        AtomicInteger closed = new AtomicInteger();
        PromotionPanel panel = new PromotionPanel(PROMOTION, closed::incrementAndGet);
        panel.okButton.doClick(0);
        assertEquals(1, closed.get());
        assertEquals("OK", panel.okButton.getText());
    }

    @Test
    void theOkButtonCanBePressedFromTheKeyboard() {
        PromotionPanel panel = new PromotionPanel(PROMOTION, () -> { });
        assertFalse(panel.descriptionText().isBlank());
        assertTrue(panel.okButton.getMnemonic() != 0);
    }
}
