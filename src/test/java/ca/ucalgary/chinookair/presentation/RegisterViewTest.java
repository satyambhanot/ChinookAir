package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.AppContext;
import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Customer;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RegisterViewTest {

    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    static Path folder;

    private static Controllers controllers;

    private RecordingNavigator navigator;
    private RegisterView view;
    private final List<String> registered = new ArrayList<>();

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
        navigator = new RecordingNavigator();
        registered.clear();
        view = new RegisterView(controllers, navigator, (username, message) -> {
            registered.add(username);
            registered.add(message);
        });
    }

    private void fill(String username, String password, String name, String email, String phone) {
        view.usernameField.setText(username);
        view.passwordField.setText(password);
        view.nameField.setText(name);
        view.emailField.setText(email);
        view.phoneField.setText(phone);
    }

    @Test
    void aNewAccountIsCreatedAndTheLoginScreenIsToldSoAndTheFieldsAreCleared() {
        fill("newuser", "pass1234", "New User", "new@x.ca", "");
        view.registerButton.doClick(0);

        assertEquals(List.of("newuser", "Account created. You can log in now."), registered);
        assertEquals("", view.usernameField.getText());
        assertEquals("", view.nameField.getText());
        // and the new account really works
        Customer found = (Customer) controllers.loginController().authenticate("newuser", "pass1234");
        assertEquals("New User", found.name());
        assertEquals("new@x.ca", found.email());
    }

    @Test
    void aDuplicateEmailIsExplainedAndNothingIsCreated() {
        fill("firstone", "pass1234", "First One", "same@x.ca", "");
        view.registerButton.doClick(0);
        registered.clear();

        fill("secondone", "pass1234", "Second One", "SAME@x.ca", "");
        view.registerButton.doClick(0);

        assertEquals("Email already registered", view.message());
        assertTrue(registered.isEmpty());
        assertEquals("secondone", view.usernameField.getText());
        assertEquals("", new String(view.passwordField.getPassword()));
    }

    @Test
    void aDuplicateUsernameIsExplained() {
        fill("takenname", "pass1234", "Taken Name", "taken@x.ca", "");
        view.registerButton.doClick(0);
        fill("TakenName", "pass1234", "Other", "other@x.ca", "");
        view.registerButton.doClick(0);
        assertEquals("Username already taken", view.message());
    }

    @Test
    void thePasswordRulesAreExplained() {
        fill("shortpass", "short1", "Short Pass", "short@x.ca", "");
        view.registerButton.doClick(0);
        assertTrue(view.message().contains("at least 8 characters"), view.message());
        fill("nodigit", "longpassword", "No Digit", "nodigit@x.ca", "");
        view.registerButton.doClick(0);
        assertTrue(view.message().contains("digit"), view.message());
        assertTrue(registered.isEmpty());
    }

    @Test
    void emptyFieldsGetPlainMessages() {
        fill("", "pass1234", "", "", "");
        view.registerButton.doClick(0);
        assertNotNull(view.message());
        assertTrue(view.message().contains("Username"), view.message());
    }

    @Test
    void enterInThePhoneFieldSubmits() {
        fill("enterkey", "pass1234", "Enter Key", "enter@x.ca", "403-555-0101");
        view.phoneField.postActionEvent();
        assertEquals("enterkey", registered.get(0));
    }

    @Test
    void theBackButtonReturnsToTheLoginScreen() {
        view.backButton.doClick(0);
        assertEquals(ViewId.LOGIN, navigator.last());
    }

    @Test
    void showingTheScreenAgainStartsFromScratch() {
        fill("someone", "pass1234", "Some One", "s@x.ca", "");
        view.onShow();
        assertEquals("", view.usernameField.getText());
        assertEquals("", view.emailField.getText());
    }
}
