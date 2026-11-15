package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.AppContext;
import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.control.SearchCriteria;
import ca.ucalgary.chinookair.business.entity.Airline;
import ca.ucalgary.chinookair.business.entity.Airport;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.data.DatabaseSeeder;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
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
    void theListsAreFilledAndTheDateIsNotInThePast() {
        assertTrue(view.originCombo.getItemCount() >= 2);
        assertEquals(view.originCombo.getItemCount(), view.destinationCombo.getItemCount());
        // "Any airline" first, then the airlines
        assertNull(view.airlineCombo.getItemAt(0));
        assertTrue(view.airlineCombo.getItemCount() >= 2);
        assertFalse(view.dateField.date().orElseThrow().isBefore(LocalDate.of(2026, 12, 1)));
        assertFalse(view.selectButton.isEnabled());
    }

    @Test
    void theFlightsOfTheStartingRouteShowWithoutPressingSearch() {
        assertTrue(view.tableModel.getRowCount() > 0, "the starting route should have a flight");
        assertEquals("", view.message());
        assertFalse(view.selectButton.isEnabled(), "nothing is chosen yet");
    }

    @Test
    void aRememberedSearchShowsItsFlightsAgainOnTheSameDayOrToday() {
        choose("YVR", "YYZ", "2026-12-03", "WS");
        view.searchButton.doClick(0);
        FlightSearchView next = new FlightSearchView(controllers, navigator, booking);
        next.onShow();
        assertEquals("2026-12-03", next.dateField.getText());
        // a remembered day that has passed starts today instead
        booking.rememberSearch("YVR", "YYZ", null, LocalDate.of(2026, 11, 1));
        FlightSearchView later = new FlightSearchView(controllers, navigator, booking);
        later.onShow();
        assertEquals("2026-12-01", later.dateField.getText());
    }

    @Test
    void swapExchangesTheTwoAirports() {
        choose("YYC", "YVR", "2026-12-02", null);
        view.swapButton.doClick(0);
        assertEquals("YVR", ((Airport) view.originCombo.getSelectedItem()).code());
        assertEquals("YYC", ((Airport) view.destinationCombo.getSelectedItem()).code());
        view.swapButton.doClick(0);
        assertEquals("YYC", ((Airport) view.originCombo.getSelectedItem()).code());
    }

    @Test
    void theNextVisitStartsFromTheLastSearch() {
        choose("YVR", "YYZ", "2026-12-03", "WS");
        view.searchButton.doClick(0);
        FlightSearchView next = new FlightSearchView(controllers, navigator, booking);
        next.onShow();
        assertEquals("YVR", ((Airport) next.originCombo.getSelectedItem()).code());
        assertEquals("YYZ", ((Airport) next.destinationCombo.getSelectedItem()).code());
        assertEquals("WS", ((Airline) next.airlineCombo.getSelectedItem()).code());
    }

    @Test
    void aNoFlightsAnswerIsAHintNotAnError() {
        choose("YEG", "YOW", "2026-12-02", null);
        view.searchButton.doClick(0);
        assertTrue(view.message().startsWith("No flights found"));
        assertEquals(Theme.MUTED, view.messageColor());
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
        int startingRows = view.tableModel.getRowCount();
        choose("YYC", "YVR", "2026-13-40", null);
        view.searchButton.doClick(0);
        assertTrue(view.message().startsWith("Date must look like yyyy-MM-dd"));
        // the flights of the starting route are still shown, nothing was searched again
        assertEquals(startingRows, view.tableModel.getRowCount());
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
    void doubleClickingAFlightSelectsItLikeTheButton() {
        choose("YYC", "YVR", "2026-12-02", null);
        view.searchButton.doClick(0);
        view.table.setRowSelectionInterval(0, 0);
        click(1);
        assertEquals(0, navigator.shown.size());
        click(2);
        assertEquals(ViewId.SEAT_SELECTION, navigator.last());
        assertTrue(booking.selectedOffer().isPresent());
    }

    @Test
    void anEmptyTableSaysWhatToDo() {
        // the starting route has flights, so a search that finds none is what shows the hint
        choose("YEG", "YOW", "2026-12-02", null);
        view.searchButton.doClick(0);
        assertEquals(0, view.tableModel.getRowCount());
        assertEquals("No flights match this search.", view.table.emptyText());
    }

    private void click(int count) {
        view.table.dispatchEvent(new java.awt.event.MouseEvent(view.table, java.awt.event.MouseEvent.MOUSE_CLICKED,
                0L, 0, 5, 5, count, false));
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

    @Test
    void whileAnAgentMovesAReservationTheBookingIsKeptAndBackReturnsToTheModifyScreen() {
        controllers.session().login(controllers.loginController()
                .authenticate(DatabaseSeeder.AGENT_USERNAME, DatabaseSeeder.AGENT_PASSWORD));
        Customer owner = (Customer) controllers.loginController()
                .authenticate(DatabaseSeeder.CUSTOMER_USERNAME, DatabaseSeeder.CUSTOMER_PASSWORD);
        booking.bookFor(owner);
        booking.startModifying(controllers.bookingController().startReservation(owner, owner,
                controllers.flightSearchController().search(new SearchCriteria("YYC", "YVR",
                        LocalDate.of(2026, 12, 2), null)).get(0).flight().id()));
        view.onShow();
        assertTrue(booking.modifying().isPresent());
        assertTrue(booking.customer().isPresent());
        view.backButton.doClick(0);
        assertEquals(ViewId.MODIFY_RESERVATION, navigator.last());
        assertTrue(booking.modifying().isEmpty());
    }
}
