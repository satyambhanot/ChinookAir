package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

class LoginViewTest {

    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    static Path folder;

    private static Controllers controllers;

    private RecordingNavigator navigator;
    private LoginView view;

    @BeforeAll
    static void startTheProgramParts() {
        controllers = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
    }

    @AfterAll
    static void shutDown() {
        controllers.shutdown().run();
    }

    @BeforeEach
    void newView() {
        controllers.session().logout();
        navigator = new RecordingNavigator();
        view = new LoginView(controllers, navigator);
    }

    private void logIn(String username, String password) {
        view.usernameField.setText(username);
        view.passwordField.setText(password);
        view.loginButton.doClick(0);
    }

    @Test
    void anEmptyUsernameIsAskedForInlineWithoutLeavingTheScreen() {
        logIn("", "whatever");
        assertEquals("Username is required", view.message());
        assertTrue(navigator.shown.isEmpty());
        assertTrue(controllers.session().currentUser().isEmpty());
    }

    @Test
    void anEmptyPasswordIsAskedFor() {
        logIn("admin", "");
        assertEquals("Password is required", view.message());
        assertTrue(navigator.shown.isEmpty());
    }

    @Test
    void aWrongPasswordShowsTheSameMessageAndClearsThePassword() {
        logIn("admin", "wrong");
        assertEquals("Invalid username or password", view.message());
        assertEquals("", new String(view.passwordField.getPassword()));
        assertEquals("admin", view.usernameField.getText());
        assertTrue(navigator.shown.isEmpty());
        assertTrue(controllers.session().currentUser().isEmpty());
    }

    @Test
    void anUnknownUserLooksTheSameAsAWrongPassword() {
        logIn("nobody", "whatever1");
        assertEquals("Invalid username or password", view.message());
    }

    @Test
    void theCustomerGoesToTheCustomerDashboard() {
        logIn("customer1", "customer123");
        assertEquals(ViewId.CUSTOMER_DASHBOARD, navigator.last());
        assertEquals("customer1", controllers.session().currentUser().orElseThrow().username());
    }

    @Test
    void theAgentGoesToTheAgentDashboardAndTheAdminToTheAdminDashboard() {
        logIn("agent1", "agent123");
        assertEquals(ViewId.AGENT_DASHBOARD, navigator.last());
        controllers.session().logout();
        logIn("admin", "admin123");
        assertEquals(ViewId.ADMIN_DASHBOARD, navigator.last());
    }

    @Test
    void theFieldsAreClearedAfterALogin() {
        logIn("admin", "admin123");
        assertEquals("", view.usernameField.getText());
        assertEquals("", new String(view.passwordField.getPassword()));
    }

    @Test
    void enterInThePasswordFieldLogsIn() {
        view.usernameField.setText("agent1");
        view.passwordField.setText("agent123");
        view.passwordField.postActionEvent();
        assertEquals(ViewId.AGENT_DASHBOARD, navigator.last());
    }

    @Test
    void enterInTheUsernameFieldAlsoSubmits() {
        view.usernameField.setText("agent1");
        view.passwordField.setText("agent123");
        view.usernameField.postActionEvent();
        assertEquals(ViewId.AGENT_DASHBOARD, navigator.last());
    }

    @Test
    void theRegisterLinkOpensTheRegistrationScreen() {
        view.registerButton.doClick(0);
        assertEquals(ViewId.REGISTER, navigator.last());
    }

    @Test
    void showingTheScreenAgainStartsFromScratch() {
        logIn("admin", "wrong");
        view.usernameField.setText("admin");
        view.onShow();
        assertEquals("", view.usernameField.getText());
        assertEquals("", view.message());
    }

    @Test
    void aNoticeAndAPrefilledUsernameCanBeShown() {
        view.prefill("newuser");
        view.showNotice("Account created. You can log in now.");
        assertEquals("newuser", view.usernameField.getText());
        assertEquals("Account created. You can log in now.", view.message());
    }

    @Test
    void aFailedAttemptAfterANoticeReplacesTheNotice() {
        view.showNotice("Account created.");
        logIn("admin", "wrong");
        assertEquals("Invalid username or password", view.message());
        assertNull(navigator.last());
    }
}
