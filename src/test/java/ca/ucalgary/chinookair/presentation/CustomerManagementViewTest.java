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

class CustomerManagementViewTest {

    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    static Path folder;

    private static Controllers controllers;
    private static int next;

    private Customer jo;
    private RecordingNavigator navigator;
    private BookingSession booking;
    private CustomerManagementView view;

    @BeforeAll
    static void startTheProgramParts() {
        controllers = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
    }

    @AfterAll
    static void shutDown() {
        controllers.shutdown().run();
    }

    @BeforeEach
    void anAgentOpensTheScreen() {
        int n = next++;
        jo = controllers.customerController().register("smithy" + n, "password1", "Zed Smithson" + n,
                "zed" + n + "@x.ca", "403-555-0100");
        controllers.session().login(controllers.loginController()
                .authenticate(DatabaseSeeder.AGENT_USERNAME, DatabaseSeeder.AGENT_PASSWORD));
        navigator = new RecordingNavigator();
        booking = new BookingSession();
        view = new CustomerManagementView(controllers, navigator, booking);
        view.onShow();
    }

    private void search(String text) {
        view.searchField.setText(text);
        view.searchButton.doClick(0);
    }

    @Test
    void searchingPartOfANameFindsTheCustomer() {
        search("smithson" + (next - 1));
        assertEquals(1, view.tableModel.getRowCount());
        assertEquals(jo.username(), view.tableModel.getValueAt(0, 3));
    }

    @Test
    void searchingForNobodyShowsAnEmptyTable() {
        search("zzzzqq");
        assertEquals(0, view.tableModel.getRowCount());
        assertEquals("No customers found.", view.message());
    }

    @Test
    void choosingARowFillsTheFormAndTheUsernameStaysLocked() {
        search(jo.email());
        view.table.setRowSelectionInterval(0, 0);
        assertEquals(jo.name(), view.form.nameField.getText());
        assertEquals("403-555-0100", view.form.phoneField.getText());
        assertFalse(view.form.usernameField.isEditable());
        assertTrue(view.saveButton.isEnabled());
        assertTrue(view.reservationsButton.isEnabled());
    }

    @Test
    void savingANewPhoneShowsItAfterSearchingAgain() {
        search(jo.email());
        view.table.setRowSelectionInterval(0, 0);
        view.form.phoneField.setText("403-555-0199");
        view.saveButton.doClick(0);
        search(jo.email());
        assertEquals("403-555-0199", view.tableModel.getValueAt(0, 2));
    }

    @Test
    void anEmailThatBelongsToSomeoneElseIsRefusedAndNothingIsSaved() {
        search(jo.email());
        view.table.setRowSelectionInterval(0, 0);
        String other = controllers.customerController().register("other" + next, "password1", "Other",
                "taken" + next + "@x.ca", null).email();
        view.form.emailField.setText(other);
        view.saveButton.doClick(0);
        assertEquals("Email already registered", view.message());
        search(jo.email());
        assertEquals(1, view.tableModel.getRowCount());
    }

    @Test
    void anInvalidEmailIsExplained() {
        search(jo.email());
        view.table.setRowSelectionInterval(0, 0);
        view.form.emailField.setText("a@b");
        view.saveButton.doClick(0);
        assertTrue(view.message().toLowerCase().contains("email"));
    }

    @Test
    void aNewCustomerIsRegisteredWithAStartingPassword() {
        view.newButton.doClick(0);
        assertTrue(view.form.usernameField.isEditable());
        int n = next;
        view.form.usernameField.setText("newbie" + n);
        view.form.passwordField.setText("password1");
        view.form.nameField.setText("New Person");
        view.form.emailField.setText("newbie" + n + "@x.ca");
        view.saveButton.doClick(0);
        assertEquals("Saved New Person", view.message());
        assertEquals(1, view.tableModel.getRowCount());
        assertEquals("newbie" + n, view.tableModel.getValueAt(0, 3));
        // the new customer can log in with the starting password
        assertEquals("newbie" + n, controllers.loginController().authenticate("newbie" + n, "password1").username());
    }

    @Test
    void aNewCustomerWithAWeakPasswordIsRefused() {
        view.newButton.doClick(0);
        view.form.usernameField.setText("weak" + next);
        view.form.passwordField.setText("short");
        view.form.nameField.setText("Weak Pass");
        view.form.emailField.setText("weak" + next + "@x.ca");
        view.saveButton.doClick(0);
        assertTrue(view.message().startsWith("Password must"));
    }

    @Test
    void reservationsOpensTheListForTheChosenCustomer() {
        search(jo.email());
        view.table.setRowSelectionInterval(0, 0);
        view.reservationsButton.doClick(0);
        assertEquals(ViewId.MY_RESERVATIONS, navigator.last());
        assertEquals(jo.id(), booking.customer().orElseThrow().id());
    }

    @Test
    void saveAndReservationsAreOffUntilSomeoneIsChosen() {
        assertFalse(view.saveButton.isEnabled());
        assertFalse(view.reservationsButton.isEnabled());
    }

    @Test
    void aCustomerCannotUseTheSearch() {
        controllers.session().login(jo);
        search("x");
        assertEquals("You do not have permission to do that.", view.message());
    }

    @Test
    void backGoesToTheAgentDashboard() {
        view.backButton.doClick(0);
        assertEquals(ViewId.AGENT_DASHBOARD, navigator.last());
    }

    @Test
    void withNobodyLoggedInTheScreenSendsYouToLogin() {
        controllers.session().logout();
        view.onShow();
        assertEquals(ViewId.LOGIN, navigator.last());
    }

    @Test
    void modifyOpensTheModifyScreenForTheChosenCustomer() {
        assertFalse(view.modifyButton.isEnabled());
        search(jo.email());
        view.table.setRowSelectionInterval(0, 0);
        view.modifyButton.doClick(0);
        assertEquals(ViewId.MODIFY_RESERVATION, navigator.last());
        assertEquals(jo.id(), booking.customer().orElseThrow().id());
    }
}
