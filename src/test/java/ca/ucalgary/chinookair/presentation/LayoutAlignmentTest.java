package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.control.Controllers;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Lays every screen out in a window of the usual size, without showing it, and measures it: the
 * pieces that should line up must line up, to the pixel. These are the checks behind the alignment
 * of every screen.
 */
class LayoutAlignmentTest {

    /** The login and registration screens are cards in the middle of the window, not pages with a heading. */
    private static final java.util.Set<String> BEFORE_LOGIN = java.util.Set.of("login", "register");

    private static final int WIDTH = 1100;
    private static final int HEIGHT = 640;

    @TempDir
    static Path folder;

    private static Controllers c;
    private static EveryScreen EVERY;
    private static final Map<String, JComponent> SCREENS = new LinkedHashMap<>();
    private static PaymentView payment;
    private static ProfileView profile;
    private static ConfirmationView confirmation;

    @BeforeAll
    static void buildEveryScreen() {
        EVERY = EveryScreen.build(folder);
        c = EVERY.controllers;
        SCREENS.putAll(EVERY.screens);
        payment = EVERY.payment;
        profile = EVERY.profile;
        confirmation = EVERY.confirmation;
    }

    @AfterAll
    static void shutDown() {
        EVERY.shutDown();
    }

    // ---- helpers

