package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.control.PromotionListener;
import ca.ucalgary.chinookair.business.entity.Promotion;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
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
    private final Set<ViewId> added = EnumSet.noneOf(ViewId.class);
    private final Map<ViewId, Component> screens = new EnumMap<>(ViewId.class);
    private ViewId current;
    private Consumer<Promotion> promotionDisplay = this::openPromotionDialog;

    public MainFrame(Controllers controllers) {
        super("ChinookAir");
        this.controllers = controllers;
        setSize(1000, 700);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);
        setContentPane(content);
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

    /** The screens that exist so far; the others show a "coming soon" page until their issue is done. */
    private void buildScreens() {
        LoginView login = new LoginView(controllers, this);
        register(ViewId.LOGIN, login);
        register(ViewId.REGISTER, new RegisterView(controllers, this, (username, message) -> {
            show(ViewId.LOGIN);
            login.prefill(username);
            login.showNotice(message);
        }));
        register(ViewId.CUSTOMER_DASHBOARD, new CustomerDashboard(controllers, this));
        register(ViewId.AGENT_DASHBOARD, new AgentDashboard(controllers, this));
        register(ViewId.ADMIN_DASHBOARD, new AdminDashboard(controllers, this));
        BookingSession booking = new BookingSession();
        register(ViewId.FLIGHT_SEARCH, new FlightSearchView(controllers, this, booking));
        register(ViewId.SEAT_SELECTION, new SeatSelectionView(controllers, this, booking));
        register(ViewId.PAYMENT, new PaymentView(controllers, this, booking));
        register(ViewId.CONFIRMATION, new ConfirmationView(controllers, this, booking));
    }

    /** Shows a screen. A screen that is not built yet shows a "coming soon" page. */
    @Override
    public final void show(ViewId view) {
        if (view == null) {
            throw new IllegalArgumentException("A screen is required");
        }
        if (!added.contains(view)) {
            register(view, new ComingSoonView(view));
        }
        current = view;
        cards.show(content, view.name());
        Component screen = screens.get(view);
        if (screen instanceof Screen) {
            // a screen may send the user elsewhere in here, for example to the login screen
            ((Screen) screen).onShow();
        }
    }

    /** Puts a built screen in place of the one that stands for it. */
    void register(ViewId view, JComponent screen) {
        content.add(screen, view.name());
        screens.put(view, screen);
        added.add(view);
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

    Controllers controllers() {
        return controllers;
    }
}
