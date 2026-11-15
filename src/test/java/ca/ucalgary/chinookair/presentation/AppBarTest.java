package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.AppContext;
import ca.ucalgary.chinookair.business.control.Controllers;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    void itShowsWhoIsLoggedInAndWhereTheyAre() {
        logIn("agent1", "agent123");
        bar.refresh(ViewId.CUSTOMER_MANAGEMENT);
        assertTrue(bar.isVisible());
        assertEquals("agent1 · Flight agent", bar.user());
        assertEquals("Customers", bar.place());
        assertTrue(bar.homeButton().isVisible());
    }

    @Test
    void onTheHomeScreenThereIsNoHomeLink() {
        logIn("admin", "admin123");
        bar.refresh(ViewId.ADMIN_DASHBOARD);
        assertEquals("Home", bar.place());
        assertFalse(bar.homeButton().isVisible());
    }

    @Test
    void homeGoesToTheDashboardOfTheRole() {
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
    void everyScreenHasAShortLabelForTheBar() {
        for (ViewId view : ViewId.values()) {
            assertFalse(view.label().isBlank(), view.name());
        }
        assertEquals("Find flights", ViewId.FLIGHT_SEARCH.label());
        assertEquals("Home", ViewId.AGENT_DASHBOARD.label());
    }
}
