package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.UIManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ThemeTest {

    @BeforeAll
    static void install() {
        Theme.install();
    }

    @Test
    void theFontIsInterAtBodySize() {
        assertEquals("Inter", UIManager.getFont("defaultFont").getFamily());
        assertEquals(Type.BODY, UIManager.getFont("defaultFont").getSize2D());
    }

    @Test
    void theThemeFileIsLoaded() {
        assertEquals(40, UIManager.getInt("Table.rowHeight"));
        assertEquals(Palette.BLUE_600, UIManager.getColor("Component.focusColor"));
        assertEquals(Palette.PAGE, UIManager.getColor("Panel.background"));
    }

    @Test
    void everyButtonStyleHasADefinition() {
        for (String name : new String[] {"primary", "danger", "secondary", "link", "card", "day", "today"}) {
            assertNotNull(UIManager.get("[style]Button." + name), name);
        }
        assertNotNull(UIManager.get("[style]ToggleButton.seat"));
    }

    @Test
    void theButtonHelpersNameTheirStyle() {
        assertEquals("primary", Theme.primary(new JButton()).getClientProperty("FlatLaf.styleClass"));
        assertEquals("danger", Theme.danger(new JButton()).getClientProperty("FlatLaf.styleClass"));
        assertEquals("secondary", Theme.secondary(new JButton()).getClientProperty("FlatLaf.styleClass"));
        assertEquals("link", Theme.link(new JButton()).getClientProperty("FlatLaf.styleClass"));
    }

    @Test
    void headingsUseTheTypeScale() {
        JLabel title = Theme.title("Find flights");
        assertEquals(Type.PAGE_HEADING, title.getFont().getSize2D());
        assertTrue(title.getFont().isBold());
        assertEquals(Type.LABEL, Theme.hint("hint").getFont().getSize2D());
    }
}
