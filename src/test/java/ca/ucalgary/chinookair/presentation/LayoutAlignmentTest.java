package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.AppContext;
import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.control.FlightOffer;
import ca.ucalgary.chinookair.business.control.SearchCriteria;
import ca.ucalgary.chinookair.data.DatabaseSeeder;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
 * list in docs/ui-refinement-plan.md.
 */
class LayoutAlignmentTest {

    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    private static final int WIDTH = 1100;
    private static final int HEIGHT = 640;

    @TempDir
    static Path folder;

    private static Controllers c;
    private static final RecordingNavigator NAV = new RecordingNavigator();
    private static final Map<String, JComponent> SCREENS = new LinkedHashMap<>();
    private static PaymentView payment;
    private static ProfileView profile;
    private static ConfirmationView confirmation;

    @BeforeAll
    static void buildEveryScreen() {
        Theme.install();
        c = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
        BookingSession booking = new BookingSession();

        c.session().login(c.loginController()
                .authenticate(DatabaseSeeder.CUSTOMER_USERNAME, DatabaseSeeder.CUSTOMER_PASSWORD));
        SCREENS.put("customer home", shown(new CustomerDashboard(c, NAV)));
        FlightSearchView search = new FlightSearchView(c, NAV, booking);
        SCREENS.put("find flights", search);
        FlightOffer offer = c.flightSearchController()
                .search(new SearchCriteria("YYC", "YVR", LocalDate.of(2026, 12, 2), null)).get(0);
        booking.select(offer);
        SeatSelectionView seats = new SeatSelectionView(c, NAV, booking);
        seats.onShow();
        SCREENS.put("choose seat", seats);
        seats.seatButtonFor("3C").doClick(0);
        seats.continueButton.doClick(0);
        payment = new PaymentView(c, NAV, booking);
        payment.onShow();
        SCREENS.put("payment", payment);
        payment.nameField.setText("Casey Customer");
        payment.cardField.setText("4111 1111 1111 1111");
        payment.expiryField.setText("12/30");
        payment.payButton.doClick(0);
        confirmation = new ConfirmationView(c, NAV, booking);
        confirmation.onShow();
        SCREENS.put("confirmation", confirmation);
        SCREENS.put("my reservations", new MyReservationsView(c, NAV, booking));
        profile = new ProfileView(c, NAV);
        SCREENS.put("profile", profile);

        c.session().login(c.loginController()
                .authenticate(DatabaseSeeder.AGENT_USERNAME, DatabaseSeeder.AGENT_PASSWORD));
        SCREENS.put("agent home", shown(new AgentDashboard(c, NAV)));
        SCREENS.put("customers", new CustomerManagementView(c, NAV, booking));
        SCREENS.put("payment records", new PaymentRecordsView(c, NAV));
        SCREENS.put("schedule", new ScheduleView(c, NAV));

        c.session().login(c.loginController()
                .authenticate(DatabaseSeeder.ADMIN_USERNAME, DatabaseSeeder.ADMIN_PASSWORD));
        SCREENS.put("admin home", shown(new AdminDashboard(c, NAV)));
        SCREENS.put("flights", new FlightManagementView(c, NAV));
        SCREENS.put("routes", new RouteManagementView(c, NAV));
        SCREENS.put("aircraft", new AircraftManagementView(c, NAV));
    }

    /** Opens a screen the way the window does, while the right person is logged in. */
    private static <T extends JComponent & Screen> T shown(T screen) {
        screen.onShow();
        return screen;
    }

    @AfterAll
    static void shutDown() {
        c.shutdown().run();
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

    private static int centreY(Component component, Component root) {
        return SwingUtilities.convertPoint(component.getParent(), 0, component.getY(), root).y
                + component.getHeight() / 2;
    }

    // ---- the checks

    @Test
    void everyScreenStartsItsContentAtTheSamePlaceOnThePage() {
        List<String> off = new ArrayList<>();
        for (Map.Entry<String, JComponent> entry : SCREENS.entrySet()) {
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
        for (String name : new String[] {"find flights", "my reservations", "customers", "flights", "routes",
                "aircraft", "schedule", "payment records"}) {
            ContentPane pane = laidOut(SCREENS.get(name), WIDTH, HEIGHT);
            javax.swing.JScrollPane first = all(pane, javax.swing.JScrollPane.class).get(0);
            assertEquals(Space.PAGE, left(first, pane), name);
            assertEquals(1, first.getBorder().getBorderInsets(first).left, name + ": the line is at the edge");
        }
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
}
