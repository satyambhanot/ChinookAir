package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.AppContext;
import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.control.FlightOffer;
import ca.ucalgary.chinookair.business.control.SearchCriteria;
import ca.ucalgary.chinookair.data.DatabaseSeeder;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JComponent;

/** Every screen of the program, built on a seeded database, for the tests that measure all of them. */
final class EveryScreen {

    static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    final RecordingNavigator navigator = new RecordingNavigator();
    final Map<String, JComponent> screens = new LinkedHashMap<>();
    Controllers controllers;
    PaymentView payment;
    ProfileView profile;
    ConfirmationView confirmation;

    /** Builds the screens, each opened the way the window opens it, with the right person logged in. */
    static EveryScreen build(Path folder) {
        Theme.install();
        EveryScreen all = new EveryScreen();
        all.fill(folder);
        return all;
    }

    private void fill(Path folder) {
        Controllers c = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
        controllers = c;
        BookingSession booking = new BookingSession();

        screens.put("login", shown(new LoginView(c, navigator)));
        screens.put("register", shown(new RegisterView(c, navigator, (name, password) -> { })));

        c.session().login(c.loginController()
                .authenticate(DatabaseSeeder.CUSTOMER_USERNAME, DatabaseSeeder.CUSTOMER_PASSWORD));
        screens.put("customer home", shown(new CustomerHome(c, navigator, booking)));
        FlightSearchView search = new FlightSearchView(c, navigator, booking);
        search.onShow();
        screens.put("find flights", search);
        FlightOffer offer = c.flightSearchController()
                .search(new SearchCriteria("YYC", "YVR", LocalDate.of(2026, 12, 2), null)).get(0);
        booking.select(offer);
        SeatSelectionView seats = new SeatSelectionView(c, navigator, booking);
        seats.onShow();
        screens.put("choose seat", seats);
        seats.seatButtonFor("3C").doClick(0);
        seats.continueButton.doClick(0);
        payment = new PaymentView(c, navigator, booking);
        payment.onShow();
        screens.put("payment", payment);
        payment.nameField.setText("Casey Customer");
        payment.cardField.setText("4111 1111 1111 1111");
        payment.expiryField.setText("12/30");
        payment.payButton.doClick(0);
        confirmation = new ConfirmationView(c, navigator, booking);
        confirmation.onShow();
        screens.put("confirmation", confirmation);
        screens.put("my reservations", shown(new MyReservationsView(c, navigator, booking)));
        profile = new ProfileView(c, navigator);
        profile.onShow();
        screens.put("profile", profile);

        c.session().login(c.loginController()
                .authenticate(DatabaseSeeder.AGENT_USERNAME, DatabaseSeeder.AGENT_PASSWORD));
        screens.put("agent home", shown(new AgentDashboard(c, navigator)));
        screens.put("customers", shown(new CustomerManagementView(c, navigator, booking)));
        screens.put("modify reservation", shown(new ModifyReservationView(c, navigator, booking)));
        screens.put("payment records", shown(new PaymentRecordsView(c, navigator)));
        screens.put("schedule", shown(new ScheduleView(c, navigator)));

        c.session().login(c.loginController()
                .authenticate(DatabaseSeeder.ADMIN_USERNAME, DatabaseSeeder.ADMIN_PASSWORD));
        screens.put("admin home", shown(new AdminDashboard(c, navigator)));
        screens.put("flights", shown(new FlightManagementView(c, navigator)));
        screens.put("routes", shown(new RouteManagementView(c, navigator)));
        screens.put("aircraft", shown(new AircraftManagementView(c, navigator)));
    }

    /** Opens a screen the way the window does, while the right person is logged in. */
    private static <T extends JComponent & Screen> T shown(T screen) {
        screen.onShow();
        return screen;
    }

    void shutDown() {
        controllers.shutdown().run();
    }
}
