package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.control.PromotionListener;
import ca.ucalgary.chinookair.business.entity.Promotion;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * The one window of the program. It holds the screens as cards and shows one at a time; screens ask
 * for another screen through the {@link Navigator}. Closing the window closes the database.
 */
public class MainFrame extends JFrame implements Navigator, PromotionListener {

    private static final long serialVersionUID = 1L;

    private final transient Controllers controllers;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final AppBar appBar;
    private final Map<ViewId, Component> screens = new EnumMap<>(ViewId.class);
    private ViewId current;
    private Consumer<Promotion> promotionDisplay = this::openPromotionDialog;

    public MainFrame(Controllers controllers) {
        super("ChinookAir");
        this.controllers = controllers;
        setIconImages(Brand.icons());
        Dimension room = roomOnScreen();
        setMinimumSize(smallestWindow(room));
        setSize(startingWindow(room));
        setLocationRelativeTo(null);
        appBar = new AppBar(controllers, this);
        JPanel root = new JPanel(new BorderLayout());
        root.add(appBar, BorderLayout.NORTH);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);
        // the window closes itself, and the database is closed first
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                showStartupPromotion();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                controllers.shutdown().run();
            }
        });
        controllers.promotionController().addListener(this);
        buildScreens();
        show(ViewId.LOGIN);
    }

    /** The window size the screens are designed for, and the least they still work in. */
    private static final Dimension USUAL = new Dimension(1000, 700);
    private static final Dimension SMALLEST = new Dimension(960, 640);
    /** Below this a screen would have to hide things, so the window is never made smaller. */
    private static final Dimension FLOOR = new Dimension(760, 440);

    /** The part of the screen a window can use, in the units the program draws in (a scaled screen is smaller). */
    private static Dimension roomOnScreen() {
        if (GraphicsEnvironment.isHeadless()) {
            return USUAL;
        }
        Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        return new Dimension(usable.width, usable.height);
    }

    /** 960 x 640, or less on a small or scaled screen, so the window always fits on it. */
    static Dimension smallestWindow(Dimension room) {
        return new Dimension(Math.max(FLOOR.width, Math.min(SMALLEST.width, room.width)),
                Math.max(FLOOR.height, Math.min(SMALLEST.height, room.height)));
    }

    /** The usual window, or the whole usable screen when that is smaller. */
    static Dimension startingWindow(Dimension room) {
        Dimension smallest = smallestWindow(room);
        return new Dimension(Math.max(smallest.width, Math.min(USUAL.width, room.width)),
                Math.max(smallest.height, Math.min(USUAL.height, room.height)));
    }

    /** Builds every screen once; each ViewId has one. */
    private void buildScreens() {
        LoginView login = new LoginView(controllers, this);
        register(ViewId.LOGIN, login);
        register(ViewId.REGISTER, new RegisterView(controllers, this, (username, message) -> {
            show(ViewId.LOGIN);
            login.prefill(username);
            login.showNotice(message);
        }));
        BookingSession booking = new BookingSession();
        register(ViewId.CUSTOMER_DASHBOARD, new CustomerHome(controllers, this, booking));
        register(ViewId.AGENT_DASHBOARD, new AgentDashboard(controllers, this));
        register(ViewId.ADMIN_DASHBOARD, new AdminDashboard(controllers, this));
        register(ViewId.FLIGHT_SEARCH, new FlightSearchView(controllers, this, booking));
        register(ViewId.SEAT_SELECTION, new SeatSelectionView(controllers, this, booking));
        register(ViewId.PAYMENT, new PaymentView(controllers, this, booking));
        register(ViewId.CONFIRMATION, new ConfirmationView(controllers, this, booking));
        register(ViewId.MY_RESERVATIONS, new MyReservationsView(controllers, this, booking));
        register(ViewId.CUSTOMER_MANAGEMENT, new CustomerManagementView(controllers, this, booking));
        register(ViewId.PROFILE, new ProfileView(controllers, this));
        register(ViewId.MODIFY_RESERVATION, new ModifyReservationView(controllers, this, booking));
        register(ViewId.PAYMENT_RECORDS, new PaymentRecordsView(controllers, this));
        register(ViewId.SCHEDULE, new ScheduleView(controllers, this));
        register(ViewId.FLIGHT_MANAGEMENT, new FlightManagementView(controllers, this));
        register(ViewId.ROUTE_MANAGEMENT, new RouteManagementView(controllers, this));
        register(ViewId.AIRCRAFT_MANAGEMENT, new AircraftManagementView(controllers, this));
    }

    /** Shows a screen. */
    @Override
    public final void show(ViewId view) {
        if (view == null) {
            throw new IllegalArgumentException("A screen is required");
        }
        current = view;
        cards.show(content, view.name());
        Component screen = screens.get(view);
        if (screen instanceof Screen) {
            // a screen may send the user elsewhere in here, for example to the login screen
            ((Screen) screen).onShow();
        }
        // after onShow, because a screen may have sent the user elsewhere
        appBar.refresh(current);
        if (current == view && screen instanceof Screen) {
            useKeyboardDefaults((Screen) screen);
        }
    }

    /** Enter presses the screen's default button and the keyboard starts in the screen's first field. */
    private void useKeyboardDefaults(Screen screen) {
        getRootPane().setDefaultButton(screen.defaultButton());
        JComponent focus = screen.initialFocus();
        if (focus != null) {
            // later, so the card has been shown before it is asked for the focus
            SwingUtilities.invokeLater(focus::requestFocusInWindow);
        }
    }

    /** Puts a built screen in the window, in place of an earlier one for the same ViewId. */
    void register(ViewId view, JComponent screen) {
        content.add(ContentPane.scrolling(new ContentPane(screen)), view.name());
        screens.put(view, screen);
    }

    /**
     * Asks the PromotionController once the window is open. On the first start of a calendar month it
     * tells this frame, which shows the promotion over the login screen.
     */
    void showStartupPromotion() {
        controllers.promotionController().checkOnStartup();
    }

    /** The Observer side: the controller found this month's promotion. */
    @Override
    public void onPromotion(Promotion promotion) {
        promotionDisplay.accept(promotion);
    }

    private void openPromotionDialog(Promotion promotion) {
        // later, so the window is painted first and the dialog sits on top of it
        SwingUtilities.invokeLater(() -> new PromotionDialog(this, promotion).setVisible(true));
    }

    /** Lets a test see the promotion instead of opening a modal dialog. */
    void setPromotionDisplay(Consumer<Promotion> display) {
        this.promotionDisplay = display;
    }

    /** The screen that is showing now, for tests. */
    ViewId currentView() {
        return current;
    }

    /** The screen that stands for a view, for tests. */
    Component screenFor(ViewId view) {
        return screens.get(view);
    }

    AppBar appBar() {
        return appBar;
    }

    Controllers controllers() {
        return controllers;
    }
}
