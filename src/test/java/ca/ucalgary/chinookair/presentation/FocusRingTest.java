package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.DefaultKeyboardFocusManager;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * A person using the keyboard must see where the focus is. Each custom control is painted with and
 * without the keyboard focus, and the two pictures must differ by a ring that stands out from the
 * surface behind it.
 */
class FocusRingTest {

    private static final double NON_TEXT_CONTRAST = 3.0;

    /** A focus manager a test can tell who has the focus, since no window has it here. */
    private static final class Owner extends DefaultKeyboardFocusManager {

        void give(Component component) {
            setGlobalPermanentFocusOwner(component);
            setGlobalFocusOwner(component);
        }
    }

    private static final Owner OWNER = new Owner();
    private static KeyboardFocusManager previous;

    @BeforeAll
    static void install() {
        Theme.install();
        previous = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        KeyboardFocusManager.setCurrentKeyboardFocusManager(OWNER);
    }

    @AfterAll
    static void restore() {
        KeyboardFocusManager.setCurrentKeyboardFocusManager(previous);
    }

    private static BufferedImage paint(JComponent component, Color behind) {
        Dimension size = component.getPreferredSize();
        // a margin round the component shows what is drawn outside its bounds
        JPanel stage = new JPanel(null);
        stage.setBackground(behind);
        stage.setSize(size.width + 20, size.height + 20);
        component.setSize(size);
        component.setLocation(10, 10);
        stage.add(component);
        layOut(component);
        BufferedImage image = new BufferedImage(stage.getWidth(), stage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        stage.paint(g);
        g.dispose();
        return image;
    }

    private static void layOut(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container) {
                layOut((Container) child);
            }
        }
    }

    /** What changes when the component gets the focus: the number of pixels, and the strongest contrast. */
    private static Result ring(JComponent component, Color behind) {
        OWNER.give(null);
        BufferedImage before = paint(component, behind);
        OWNER.give(component);
        BufferedImage after = paint(component, behind);
        OWNER.give(null);
        int changed = 0;
        double best = 0;
        for (int y = 0; y < before.getHeight(); y++) {
            for (int x = 0; x < before.getWidth(); x++) {
                Color was = new Color(before.getRGB(x, y), true);
                Color now = new Color(after.getRGB(x, y), true);
                if (!was.equals(now)) {
                    changed++;
                    best = Math.max(best, PaletteTest.contrast(now, was));
                }
            }
        }
        return new Result(changed, best);
    }

    private record Result(int changed, double contrast) { }

    private static void assertVisibleRing(String what, JComponent component, Color behind) {
        Result result = ring(component, behind);
        assertTrue(result.changed() >= 20, what + " looks the same with and without the focus ("
                + result.changed() + " pixels differ)");
        assertTrue(result.contrast() >= NON_TEXT_CONTRAST, what + " shows the focus with contrast "
                + String.format("%.2f", result.contrast()));
    }

    // ---- the custom controls

    @Test
    void aDashboardCardShowsTheFocus() {
        TaskCard card = new TaskCard("Find flights", "Search and book", "search");
        assertVisibleRing("dashboard card", card, Theme.BACKGROUND);
    }

    @Test
    void aSeatShowsTheFocus() {
        JToggleButton seat = new JToggleButton("5C");
        seat.putClientProperty("FlatLaf.styleClass", "seat");
        seat.setOpaque(true);
        seat.setBackground(Theme.SURFACE);
        seat.setPreferredSize(new Dimension(44, 36));
        assertVisibleRing("seat", seat, Theme.BACKGROUND);
    }

    @Test
    void aSeatThatIsChosenStillShowsTheFocus() {
        JToggleButton seat = new JToggleButton("5C");
        seat.putClientProperty("FlatLaf.styleClass", "seat");
        seat.setSelected(true);
        seat.setPreferredSize(new Dimension(44, 36));
        assertVisibleRing("chosen seat", seat, Theme.BACKGROUND);
    }

    @Test
    void aNearbyDayShowsTheFocus() {
        DayStrip strip = new DayStrip();
        LocalDate today = LocalDate.of(2026, 12, 1);
        strip.show(today.plusDays(3), today, day -> java.util.Optional.empty());
        for (JToggleButton day : strip.days()) {
            if (day.isEnabled()) {
                assertVisibleRing("nearby day", day, Theme.BACKGROUND);
            }
        }
    }

    @Test
    void aDayOfTheCalendarShowsTheFocus() {
        DatePickerField field = new DatePickerField(10, () -> LocalDate.of(2026, 12, 1));
        field.setDate(LocalDate.of(2026, 12, 15));
        field.openCalendar();
        List<JButton> days = new ArrayList<>();
        collect(field.calendar(), days);
        assertFalse(days.isEmpty());
        for (JButton day : days) {
            if (day.isEnabled()) {
                assertVisibleRing("calendar day " + day.getName(), day, Theme.SURFACE);
            }
        }
    }

    @Test
    void aChipShowsTheFocus() {
        JToggleButton chip = Theme.chip(new JToggleButton("Cheapest"));
        assertVisibleRing("chip", chip, Theme.BACKGROUND);
    }

    @Test
    void everyKindOfButtonShowsTheFocus() {
        assertVisibleRing("primary button", Theme.primary(new JButton("Pay")), Theme.BACKGROUND);
        assertVisibleRing("secondary button", Theme.secondary(new JButton("Back")), Theme.BACKGROUND);
        assertVisibleRing("danger button", Theme.danger(new JButton("Cancel")), Theme.BACKGROUND);
        assertVisibleRing("filled red button", Theme.dangerFilled(new JButton("Cancel")), Theme.SURFACE);
        assertVisibleRing("link button", Theme.link(new JButton("Open folder")), Theme.BACKGROUND);
    }

    private static void collect(Container root, List<JButton> into) {
        for (Component child : root.getComponents()) {
            if (child instanceof JButton && child.getName() != null && child.getName().startsWith("20")) {
                into.add((JButton) child);
            }
            if (child instanceof Container) {
                collect((Container) child, into);
            }
        }
    }

    @Test
    void theHelperSeesNoRingWhenNothingChanges() {
        JPanel plain = new JPanel();
        plain.setPreferredSize(new Dimension(40, 20));
        assertEquals(0, ring(plain, Theme.BACKGROUND).changed());
    }
}
