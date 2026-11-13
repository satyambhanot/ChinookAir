package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.AppContext;
import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Airport;
import ca.ucalgary.chinookair.data.DatabaseSeeder;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import javax.swing.JComboBox;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlightSearchViewTest {

    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    static Path folder;

    private static Controllers controllers;

    private RecordingNavigator navigator;
    private BookingSession booking;
    private FlightSearchView view;

    @BeforeAll
    static void startTheProgramParts() {
        controllers = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
    }

    @AfterAll
    static void shutDown() {
        controllers.shutdown().run();
    }

    @BeforeEach
    void openTheScreen() {
        controllers.session().login(controllers.loginController()
                .authenticate(DatabaseSeeder.CUSTOMER_USERNAME, DatabaseSeeder.CUSTOMER_PASSWORD));
        navigator = new RecordingNavigator();
        booking = new BookingSession();
        view = new FlightSearchView(controllers, navigator, booking);
        view.onShow();
    }

    private void choose(String origin, String destination, String date, String airlineCode) {
        select(view.originCombo, origin);
        select(view.destinationCombo, destination);
        view.dateField.setText(date);
        view.airlineCombo.setSelectedIndex(0);
        for (int i = 0; i < view.airlineCombo.getItemCount(); i++) {
            if (view.airlineCombo.getItemAt(i) != null && view.airlineCombo.getItemAt(i).code().equals(airlineCode)) {
                view.airlineCombo.setSelectedIndex(i);
            }
        }
    }

    private static void select(JComboBox<Airport> combo, String code) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).code().equals(code)) {
                combo.setSelectedIndex(i);
            }
        }
    }

    @Test
    void theScreenStartsWithTodayAndTheListsFilled() {
        assertEquals("2026-12-01", view.dateField.getText());
        assertTrue(view.originCombo.getItemCount() >= 2);
        assertEquals(view.originCombo.getItemCount(), view.destinationCombo.getItemCount());
        // "Any airline" first, then the airlines
        assertNull(view.airlineCombo.getItemAt(0));
        assertTrue(view.airlineCombo.getItemCount() >= 2);
        assertEquals(0, view.tableModel.getRowCount());
        assertFalse(view.selectButton.isEnabled());
    }

    @Test
    void aSearchOnASeededDayFillsTheTable() {
        choose("YYC", "YVR", "2026-12-02", null);
        view.searchButton.doClick(0);
        assertTrue(view.tableModel.getRowCount() >= 1);
        assertEquals("", view.message());
        assertEquals("YYC → YVR", view.tableModel.getValueAt(0, 2));
    }

    @Test
    void theAirlineFilterShowsOnlyThatAirline() {
        choose("YYC", "YVR", "2026-12-02", "AC");
        view.searchButton.doClick(0);
        assertTrue(view.tableModel.getRowCount() >= 1);
        for (int row = 0; row < view.tableModel.getRowCount(); row++) {
            assertEquals("Air Canada", view.tableModel.getValueAt(row, 1));
        }
    }

    @Test
    void aDateWithNoFlightsShowsTheEmptyMessage() {
        choose("YYC", "YVR", "2030-01-01", null);
        view.searchButton.doClick(0);
        assertEquals(0, view.tableModel.getRowCount());
        assertEquals("No flights found. Try another date or airline.", view.message());
    }

    @Test
    void theSameOriginAndDestinationIsRefusedAndTheTableIsUnchanged() {
        choose("YYC", "YVR", "2026-12-02", null);
        view.searchButton.doClick(0);
        int rows = view.tableModel.getRowCount();
        choose("YYC", "YYC", "2026-12-02", null);
        view.searchButton.doClick(0);
        assertEquals("Origin and destination must be different", view.message());
        assertEquals(rows, view.tableModel.getRowCount());
    }

    @Test
    void aBadDateIsExplainedAndNothingIsSearched() {
        choose("YYC", "YVR", "2026-13-40", null);
        view.searchButton.doClick(0);
        assertTrue(view.message().startsWith("Date must look like yyyy-MM-dd"));
        assertEquals(0, view.tableModel.getRowCount());
    }

    @Test
    void aDateInThePastIsRefused() {
        choose("YYC", "YVR", "2026-11-30", null);
        view.searchButton.doClick(0);
        assertEquals("Date cannot be in the past", view.message());
    }

    @Test
    void selectIsDisabledUntilARowIsChosenThenKeepsTheOfferAndOpensTheSeatScreen() {
        choose("YYC", "YVR", "2026-12-02", null);
        view.searchButton.doClick(0);
        assertFalse(view.selectButton.isEnabled());
        view.table.setRowSelectionInterval(0, 0);
        assertTrue(view.selectButton.isEnabled());
        view.table.clearSelection();
        assertFalse(view.selectButton.isEnabled());
        view.table.setRowSelectionInterval(0, 0);
        view.selectButton.doClick(0);
        assertEquals(ViewId.SEAT_SELECTION, navigator.last());
        assertEquals(view.tableModel.offerAt(view.table.convertRowIndexToModel(0)),
                booking.selectedOffer().orElseThrow());
    }

    @Test
    void backGoesToTheDashboardOfTheRole() {
        view.backButton.doClick(0);
        assertEquals(ViewId.CUSTOMER_DASHBOARD, navigator.last());
    }

    @Test
    void withNobodyLoggedInTheScreenSendsYouToLogin() {
        controllers.session().logout();
        navigator.shown.clear();
        view.onShow();
        assertEquals(ViewId.LOGIN, navigator.last());
    }
}
