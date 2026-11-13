package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.AppContext;
import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.data.DatabaseSeeder;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProfileViewTest {

    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    static Path folder;

    private static Controllers controllers;
    private static int next;

    private Customer customer;
    private RecordingNavigator navigator;
    private ProfileView view;

    @BeforeAll
    static void startTheProgramParts() {
        controllers = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
    }

    @AfterAll
    static void shutDown() {
        controllers.shutdown().run();
    }

    @BeforeEach
    void aCustomerOpensTheirProfile() {
        int n = next++;
        customer = controllers.customerController().register("prof" + n, "password1", "Pat Profile " + n,
                "pat" + n + "@x.ca", null);
        controllers.session().login(customer);
        navigator = new RecordingNavigator();
        view = new ProfileView(controllers, navigator);
        view.onShow();
    }

    @Test
    void theFormShowsOnlyTheCustomersOwnDataAndNoPasswordOrSearch() {
        assertEquals(customer.name(), view.form.nameField.getText());
        assertEquals(customer.email(), view.form.emailField.getText());
        assertEquals(customer.username(), view.form.usernameField.getText());
        assertFalse(view.form.usernameField.isEditable());
        assertFalse(view.form.passwordField.isVisible());
    }

    @Test
    void savingChangesTheDataAndTheSession() {
        view.form.phoneField.setText("403-555-0123");
        view.form.nameField.setText("Pat Changed");
        view.saveButton.doClick(0);
        assertEquals("Saved", view.message());
        Customer stored = controllers.customerController().view(customer, customer.id());
        assertEquals("403-555-0123", stored.phone());
        assertEquals("Pat Changed", ((Customer) controllers.session().currentUser().orElseThrow()).name());
    }

    @Test
    void anEmailOfAnotherCustomerIsRefused() {
        controllers.customerController().register("owner" + next, "password1", "Owner", "owner" + next + "@x.ca",
                null);
        view.form.emailField.setText("owner" + next + "@x.ca");
        view.saveButton.doClick(0);
        assertEquals("Email already registered", view.message());
    }

    @Test
    void aBadEmailIsExplained() {
        view.form.emailField.setText("a@b");
        view.saveButton.doClick(0);
        assertTrue(view.message().toLowerCase().contains("email"));
    }

    @Test
    void anAgentIsSentToLoginBecauseThereIsNoProfileForThem() {
        controllers.session().login(controllers.loginController()
                .authenticate(DatabaseSeeder.AGENT_USERNAME, DatabaseSeeder.AGENT_PASSWORD));
        view.onShow();
        assertEquals(ViewId.LOGIN, navigator.last());
    }

    @Test
    void backGoesToTheCustomerDashboard() {
        view.backButton.doClick(0);
        assertEquals(ViewId.CUSTOMER_DASHBOARD, navigator.last());
    }
}
