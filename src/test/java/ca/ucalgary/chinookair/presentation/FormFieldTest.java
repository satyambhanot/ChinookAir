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
    void aNullMessageClearsTheError() {
        field.showError("x");
        field.showError(null);
        assertFalse(field.hasError());
    }
}
