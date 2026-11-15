package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import ca.ucalgary.chinookair.AppContext;
import ca.ucalgary.chinookair.business.control.Controllers;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Every screen says where the keyboard starts and which button Enter presses. */
class ScreenDefaultsTest {

    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    static Path folder;

    private static Controllers c;
    private static final RecordingNavigator NAV = new RecordingNavigator();
    private static final BookingSession BOOKING = new BookingSession();

    @BeforeAll
    static void startTheProgramParts() {
        c = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
    }

    @AfterAll
    static void shutDown() {
        c.shutdown().run();
    }

    @Test
    void loginAndRegisterStartInTheUsernameAndEnterPressesTheirMainButton() {
        LoginView login = new LoginView(c, NAV);
        assertSame(login.loginButton, login.defaultButton());
        assertSame(login.usernameField, login.initialFocus());
        RegisterView register = new RegisterView(c, NAV, (user, text) -> { });
        assertSame(register.registerButton, register.defaultButton());
        assertSame(register.usernameField, register.initialFocus());
    }

    @Test
    void theBookingScreensPressTheirNextStepAndStartInTheirFirstField() {
        FlightSearchView search = new FlightSearchView(c, NAV, BOOKING);
        assertSame(search.searchButton, search.defaultButton());
        assertSame(search.originCombo, search.initialFocus());
        PaymentView payment = new PaymentView(c, NAV, BOOKING);
        assertSame(payment.payButton, payment.defaultButton());
        assertSame(payment.nameField, payment.initialFocus());
        SeatSelectionView seats = new SeatSelectionView(c, NAV, BOOKING);
        assertSame(seats.continueButton, seats.defaultButton());
        ConfirmationView done = new ConfirmationView(c, NAV, BOOKING);
        assertSame(done.dashboardButton, done.defaultButton());
    }

    @Test
    void listScreensStartInTheirSearchBoxAndHaveNoDefaultButton() {
        CustomerManagementView customers = new CustomerManagementView(c, NAV, BOOKING);
        assertSame(customers.searchField, customers.initialFocus());
        assertNull(customers.defaultButton());
        PaymentRecordsView records = new PaymentRecordsView(c, NAV);
        assertSame(records.searchField, records.initialFocus());
        FlightManagementView flights = new FlightManagementView(c, NAV);
        assertSame(flights.filterField, flights.initialFocus());
        assertNull(flights.defaultButton(), "Add, Update and Remove are not an obvious choice");
        MyReservationsView mine = new MyReservationsView(c, NAV, BOOKING);
        assertSame(mine.table, mine.initialFocus());
    }

    @Test
    void formScreensPressTheirOneMainButton() {
        assertNotNull(new ProfileView(c, NAV).defaultButton());
        ScheduleView schedule = new ScheduleView(c, NAV);
        assertSame(schedule.showButton, schedule.defaultButton());
        RouteManagementView routes = new RouteManagementView(c, NAV);
        assertSame(routes.addButton, routes.defaultButton());
        AircraftManagementView aircraft = new AircraftManagementView(c, NAV);
        assertSame(aircraft.addButton, aircraft.defaultButton());
        assertSame(aircraft.modelField, aircraft.initialFocus());
        ModifyReservationView modify = new ModifyReservationView(c, NAV, BOOKING);
        assertSame(modify.modifyButton, modify.defaultButton());
    }

    @Test
    void anAgentDashboardStartsOnItsFirstCard() {
        AgentDashboard dashboard = new AgentDashboard(c, NAV);
        assertSame(dashboard.taskButtons().get(0), dashboard.initialFocus());
    }

    @Test
    void theCustomerHomeStartsInTheFromBoxAndEnterSearches() {
        CustomerHome home = new CustomerHome(c, NAV, BOOKING);
        assertSame(home.originCombo, home.initialFocus());
        assertSame(home.searchButton, home.defaultButton());
    }

    @Test
    void everyTableHasANameForScreenReaders() {
        assertNotNull(new FlightSearchView(c, NAV, BOOKING).table.getAccessibleContext().getAccessibleName());
        assertNotNull(new CustomerManagementView(c, NAV, BOOKING).table.getAccessibleContext().getAccessibleName());
        PaymentRecordsView records = new PaymentRecordsView(c, NAV);
        assertNotNull(records.customerTable.getAccessibleContext().getAccessibleName());
        assertNotNull(records.paymentTable.getAccessibleContext().getAccessibleName());
        assertNotNull(new FlightManagementView(c, NAV).table.getAccessibleContext().getAccessibleName());
        assertNotNull(new RouteManagementView(c, NAV).table.getAccessibleContext().getAccessibleName());
        assertNotNull(new AircraftManagementView(c, NAV).table.getAccessibleContext().getAccessibleName());
        assertNotNull(new ScheduleView(c, NAV).table.getAccessibleContext().getAccessibleName());
        assertNotNull(new MyReservationsView(c, NAV, BOOKING).table.getAccessibleContext().getAccessibleName());
        assertNotNull(new ModifyReservationView(c, NAV, BOOKING).table.getAccessibleContext().getAccessibleName());
    }
}
