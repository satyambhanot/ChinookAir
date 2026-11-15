package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import javax.swing.JPanel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Builds the dialog without showing it; skipped on a machine without a display. */
class ConfirmDialogTest {

    @BeforeEach
    void needsADisplay() {
        assumeFalse(GraphicsEnvironment.isHeadless());
    }

    private static ConfirmDialog dialog(boolean danger) {
        return new ConfirmDialog(new JPanel(), "Remove this flight?", "It will be gone.", "Remove flight", danger);
    }

    @Test
    void theConfirmButtonSaysWhatItDoes() {
        assertEquals("Remove flight", dialog(true).confirmButton().getText());
        assertEquals("Cancel", dialog(true).cancelButton().getText());
    }

    @Test
    void aDangerousActionHasARedButtonAndEnterPressesCancel() {
        ConfirmDialog dialog = dialog(true);
        assertEquals("dangerFilled", dialog.confirmButton().getClientProperty("FlatLaf.styleClass"));
        assertSame(dialog.cancelButton(), dialog.getRootPane().getDefaultButton());
    }

    @Test
    void anOrdinaryQuestionHasABlueButtonAndEnterPressesIt() {
        ConfirmDialog dialog = dialog(false);
        assertEquals("primary", dialog.confirmButton().getClientProperty("FlatLaf.styleClass"));
        assertSame(dialog.confirmButton(), dialog.getRootPane().getDefaultButton());
    }

    @Test
    void theAnswerIsNoUntilTheConfirmButtonIsPressed() {
        ConfirmDialog dialog = dialog(true);
        assertFalse(dialog.confirmed());
        dialog.cancelButton().doClick(0);
        assertFalse(dialog.confirmed());
        assertFalse(dialog.isDisplayable(), "the dialog closes");
    }

    @Test
    void pressingTheConfirmButtonAnswersYes() {
        ConfirmDialog dialog = dialog(true);
        dialog.confirmButton().doClick(0);
        assertTrue(dialog.confirmed());
    }

    @Test
    void escapeMeansNo() {
        ConfirmDialog dialog = dialog(true);
        dialog.confirmButton().doClick(0);
        assertTrue(dialog.confirmed());
        ConfirmDialog second = dialog(true);
        second.getRootPane().getActionForKeyStroke(javax.swing.KeyStroke.getKeyStroke("ESCAPE"))
                .actionPerformed(null);
        assertFalse(second.confirmed());
        assertFalse(second.isDisplayable());
    }

    @Test
    void markupInTheMessageIsNotInterpreted() {
        ConfirmDialog dialog = new ConfirmDialog(new JPanel(), "Title", "Use <b>bold</b>", "OK", false);
        assertTrue(dialog.getContentPane().getComponentCount() > 0);
    }
}