    private static ContentPane laidOut(JComponent screen, int width, int height) {
        ContentPane pane = new ContentPane(screen);
        pane.setSize(width, height);
        layout(pane);
        return pane;
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container) {
                layout((Container) child);
            }
        }
    }

    private static boolean visible(Component component, Component root) {
        for (Component at = component; at != null && at != root.getParent(); at = at.getParent()) {
            if (!at.isVisible()) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Component> List<T> all(Container root, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (type.isInstance(child) && visible(child, root)) {
                found.add((T) child);
            }
            if (child instanceof Container && child.isVisible()) {
                found.addAll(all((Container) child, type));
            }
        }
        return found;
    }

    private static int left(Component component, Component root) {
        return SwingUtilities.convertPoint(component.getParent(), component.getX(), 0, root).x;
    }

    private static int right(Component component, Component root) {
        return left(component, root) + component.getWidth();
    }

    private static int top(Component component, Component root) {
        return SwingUtilities.convertPoint(component.getParent(), 0, component.getY(), root).y;
    }

    private static int centreY(Component component, Component root) {
        return SwingUtilities.convertPoint(component.getParent(), 0, component.getY(), root).y
                + component.getHeight() / 2;
    }

    // ---- the checks

    @Test
    void everyScreenStartsItsContentAtTheSamePlaceOnThePage() {
        List<String> off = new ArrayList<>();
        for (Map.Entry<String, JComponent> entry : SCREENS.entrySet()) {
            if (BEFORE_LOGIN.contains(entry.getKey())) {
                continue;
            }
            ContentPane pane = laidOut(entry.getValue(), WIDTH, HEIGHT);
            Component first = null;
            if (entry.getKey().equals("confirmation")) {
                first = all(pane, SuccessMark.class).get(0);
            } else {
                for (JLabel label : all(pane, JLabel.class)) {
                    Font font = label.getFont();
                    if (font.isBold() && font.getSize2D() == Type.PAGE_HEADING && !label.getText().isBlank()) {
                        first = label;
                        break;
                    }
                }
            }
            assertTrue(first != null, entry.getKey() + " has no page heading");
            if (left(first, pane) != Space.PAGE) {
                off.add(entry.getKey() + " starts at " + left(first, pane));
            }
        }
        assertTrue(off.isEmpty(), "headings should start at " + Space.PAGE + ": " + off);
    }

    @Test
    void everyLabelStartsWhereTheDrawnEdgeOfItsInputStarts() {
        List<String> off = new ArrayList<>();
        int checked = 0;
        for (Map.Entry<String, JComponent> entry : SCREENS.entrySet()) {
            ContentPane pane = laidOut(entry.getValue(), WIDTH, HEIGHT);
            for (JLabel label : all(pane, JLabel.class)) {
                Component input = label.getLabelFor();
                if (input == null || !visible(input, pane)) {
                    continue;
                }
                int text = left(label, pane) + label.getInsets().left;
                int edge = left(input, pane) + Space.fieldEdge();
                checked++;
                if (Math.abs(text - edge) > 1) {
                    off.add(entry.getKey() + ": \"" + label.getText() + "\" at " + text + ", input edge at " + edge);
                }
            }
        }
        assertTrue(checked > 20, "only " + checked + " labels were found");
        assertTrue(off.isEmpty(), off.toString());
    }

    @Test
    void theInputsAndButtonsOfAFilterBarShareOneCentreLine() {
        List<String> off = new ArrayList<>();
        int bars = 0;
        for (Map.Entry<String, JComponent> entry : SCREENS.entrySet()) {
            ContentPane pane = laidOut(entry.getValue(), WIDTH, HEIGHT);
            for (FilterBar bar : all(pane, FilterBar.class)) {
                bars++;
                int line = centreY(bar.controls().get(0), pane);
                for (JComponent control : bar.controls()) {
                    if (Math.abs(centreY(control, pane) - line) > 1) {
                        off.add(entry.getKey() + ": " + control.getClass().getSimpleName() + " at "
                                + centreY(control, pane) + " instead of " + line);
                    }
                }
            }
        }
        assertTrue(bars >= 3, "found " + bars + " filter bars");
        assertTrue(off.isEmpty(), off.toString());
    }

    @Test
    void aFilterBarStartsAtItsLeftEdgeInsteadOfCentringItsControls() {
        int bars = 0;
        for (Map.Entry<String, JComponent> entry : SCREENS.entrySet()) {
            ContentPane pane = laidOut(entry.getValue(), WIDTH, HEIGHT);
            for (FilterBar bar : all(pane, FilterBar.class)) {
                bars++;
                assertEquals(left(bar, pane), left(bar.controls().get(0), pane), entry.getKey());
            }
        }
        assertTrue(bars >= 3);
    }

    @Test
    void tablesStartAtTheSameEdgeAsTheHeadingAndAreDrawnToTheirBounds() {
        for (String name : new String[] {"customers", "flights", "routes",
                "aircraft", "schedule", "payment records"}) {
            ContentPane pane = laidOut(SCREENS.get(name), WIDTH, HEIGHT);
            javax.swing.JScrollPane first = all(pane, javax.swing.JScrollPane.class).get(0);
            assertEquals(Space.PAGE, left(first, pane), name);
            assertEquals(1, first.getBorder().getBorderInsets(first).left, name + ": the line is at the edge");
        }
    }

    @Test
    void theTimesOfAFlightAreInViewInTheSmallestWindow() {
        FlightManagementView view = (FlightManagementView) SCREENS.get("flights");
        view.newButton.doClick(0);
        laidOut(view, 960, 640);
        javax.swing.JScrollPane form = null;
        for (JComponent c : all(view, JComponent.class)) {
            if (c.getBorder() instanceof SurfaceBorder) {
                form = all(c, javax.swing.JScrollPane.class).get(0);
                break;
            }
        }
        assertTrue(form != null, "the card shows");
        JComponent content = (JComponent) form.getViewport().getView();
        for (Component field : new Component[] {view.departureField, view.arrivalField}) {
            int bottom = SwingUtilities.convertPoint(field.getParent(), 0, field.getY() + field.getHeight(),
                    content).y;
            assertTrue(bottom <= form.getViewport().getHeight(),
                    "the field ends at " + bottom + " but the card shows " + form.getViewport().getHeight());
        }
    }

    @Test
    void thePaymentsSitBesideTheCustomerListNotUnderIt() {
        ContentPane pane = laidOut(SCREENS.get("payment records"), WIDTH, HEIGHT);
        List<javax.swing.JScrollPane> tables = all(pane, javax.swing.JScrollPane.class);
        assertEquals(Space.PAGE, left(tables.get(0), pane));
        assertTrue(right(tables.get(0), pane) <= Space.PAGE + 420, "the list is " + tables.get(0).getWidth());
        assertTrue(left(tables.get(1), pane) > right(tables.get(0), pane), "the payments are on the right");
        assertEquals(top(tables.get(0), pane), top(tables.get(1), pane), "both lists start at the same height");
    }

    @Test
    void theReservationCardsStartAtThePageEdgeAndFillItsWidth() {
        ContentPane pane = laidOut(SCREENS.get("my reservations"), WIDTH, HEIGHT);
        List<ReservationCard> cards = all(pane, ReservationCard.class);
        assertTrue(cards.size() >= 1, "the customer has a booking");
        assertEquals(Space.PAGE, left(cards.get(0), pane));
        assertEquals(WIDTH - Space.PAGE, right(cards.get(0), pane));
    }

    @Test
    void theFlightCardsStartAtThePageEdgeAndFillItsWidth() {
        FlightSearchView view = (FlightSearchView) SCREENS.get("find flights");
        view.onShow();
        ContentPane pane = laidOut(view, WIDTH, HEIGHT);
        assertTrue(view.results.offers().size() > 0, "the starting route has flights");
        assertEquals(Space.PAGE, left(view.results, pane));
        assertEquals(WIDTH - Space.PAGE, right(view.results, pane));
    }

    @Test
    void aTableAndTheCardBesideItStartAtTheSameHeight() {
        for (String name : new String[] {"routes", "aircraft", "flights", "choose seat"}) {
            ContentPane pane = laidOut(SCREENS.get(name), WIDTH, HEIGHT);
            if (name.equals("flights")) {
                // the card of this screen shows only when a flight is chosen
                ((FlightManagementView) SCREENS.get(name)).newButton.doClick(0);
                pane = laidOut(SCREENS.get(name), WIDTH, HEIGHT);
            }
            javax.swing.JScrollPane table = all(pane, javax.swing.JScrollPane.class).get(0);
            JComponent card = null;
            for (JComponent c : all(pane, JComponent.class)) {
                if (c.getBorder() instanceof SurfaceBorder) {
                    card = c;
                    break;
                }
            }
            assertTrue(card != null, name + " has no card");
            int top = SwingUtilities.convertPoint(table.getParent(), table.getX(), table.getY(), pane).y;
            int cardTop = SwingUtilities.convertPoint(card.getParent(), card.getX(), card.getY(), pane).y;
            assertEquals(top, cardTop, name);
        }
    }

    @Test
    void aButtonRowEndsAtTheEdgeOfTheFieldsAboveIt() {
        ContentPane pane = laidOut(SCREENS.get("payment"), WIDTH, HEIGHT);
        assertEquals(right(payment.nameField, pane), right(payment.payButton, pane),
                "Pay ends where the fields end");
        pane = laidOut(SCREENS.get("profile"), WIDTH, HEIGHT);
        List<javax.swing.JTextField> fields = all(pane, javax.swing.JTextField.class);
        assertEquals(right(fields.get(0), pane), right(profile.saveButton, pane),
                "Save ends where the fields end");
    }

    @Test
    void theConfirmationButtonsStartAtTheEdgeOfTheCardsAbove() {
        ContentPane pane = laidOut(SCREENS.get("confirmation"), WIDTH, HEIGHT);
        assertEquals(Space.PAGE, left(confirmation.dashboardButton, pane));
    }

    @Test
    void aWideWindowKeepsTheScreenAtItsReadableWidthAndCentred() {
        for (String name : new String[] {"find flights", "flights", "customers"}) {
            ContentPane pane = laidOut(SCREENS.get(name), 1800, 900);
            JComponent screen = SCREENS.get(name);
            assertEquals(ContentPane.MAX_WIDTH, screen.getWidth(), name);
            assertEquals((1800 - ContentPane.MAX_WIDTH) / 2, screen.getX(), name);
            assertEquals(pane.getHeight(), screen.getHeight(), name);
        }
    }

    @Test
    void aSmallWindowGivesTheScreenAllOfItsWidth() {
        ContentPane pane = laidOut(SCREENS.get("find flights"), 960, 640);
        assertEquals(960, SCREENS.get("find flights").getWidth());
        assertEquals(0, SCREENS.get("find flights").getX());
        assertEquals(640, pane.getHeight());
    }

    // ---- the smallest window: a laptop screen of 1366 x 768 pixels drawn at 150 %

    /** 910 pixels of window, less the scroll bar that a low window shows and the edge of the window. */
    private static final int SMALL_WIDTH = 866;
    private static final int SMALL_HEIGHT = 400;

    /** The screens that keep their shape: the others are checked when they hold something to show. */
    @Test
    void noControlSticksOutOrLosesItsWordsInTheSmallestWindow() {
        List<String> outside = new ArrayList<>();
        for (Map.Entry<String, JComponent> entry : SCREENS.entrySet()) {
            if (BEFORE_LOGIN.contains(entry.getKey())) {
                continue;
            }
            ContentPane pane = laidOut(entry.getValue(), SMALL_WIDTH, SMALL_HEIGHT);
            for (Component control : all(pane, Component.class)) {
                if (!(control instanceof javax.swing.AbstractButton || control instanceof javax.swing.JComboBox
                        || control instanceof javax.swing.text.JTextComponent)) {
                    continue;
                }
                if (inScrollPane(control, pane)) {
                    continue;
                }
                int edge = right(control, pane);
                if (edge > SMALL_WIDTH || left(control, pane) < 0) {
                    outside.add(entry.getKey() + ": " + control.getClass().getSimpleName() + " ends at " + edge);
                }
                if (control instanceof javax.swing.JComboBox && control.getWidth() < 120) {
                    outside.add(entry.getKey() + ": a list box is only " + control.getWidth() + " wide");
                }
                if (control instanceof javax.swing.AbstractButton
                        && !((javax.swing.AbstractButton) control).getText().isEmpty()
                        && control.getWidth() < control.getPreferredSize().width) {
                    outside.add(entry.getKey() + ": the button '" + ((javax.swing.AbstractButton) control).getText()
                            + "' is " + control.getWidth() + " wide and wants " + control.getPreferredSize().width);
                }
            }
        }
        assertTrue(outside.isEmpty(), "sticking out: " + outside);
    }

    private static boolean inScrollPane(Component control, Component root) {
        for (Component at = control.getParent(); at != null && at != root; at = at.getParent()) {
            if (at instanceof javax.swing.JScrollPane || at instanceof javax.swing.JViewport) {
                return true;
            }
        }
        return false;
    }

    @Test
    void theFlightResultsKeepAWholeCardInTheLowestWindow() {
        FlightSearchView view = (FlightSearchView) SCREENS.get("find flights");
        // what is left of a 485 pixel window under the top bar and the step strip
        javax.swing.JScrollPane page = ContentPane.scrolling(new ContentPane(view));
        page.setSize(SMALL_WIDTH, 354);
        layout(page);
        ResultsArea area = all((Container) page.getViewport().getView(), ResultsArea.class).get(0);
        assertTrue(area.scrollPane().getHeight() >= FlightResultList.CELL_HEIGHT,
                "the list is " + area.scrollPane().getHeight() + " px high");
    }

    @Test
    void aScreenThatFitsFillsTheWindowAndALowWindowScrollsThePageInstead() {
        JComponent plain = new javax.swing.JPanel();
        javax.swing.JScrollPane roomy = ContentPane.scrolling(new ContentPane(plain));
        roomy.setSize(900, 700);
        layout(roomy);
        assertEquals(700, roomy.getViewport().getView().getHeight());
        assertFalse(roomy.getVerticalScrollBar().isVisible());

        FlightSearchView view = (FlightSearchView) SCREENS.get("find flights");
        javax.swing.JScrollPane low = ContentPane.scrolling(new ContentPane(view));
        low.setSize(900, 200);
        layout(low);
        assertTrue(low.getViewport().getView().getHeight() > 200, "the page is taller than the window");
        assertTrue(low.getVerticalScrollBar().isVisible(), "so it scrolls");
    }
}
