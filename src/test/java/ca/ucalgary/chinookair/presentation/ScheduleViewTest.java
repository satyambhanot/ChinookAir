package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.AppContext;
import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.data.DatabaseSeeder;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScheduleViewTest {

    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    static Path folder;

    private static Controllers controllers;

    private RecordingNavigator navigator;
    private ScheduleView view;

    @BeforeAll
    static void startTheProgramParts() {
        controllers = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
    }

    @AfterAll
    static void shutDown() {
        controllers.shutdown().run();
    }

    @BeforeEach
    void anAgentOpensTheSchedule() {
        controllers.session().login(controllers.loginController()
                .authenticate(DatabaseSeeder.AGENT_USERNAME, DatabaseSeeder.AGENT_PASSWORD));
        navigator = new RecordingNavigator();
        view = new ScheduleView(controllers, navigator);
        view.onShow();
    }

    private void show(String from, String to) {
        view.fromField.setText(from);
        view.toField.setText(to);
        view.showButton.doClick(0);
    }

    @Test
    void itStartsWithTodayAndAWeekAndShowsTheFlightsOfThatTime() {
        assertEquals("2026-12-01", view.fromField.getText());
        assertEquals("2026-12-08", view.toField.getText());
        assertTrue(view.tableModel.getRowCount() > 0);
    }

    @Test
    void flightsAreOrderedByDeparture() {
        show("2026-12-01", "2026-12-03");
        assertTrue(view.tableModel.getRowCount() >= 2);
        for (int row = 1; row < view.tableModel.getRowCount(); row++) {
            assertTrue(view.tableModel.flightAt(row - 1).departure()
                    .compareTo(view.tableModel.flightAt(row).departure()) <= 0);
            assertTrue(view.tableModel.flightAt(row).departure().toLocalDate().isBefore(
                    LocalDate.of(2026, 12, 4)));
        }
    }

    @Test
    void aPeriodWithNoFlightsShowsAnEmptyTableAndAMessage() {
        show("2020-01-01", "2020-01-02");
        assertEquals(0, view.tableModel.getRowCount());
        assertEquals("No flights in this period.", view.message());
    }

    @Test
    void aBadDateIsExplainedAndTheTableIsUnchanged() {
        int rows = view.tableModel.getRowCount();
        show("2026-13-40", "2026-12-03");
        assertTrue(view.message().startsWith("Date must look like yyyy-MM-dd"));
        assertEquals(rows, view.tableModel.getRowCount());
    }

    @Test
    void aLastDayBeforeTheFirstIsRefused() {
        show("2026-12-05", "2026-12-01");
        assertEquals("The last day cannot be before the first day", view.message());
    }

    @Test
    void anAdministratorMayLookToo() {
        controllers.session().login(controllers.loginController()
                .authenticate(DatabaseSeeder.ADMIN_USERNAME, DatabaseSeeder.ADMIN_PASSWORD));
        view.onShow();
        assertTrue(view.tableModel.getRowCount() > 0);
        view.backButton.doClick(0);
        assertEquals(ViewId.ADMIN_DASHBOARD, navigator.last());
    }

    @Test
    void aCustomerIsRefused() {
        controllers.session().login(controllers.loginController()
                .authenticate(DatabaseSeeder.CUSTOMER_USERNAME, DatabaseSeeder.CUSTOMER_PASSWORD));
        show("2026-12-01", "2026-12-03");
        assertEquals("You do not have permission to do that.", view.message());
    }

    @Test
    void withNobodyLoggedInTheScreenSendsYouToLogin() {
        controllers.session().logout();
        view.onShow();
        assertEquals(ViewId.LOGIN, navigator.last());
    }

    // ---- the administrator moves flights

    private void asAdmin() {
        controllers.session().login(controllers.loginController()
                .authenticate(DatabaseSeeder.ADMIN_USERNAME, DatabaseSeeder.ADMIN_PASSWORD));
        view.onShow();
        show("2026-12-02", "2026-12-20");
    }

    @Test
    void theRescheduleControlsAreOnlyForTheAdministrator() {
        assertFalse(view.rescheduleButton.getParent().isVisible());
        asAdmin();
        assertTrue(view.rescheduleButton.getParent().isVisible());
    }

    @Test
    void choosingAFlightFillsTheNewTimesAndEnablesTheButton() {
        asAdmin();
        assertFalse(view.rescheduleButton.isEnabled());
        view.table.setRowSelectionInterval(0, 0);
        assertTrue(view.rescheduleButton.isEnabled());
        assertEquals(view.tableModel.getValueAt(0, 2), view.newDepartureField.getText());
        assertEquals(view.tableModel.getValueAt(0, 3), view.newArrivalField.getText());
    }

    @Test
    void aFlightMovedToAFreeSlotShowsTheNewTimeInTheTable() {
        asAdmin();
        view.table.setRowSelectionInterval(0, 0);
        String number = (String) view.tableModel.getValueAt(0, 0);
        view.newDepartureField.setText("2026-12-19 03:00");
        view.newArrivalField.setText("2026-12-19 04:30");
        view.rescheduleButton.doClick(0);
        assertEquals("Rescheduled " + number, view.message());
        boolean found = false;
        for (int row = 0; row < view.tableModel.getRowCount(); row++) {
            if (view.tableModel.getValueAt(row, 0).equals(number)) {
                assertEquals("2026-12-19 03:00", view.tableModel.getValueAt(row, 2));
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    void aFlightMovedOntoAnotherFlightOfTheSameAircraftIsRefused() {
        asAdmin();
        // two flights of one aircraft: move the first onto the time of the second
        int first = 0;
        int second = -1;
        for (int row = 1; row < view.tableModel.getRowCount() && second < 0; row++) {
            if (view.tableModel.flightAt(row).aircraft().id().equals(view.tableModel.flightAt(first).aircraft().id())) {
                second = row;
            }
        }
        assertTrue(second > 0);
        view.table.setRowSelectionInterval(first, first);
        view.newDepartureField.setText((String) view.tableModel.getValueAt(second, 2));
        view.newArrivalField.setText((String) view.tableModel.getValueAt(second, 3));
        view.rescheduleButton.doClick(0);
        assertTrue(view.message().contains("Aircraft is already assigned"), view.message());
    }

    @Test
    void aDepartureInThePastIsRefused() {
        asAdmin();
        view.table.setRowSelectionInterval(0, 0);
        view.newDepartureField.setText("2026-11-30 10:00");
        view.newArrivalField.setText("2026-11-30 12:00");
        view.rescheduleButton.doClick(0);
        assertTrue(view.message().contains("must be in the future"), view.message());
    }

    @Test
    void aBadTimeIsExplained() {
        asAdmin();
        view.table.setRowSelectionInterval(0, 0);
        view.newDepartureField.setText("soon");
        view.rescheduleButton.doClick(0);
        assertTrue(view.message().startsWith("Date and time must look like"), view.message());
    }
}
