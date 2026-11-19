package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AlertTest {

    private final Alert alert = new Alert(300);

    @Test
    void itIsHiddenUntilItHasSomethingToSay() {
        assertFalse(alert.isVisible());
        assertEquals("", alert.text());
    }

    @Test
    void showingAMessageMakesItVisibleWithItsKind() {
        alert.show(Alert.Kind.ERROR, "Wrong username or password");
        assertTrue(alert.isVisible());
        assertEquals("Wrong username or password", alert.text());
        assertEquals(Alert.Kind.ERROR, alert.kind());
        alert.show(Alert.Kind.SUCCESS, "Account created");
        assertEquals(Alert.Kind.SUCCESS, alert.kind());
    }

    @Test
    void clearingHidesItAgain() {
        alert.show(Alert.Kind.INFO, "Hello");
        alert.clear();
        assertFalse(alert.isVisible());
        assertEquals("", alert.text());
    }

    @Test
    void aBlankOrMissingMessageClearsIt() {
        alert.show(Alert.Kind.ERROR, "Problem");
        alert.show(Alert.Kind.ERROR, "   ");
        assertFalse(alert.isVisible());
        alert.show(Alert.Kind.ERROR, "Problem");
        alert.show(Alert.Kind.ERROR, null);
        assertFalse(alert.isVisible());
    }

    @Test
    void markupInAMessageIsShownAsTextAndNotInterpreted() {
        alert.show(Alert.Kind.ERROR, "Use <b>bold</b> & more");
        assertEquals("Use <b>bold</b> & more", alert.text());
        assertFalse(((javax.swing.JLabel) alert.getComponent(1)).getText().contains("<b>"));
    }

    @Test
    void everyKindIsReadableOnItsFill() {
        for (Alert.Kind kind : Alert.Kind.values()) {
            alert.show(kind, "x");
            assertTrue(alert.isVisible());
        }
        assertTrue(PaletteTest.contrast(Palette.DANGER_TEXT, Palette.DANGER_TINT) >= 4.5);
        assertTrue(PaletteTest.contrast(Palette.SUCCESS_TEXT, Palette.SUCCESS_TINT) >= 4.5);
        assertTrue(PaletteTest.contrast(Palette.NAVY_900, Palette.SKY_100) >= 4.5);
    }
}
