package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.AppContext;
import ca.ucalgary.chinookair.business.control.Controllers;
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

class AircraftManagementViewTest {

    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    static Path folder;

    private static Controllers controllers;

    private RecordingNavigator navigator;
    private AircraftManagementView view;

    @BeforeAll
    static void startTheProgramParts() {
        controllers = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
    }

    @AfterAll
    static void shutDown() {
        controllers.shutdown().run();
    }

    @BeforeEach
    void theAdministratorOpensTheScreen() {
        controllers.session().login(controllers.loginController()
                .authenticate(DatabaseSeeder.ADMIN_USERNAME, DatabaseSeeder.ADMIN_PASSWORD));
        navigator = new RecordingNavigator();
        view = new AircraftManagementView(controllers, navigator);
        view.onShow();
    }

    private void add(String model, String rows, String seats) {
        view.modelField.setText(model);
        view.rowsField.setText(rows);
        view.seatsField.setText(seats);
        view.addButton.doClick(0);
    }

    @Test
    void aNewAircraftShowsItsComputedSeatCount() {
        int before = view.tableModel.getRowCount();
        add("E190", "25", "4");
        assertEquals(before + 1, view.tableModel.getRowCount());
        assertEquals(100, view.tableModel.getValueAt(before, 3));
        assertEquals("Added E190 with 100 seats", view.message());
        assertEquals("", view.modelField.getText());
    }

    @Test
    void zeroRowsIsExplained() {
        int before = view.tableModel.getRowCount();
        add("E190", "0", "4");
        assertTrue(view.message().contains("Rows"), view.message());
        assertEquals(before, view.tableModel.getRowCount());
    }

    @Test
    void tooManySeatsPerRowIsExplained() {
        add("E190", "10", "9");
        assertTrue(view.message().startsWith("Seats per row must be between"), view.message());
    }

    @Test
    void textInsteadOfANumberIsExplained() {
        add("E190", "many", "4");
        assertEquals("Rows must be a whole number", view.message());
    }

    @Test
    void aBlankModelIsExplained() {
        add(" ", "10", "4");
        assertTrue(view.message().contains("Aircraft model"), view.message());
    }

    @Test
    void anAgentCannotReachTheScreen() {
        controllers.session().login(controllers.loginController()
                .authenticate(DatabaseSeeder.AGENT_USERNAME, DatabaseSeeder.AGENT_PASSWORD));
        view.onShow();
        assertEquals(ViewId.LOGIN, navigator.last());
    }

    @Test
    void backGoesToTheAdministratorDashboard() {
        view.backButton.doClick(0);
        assertEquals(ViewId.ADMIN_DASHBOARD, navigator.last());
    }
}
