package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.accessibility.AccessibleContext;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollBar;
import javax.swing.JTable;
import javax.swing.text.JTextComponent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * What a screen reader and the keyboard need from every screen: each control that takes the focus says
 * what it is, and each button can be reached with the Tab key.
 */
class AccessibilityTest {

    @TempDir
    static Path folder;

    private static EveryScreen every;

    @BeforeAll
    static void buildEveryScreen() {
        every = EveryScreen.build(folder);
    }

    @AfterAll
    static void shutDown() {
        every.shutDown();
    }

    private static boolean shown(Component component, Component root) {
        for (Component at = component; at != null && at != root.getParent(); at = at.getParent()) {
            if (!at.isVisible()) {
                return false;
            }
        }
        return true;
    }

    private static void controls(Container root, Container from, List<JComponent> into) {
        for (Component child : from.getComponents()) {
            if (!shown(child, root)) {
                continue;
            }
            if (child instanceof AbstractButton || child instanceof JTextComponent || child instanceof JComboBox
                    || child instanceof JList || child instanceof JTable) {
                into.add((JComponent) child);
            }
            if (child instanceof Container) {
                controls(root, (Container) child, into);
            }
        }
    }

    private static String nameOf(JComponent control) {
        AccessibleContext context = control.getAccessibleContext();
        String name = context == null ? null : context.getAccessibleName();
        if (name == null || name.isBlank()) {
            name = control instanceof AbstractButton ? ((AbstractButton) control).getText() : null;
        }
        return name;
    }

    /** The arrows of a scroll bar and of a combo box are reached with the arrow keys, not the Tab key. */
    private static boolean insideComboBox(Component component) {
        for (Component at = component.getParent(); at != null; at = at.getParent()) {
            if (at instanceof JComboBox || at instanceof JScrollBar) {
                return true;
            }
        }
        return false;
    }

    @Test
    void everyControlThatTakesTheFocusSaysWhatItIs() {
        List<String> nameless = new ArrayList<>();
        for (Map.Entry<String, JComponent> screen : every.screens.entrySet()) {
            List<JComponent> found = new ArrayList<>();
            controls(screen.getValue(), screen.getValue(), found);
            for (JComponent control : found) {
                if (!control.isFocusable() || insideComboBox(control)) {
                    continue;
                }
                String name = nameOf(control);
                if (name == null || name.isBlank()) {
                    nameless.add(screen.getKey() + ": " + control.getClass().getSimpleName() + " "
                            + control.getName());
                }
            }
        }
        assertTrue(nameless.isEmpty(), "controls without a name: " + nameless);
    }

    @Test
    void everyEnabledButtonCanBeReachedWithTheKeyboard() {
        List<String> skipped = new ArrayList<>();
        for (Map.Entry<String, JComponent> screen : every.screens.entrySet()) {
            List<JComponent> found = new ArrayList<>();
            controls(screen.getValue(), screen.getValue(), found);
            for (JComponent control : found) {
                boolean calendarOpener = "Choose a date".equals(nameOf(control));
                if (control instanceof AbstractButton && control.isEnabled() && !control.isFocusable()
                        && !insideComboBox(control) && !calendarOpener) {
                    skipped.add(screen.getKey() + ": " + nameOf(control));
                }
            }
        }
        assertTrue(skipped.isEmpty(), "buttons the Tab key skips: " + skipped);
    }

    @Test
    void theCalendarOpensFromTheKeyboardAndTheButtonSaysHow() {
        DatePickerField field = new DatePickerField(10, () -> java.time.LocalDate.of(2026, 12, 1));
        javax.swing.KeyStroke altDown = javax.swing.KeyStroke.getKeyStroke("alt DOWN");
        Object action = field.getInputMap(JComponent.WHEN_FOCUSED).get(altDown);
        assertTrue("openCalendar".equals(action), "Alt + Down should open the calendar");
        JComponent opener = (JComponent) field.getClientProperty("JTextField.trailingComponent");
        assertTrue(opener.getToolTipText().contains("Alt"), "the calendar button names the keys that open it");
    }

    @Test
    void aDashboardCardReadsOutItsNameAndWhatItIsFor() {
        TaskCard card = new TaskCard("Find flights", "Search and book a trip", "search");
        assertTrue("Find flights".equals(card.getAccessibleContext().getAccessibleName()));
        assertTrue("Search and book a trip".equals(card.getAccessibleContext().getAccessibleDescription()));
    }
}
