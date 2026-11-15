package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.AppContext;
import ca.ucalgary.chinookair.business.control.Controllers;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import javax.swing.JButton;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppBarTest {

    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    static Path folder;

    private static Controllers controllers;

    private RecordingNavigator navigator;
    private AppBar bar;

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
        bar = new AppBar(controllers, navigator);
    }

    private void logIn(String username, String password) {
        controllers.session().login(controllers.loginController().authenticate(username, password));
    }

    private static List<String> words(List<JButton> buttons) {
        return buttons.stream().map(JButton::getText).toList();
    }

    @Test
    void theBarIsHiddenUntilSomeoneIsLoggedIn() {
        assertFalse(bar.isVisible());
        bar.refresh(ViewId.LOGIN);
        assertFalse(bar.isVisible());
        bar.refresh(ViewId.CUSTOMER_DASHBOARD);
        assertFalse(bar.isVisible());
    }

    @Test
    void theBarIsHiddenOnTheLoginAndRegisterScreensEvenWithALogin() {
        logIn("customer1", "customer123");
        bar.refresh(ViewId.LOGIN);
        assertFalse(bar.isVisible());
        bar.refresh(ViewId.REGISTER);
        assertFalse(bar.isVisible());
    }

    @Test
    void itShowsWhoIsLoggedIn() {
        logIn("agent1", "agent123");
        bar.refresh(ViewId.CUSTOMER_MANAGEMENT);
        assertTrue(bar.isVisible());
        assertEquals("agent1", bar.user());
        assertEquals("Flight agent", bar.role());
        assertEquals("A", bar.initial());
    }

    @Test
    void eachRoleSeesItsOwnTabs() {
        logIn("customer1", "customer123");
        bar.refresh(ViewId.CUSTOMER_DASHBOARD);
        assertEquals(List.of("Find flights", "My reservations", "My profile"), words(bar.tabs()));

        logIn("agent1", "agent123");
        bar.refresh(ViewId.AGENT_DASHBOARD);
        assertEquals(List.of("Customers", "Payment records", "Schedule"), words(bar.tabs()));

        logIn("admin", "admin123");
        bar.refresh(ViewId.ADMIN_DASHBOARD);
        assertEquals(List.of("Flights", "Routes", "Aircraft", "Schedule"), words(bar.tabs()));
    }

    @Test
    void theTabOfTheCurrentScreenIsMarked() {
        logIn("customer1", "customer123");
        bar.refresh(ViewId.MY_RESERVATIONS);
        assertEquals("My reservations", bar.selectedTab());
        bar.refresh(ViewId.PAYMENT);
        assertEquals("Find flights", bar.selectedTab());
        bar.refresh(ViewId.CUSTOMER_DASHBOARD);
        assertEquals("", bar.selectedTab());
    }

    @Test
    void theMarkedTabStaysRightWhenTheRoleChanges() {
        logIn("customer1", "customer123");
        bar.refresh(ViewId.MY_RESERVATIONS);
        logIn("agent1", "agent123");
        bar.refresh(ViewId.MY_RESERVATIONS);
        assertEquals("Customers", bar.selectedTab());
    }

    @Test
    void aTabOpensItsScreen() {
        logIn("admin", "admin123");
        bar.refresh(ViewId.ADMIN_DASHBOARD);
        bar.tabs().get(2).doClick(0);
        assertEquals(ViewId.AIRCRAFT_MANAGEMENT, navigator.last());
        bar.tabs().get(0).doClick(0);
        assertEquals(ViewId.FLIGHT_MANAGEMENT, navigator.last());
    }

    @Test
    void theLogoGoesToTheDashboardOfTheRole() {
        logIn("customer1", "customer123");
        bar.refresh(ViewId.FLIGHT_SEARCH);
        bar.homeButton().doClick(0);
        assertEquals(ViewId.CUSTOMER_DASHBOARD, navigator.last());

        logIn("admin", "admin123");
        bar.homeButton().doClick(0);
        assertEquals(ViewId.ADMIN_DASHBOARD, navigator.last());
    }

    @Test
    void logOutClearsTheSessionAndReturnsToLogin() {
        logIn("admin", "admin123");
        bar.refresh(ViewId.SCHEDULE);
        bar.logOutButton().doClick(0);
        assertTrue(controllers.session().currentUser().isEmpty());
        assertEquals(ViewId.LOGIN, navigator.last());
    }

    @Test
    void theStepsShowDuringABookingAndNotOtherwise() {
        logIn("customer1", "customer123");
        bar.refresh(ViewId.CUSTOMER_DASHBOARD);
        assertFalse(bar.steps().isVisible());
        assertNull(bar.steps().flow());

        bar.refresh(ViewId.SEAT_SELECTION);
        assertTrue(bar.steps().isVisible());
        assertEquals("Step 2 of 4: Choose a seat", bar.steps().description());

        bar.refresh(ViewId.MY_RESERVATIONS);
        assertFalse(bar.steps().isVisible());
    }

    @Test
    void anAgentSeesTheStepsOfChangingAReservation() {
        logIn("agent1", "agent123");
        bar.refresh(ViewId.MODIFY_RESERVATION);
        assertEquals("Step 1 of 3: Reservation", bar.steps().description());
        bar.refresh(ViewId.SEAT_SELECTION);
        assertEquals("Step 3 of 3: New seat", bar.steps().description());
    }

    @Test
    void theHomeButtonCanBeReachedByScreenReadersAndHasATip() {
        assertEquals("ChinookAir home", bar.homeButton().getAccessibleContext().getAccessibleName());
        assertNotNull(bar.homeButton().getToolTipText());
    }

    @Test
    void everyScreenHasAShortLabel() {
        for (ViewId view : ViewId.values()) {
            assertFalse(view.label().isBlank(), view.name());
        }
        assertEquals("Find flights", ViewId.FLIGHT_SEARCH.label());
        assertEquals("Home", ViewId.AGENT_DASHBOARD.label());
    }
}
