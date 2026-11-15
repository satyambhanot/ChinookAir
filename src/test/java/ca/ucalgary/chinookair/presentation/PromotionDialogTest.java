package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import ca.ucalgary.chinookair.business.entity.Promotion;
import java.awt.GraphicsEnvironment;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Builds the dialog without showing it; skipped on a machine without a display. */
class PromotionDialogTest {

    private static final Promotion PROMOTION =
            new Promotion(1L, "Spring Break Sale", "Book a March flight and save 15% on economy seats.");

    @BeforeEach
    void needsADisplay() {
        assumeFalse(GraphicsEnvironment.isHeadless());
        Theme.install();
    }

    private static PromotionDialog dialog() {
        return new PromotionDialog(new JFrame(), PROMOTION);
    }

    @Test
    void enterPressesOkAndOkCloses() {
        PromotionDialog dialog = dialog();
        PromotionPanel panel = (PromotionPanel) dialog.getContentPane();
        assertSame(panel.okButton, dialog.getRootPane().getDefaultButton());
        panel.okButton.doClick(0);
        assertFalse(dialog.isDisplayable());
    }

    @Test
    void escapeClosesTheDialogToo() {
        PromotionDialog dialog = dialog();
        javax.swing.Action escape = dialog.getRootPane().getActionMap().get(
                dialog.getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
                        .get(KeyStroke.getKeyStroke("ESCAPE")));
        assertNotNull(escape, "Escape is bound");
        escape.actionPerformed(new java.awt.event.ActionEvent(dialog, 0, "escape"));
        assertFalse(dialog.isDisplayable());
    }

    @Test
    void theDialogBelongsToTheWindowSoItIsModalOverIt() {
        assertNotNull(dialog().getOwner());
    }
}
