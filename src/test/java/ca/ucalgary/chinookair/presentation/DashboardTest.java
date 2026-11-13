package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.AppContext;
import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Role;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DashboardTest {

    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    static Path folder;

    private static Controllers controllers;

    private RecordingNavigator navigator;

    @BeforeAll
    static void startTheProgramParts() {
        controllers = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
    }

    @AfterAll
    static void shutDown() {
        controllers.shutdown().run();
    }

    @BeforeEach
    void fresh() {
        navigator = new RecordingNavigator();
        controllers.session().logout();
    }

    private void logIn(String username, String password) {
        controllers.session().login(controllers.loginController().authenticate(username, password));
    }

    private static List<String> labels(Dashboard dashboard) {
        return dashboard.taskButtons().stream().map(JButton::getText).toList();
    }

    private static void click(Dashboard dashboard, String label) {
        dashboard.taskButtons().stream().filter(b -> b.getText().equals(label)).findFirst().orElseThrow().doClick(0);
    }

    // ---- the customer

    @Test
    void theCustomerDashboardHasItsFourButtonsAndAHeader() {
        logIn("customer1", "customer123");
        CustomerDashboard dashboard = new CustomerDashboard(controllers, navigator);
        dashboard.onShow();
        assertEquals("Logged in as customer1 (Customer)", dashboard.headerText());
        assertEquals(List.of("Search flights", "My reservations", "My profile"), labels(dashboard));
        assertEquals("Log out", dashboard.logOutButton().getText());
        assertTrue(navigator.shown.isEmpty());
    }

    @Test
    void theCustomerButtonsOpenTheirScreens() {
        logIn("customer1", "customer123");
        CustomerDashboard dashboard = new CustomerDashboard(controllers, navigator);
        click(dashboard, "Search flights");
        click(dashboard, "My reservations");
        click(dashboard, "My profile");
        assertEquals(List.of(ViewId.FLIGHT_SEARCH, ViewId.MY_RESERVATIONS, ViewId.PROFILE), navigator.shown);
    }

    // ---- the agent

    @Test
    void theAgentDashboardHasItsButtonsAndAHeader() {
        logIn("agent1", "agent123");
        AgentDashboard dashboard = new AgentDashboard(controllers, navigator);
        dashboard.onShow();
        assertEquals("Logged in as agent1 (Flight agent)", dashboard.headerText());
        assertEquals(List.of("Customers", "Reservations", "Payment records", "Schedule"), labels(dashboard));
        click(dashboard, "Customers");
        click(dashboard, "Reservations");
        click(dashboard, "Payment records");
        click(dashboard, "Schedule");
        assertEquals(List.of(ViewId.CUSTOMER_MANAGEMENT, ViewId.MY_RESERVATIONS, ViewId.PAYMENT_RECORDS,
                ViewId.SCHEDULE), navigator.shown);
    }

    // ---- the administrator

    @Test
    void theAdminDashboardHasItsButtonsAndAHeader() {
        logIn("admin", "admin123");
        AdminDashboard dashboard = new AdminDashboard(controllers, navigator);
        dashboard.onShow();
        assertEquals("Logged in as admin (Administrator)", dashboard.headerText());
        assertEquals(List.of("Flights", "Routes", "Aircraft", "Schedule"), labels(dashboard));
        click(dashboard, "Flights");
        click(dashboard, "Routes");
        click(dashboard, "Aircraft");
        click(dashboard, "Schedule");
        assertEquals(List.of(ViewId.FLIGHT_MANAGEMENT, ViewId.ROUTE_MANAGEMENT, ViewId.AIRCRAFT_MANAGEMENT,
                ViewId.SCHEDULE), navigator.shown);
    }

    // ---- logging out and guarding

    @Test
    void logOutClearsTheSessionAndReturnsToLogin() {
        logIn("admin", "admin123");
        AdminDashboard dashboard = new AdminDashboard(controllers, navigator);
        dashboard.logOutButton().doClick(0);
        assertTrue(controllers.session().currentUser().isEmpty());
        assertEquals(ViewId.LOGIN, navigator.last());
    }

    @Test
    void aDashboardShownWithoutALoginSendsYouToTheLoginScreen() {
        new AdminDashboard(controllers, navigator).onShow();
        assertEquals(List.of(ViewId.LOGIN), navigator.shown);
    }

    @Test
    void aDashboardOfAnotherRoleIsNotShown() {
        logIn("customer1", "customer123");
        new AdminDashboard(controllers, navigator).onShow();
        new AgentDashboard(controllers, navigator).onShow();
        assertEquals(List.of(ViewId.LOGIN, ViewId.LOGIN), navigator.shown);
    }

    @Test
    void theButtonsHaveMnemonicsThatDoNotClash() {
        for (Dashboard dashboard : List.of(new CustomerDashboard(controllers, navigator),
                new AgentDashboard(controllers, navigator), new AdminDashboard(controllers, navigator))) {
            List<Integer> keys = new ArrayList<>();
            dashboard.taskButtons().forEach(b -> keys.add(b.getMnemonic()));
            keys.add(dashboard.logOutButton().getMnemonic());
            assertEquals(keys.size(), keys.stream().distinct().count(), dashboard.getClass().getSimpleName());
        }
    }

    @Test
    void everyRoleHasADashboardView() {
        assertEquals(ViewId.CUSTOMER_DASHBOARD, ViewId.dashboardFor(Role.CUSTOMER));
        assertEquals(ViewId.AGENT_DASHBOARD, ViewId.dashboardFor(Role.AGENT));
        assertEquals(ViewId.ADMIN_DASHBOARD, ViewId.dashboardFor(Role.ADMIN));
    }
}
