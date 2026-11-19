package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JLabel;
import javax.swing.JTextField;
import org.junit.jupiter.api.Test;

class FormFieldTest {

    private final JTextField input = new JTextField(10);
    private final FormField field = new FormField("Email", input);

    @Test
    void theLabelAndTheInputBelongTogether() {
        assertEquals("Email", field.labelText());
        assertSame(input, field.field());
        assertEquals("Email", input.getAccessibleContext().getAccessibleName());
        assertFalse(field.hasError());
        assertEquals("", field.lineText());
    }

    @Test
    void theHintShowsUntilThereIsAnError() {
        field.helper("We only use it for confirmations");
        assertEquals("We only use it for confirmations", field.lineText());
        field.showError("That does not look like an email");
        assertEquals("That does not look like an email", field.lineText());
        assertTrue(field.hasError());
        assertEquals(Theme.DANGER, ((JLabel) field.getComponent(2)).getForeground());
        field.clearError();
        assertEquals("We only use it for confirmations", field.lineText());
        assertEquals(Theme.MUTED, ((JLabel) field.getComponent(2)).getForeground());
    }

    @Test
    void anErrorGivesTheInputARedOutlineAndClearingTakesItAway() {
        assertNull(input.getClientProperty("JComponent.outline"));
        field.showError("Required");
        assertEquals("error", input.getClientProperty("JComponent.outline"));
        field.clearError();
        assertNull(input.getClientProperty("JComponent.outline"));
    }

    @Test
    void screenReadersGetTheCurrentLineAsTheDescription() {
        field.helper("Hint");
        assertEquals("Hint", input.getAccessibleContext().getAccessibleDescription());
        field.showError("Problem");
        assertEquals("Problem", input.getAccessibleContext().getAccessibleDescription());
    }

    @Test
    void theMnemonicIsOnTheLabel() {
        field.mnemonic('E');
        assertEquals('E', ((JLabel) field.getComponent(0)).getDisplayedMnemonic());
    }

    @Test
    void aVerticalLayoutDoesNotStretchTheFieldTallerThanItNeeds() {
        assertEquals(field.getPreferredSize().height, field.getMaximumSize().height);
    }

    @Test
    void aNullMessageClearsTheError() {
        field.showError("x");
        field.showError(null);
        assertFalse(field.hasError());
    }

    @Test
    void checkingOnLeaveShowsTheProblemWhenTheCursorLeavesTheBox() {
        javax.swing.JTextField input = new javax.swing.JTextField();
        FormField row = new FormField("Rows", input).checkOnLeave(
                text -> text.equals("0") ? "Rows must be greater than 0" : null);
        input.setText("0");
        leave(input, false);
        assertEquals("Rows must be greater than 0", row.lineText());
        assertTrue(row.hasError());
        input.setText("5");
        leave(input, false);
        assertFalse(row.hasError(), "a fixed box loses its message");
    }

    @Test
    void anEmptyBoxIsNotScoldedForBeingPassedBy() {
        javax.swing.JTextField input = new javax.swing.JTextField();
        FormField row = new FormField("Rows", input).checkOnLeave(text -> "never asked");
        leave(input, false);
        assertFalse(row.hasError());
    }

    @Test
    void aTemporaryLossOfFocusIsIgnored() {
        javax.swing.JTextField input = new javax.swing.JTextField("bad");
        FormField row = new FormField("Rows", input).checkOnLeave(text -> "Bad");
        leave(input, true);
        assertFalse(row.hasError(), "a click on a menu or another window is not leaving the box");
    }

    /** What the keyboard focus does when the cursor leaves a box, told to the box's listeners directly. */
    private static void leave(javax.swing.JTextField input, boolean temporary) {
        java.awt.event.FocusEvent lost = new java.awt.event.FocusEvent(input, java.awt.event.FocusEvent.FOCUS_LOST,
                temporary);
        for (java.awt.event.FocusListener listener : input.getFocusListeners()) {
            listener.focusLost(lost);
        }
    }
}
