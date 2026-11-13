package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.AppContext;
import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.control.FlightOffer;
import ca.ucalgary.chinookair.business.control.SearchCriteria;
import ca.ucalgary.chinookair.business.entity.Aircraft;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.PaymentDetails;
import ca.ucalgary.chinookair.business.entity.PaymentMethodType;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.Route;
import ca.ucalgary.chinookair.data.DatabaseSeeder;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlightManagementViewTest {

    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    static Path folder;

    private static Controllers controllers;
    private static int next;

    private int n;
    private RecordingNavigator navigator;
    private FlightManagementView view;
    private List<String> questions;
    private String number;

    @BeforeAll
    static void startTheProgramParts() {
        controllers = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
    }

    @AfterAll
    static void shutDown() {
        controllers.shutdown().run();
    }

    @BeforeEach
    void theAdministratorOpensTheScreenWithAFreshAircraft() {
        n = next++;
        number = "XY" + (9000 + n);
        controllers.session().login(controllers.loginController()
                .authenticate(DatabaseSeeder.ADMIN_USERNAME, DatabaseSeeder.ADMIN_PASSWORD));
        // an aircraft of its own, so no seeded flight is in its way
        controllers.aircraftController().addAircraft(controllers.session().currentUser().orElseThrow(),
                "Test plane " + n, 5, 4);
        navigator = new RecordingNavigator();
        view = new FlightManagementView(controllers, navigator);
        questions = new ArrayList<>();
        view.setConfirmer(q -> {
            questions.add(q);
            return true;
        });
        view.onShow();
        view.aircraftCombo.setSelectedIndex(view.aircraftCombo.getItemCount() - 1);
    }

    private void fill(String flightNumber, String departure, String arrival, String price) {
        view.numberField.setText(flightNumber);
        view.departureField.setText(departure);
        view.arrivalField.setText(arrival);
        view.priceField.setText(price);
    }

    private String day(int offset) {
        return LocalDate.of(2026, 12, 10).plusDays(offset).toString();
    }

    private int rowOf(String flightNumber) {
        for (int row = 0; row < view.tableModel.getRowCount(); row++) {
            if (view.tableModel.getValueAt(row, 0).equals(flightNumber)) {
                return row;
            }
        }
        return -1;
    }

    private void add(String flightNumber) {
        fill(flightNumber, day(n) + " 10:00", day(n) + " 12:00", "199.99");
        view.addButton.doClick(0);
    }

    @Test
    void addingAFlightShowsItInTheTableAndCustomersCanFindIt() {
        add(number);
        assertTrue(view.message().startsWith("Added " + number), view.message());
        assertTrue(rowOf(number) >= 0);
        Aircraft plane = (Aircraft) view.aircraftCombo.getSelectedItem();
        assertEquals(plane.model(), view.tableModel.getValueAt(rowOf(number), 4));
        String origin = ((Route) view.routeCombo.getSelectedItem())
                .origin().code();
        String destination = ((Route) view.routeCombo.getSelectedItem())
                .destination().code();
        List<FlightOffer> found = controllers.flightSearchController()
                .search(new SearchCriteria(origin, destination, LocalDate.parse(day(n)), null));
        assertTrue(found.stream().anyMatch(o -> o.flight().flightNumber().equals(number)));
    }

    @Test
    void theSameFlightNumberTwiceIsRefused() {
        add(number);
        fill(number, day(n + 20) + " 10:00", day(n + 20) + " 12:00", "199.99");
        view.addButton.doClick(0);
        assertEquals("Flight number already exists", view.message());
    }

    @Test
    void anArrivalBeforeTheDepartureIsRefused() {
        fill(number, day(n) + " 12:00", day(n) + " 10:00", "199.99");
        view.addButton.doClick(0);
        assertTrue(view.message().toLowerCase().contains("arrival"), view.message());
        assertEquals(-1, rowOf(number));
    }

    @Test
    void aDepartureInThePastIsRefused() {
        fill(number, "2026-11-30 10:00", "2026-11-30 12:00", "199.99");
        view.addButton.doClick(0);
        assertTrue(view.message().contains("must be in the future"), view.message());
    }

    @Test
    void theSameAircraftAtTheSameTimeIsRefused() {
        add(number);
        fill("XZ" + (9000 + n), day(n) + " 11:00", day(n) + " 13:00", "150.00");
        view.addButton.doClick(0);
        assertTrue(view.message().contains("Aircraft is already assigned"), view.message());
    }

    @Test
    void aBadDateOrPriceIsExplainedBeforeAnythingIsSent() {
        fill(number, "tomorrow", day(n) + " 12:00", "199.99");
        view.addButton.doClick(0);
        assertTrue(view.message().startsWith("Date and time must look like"), view.message());
        fill(number, day(n) + " 10:00", day(n) + " 12:00", "abc");
        view.addButton.doClick(0);
        assertTrue(view.message().startsWith("Base price must be a number"), view.message());
    }

    @Test
    void choosingARowFillsTheFormAndTheOtherButtonsWakeUp() {
        add(number);
        assertFalse(view.updateButton.isEnabled());
        assertFalse(view.removeButton.isEnabled());
        view.numberField.setText("");
        view.table.setRowSelectionInterval(rowOf(number), rowOf(number));
        assertEquals(number, view.numberField.getText());
        assertEquals(day(n) + " 10:00", view.departureField.getText());
        assertEquals("199.99", view.priceField.getText());
        assertTrue(view.updateButton.isEnabled());
        assertTrue(view.removeButton.isEnabled());
    }

    @Test
    void updatingChangesThePriceAndTheTimes() {
        add(number);
        view.table.setRowSelectionInterval(rowOf(number), rowOf(number));
        view.priceField.setText("250.00");
        view.departureField.setText(day(n) + " 09:00");
        view.updateButton.doClick(0);
        assertEquals("Updated " + number, view.message());
        assertEquals(day(n) + " 09:00", view.tableModel.getValueAt(rowOf(number), 2));
        view.table.setRowSelectionInterval(rowOf(number), rowOf(number));
        assertEquals("250.00", view.priceField.getText());
    }

    @Test
    void removingAFlightWithoutReservationsAsksAndThenItDisappears() {
        add(number);
        view.table.setRowSelectionInterval(rowOf(number), rowOf(number));
        view.removeButton.doClick(0);
        assertEquals("Remove flight " + number + "?", questions.get(0));
        assertEquals(-1, rowOf(number));
        assertEquals("Removed " + number, view.message());
    }

    @Test
    void answeringNoKeepsTheFlight() {
        add(number);
        view.setConfirmer(q -> false);
        view.table.setRowSelectionInterval(rowOf(number), rowOf(number));
        view.removeButton.doClick(0);
        assertTrue(rowOf(number) >= 0);
    }

    @Test
    void aFlightWithAnActiveReservationCannotBeRemoved() {
        add(number);
        Customer customer = controllers.customerController().register("fm" + n, "password1", "Fm Customer",
                "fm" + n + "@x.ca", null);
        long flightId = view.tableModel.flightAt(rowOf(number)).id();
        Reservation waiting = controllers.bookingController().selectSeat(controllers.bookingController()
                .startReservation(customer, customer, flightId).id(), "3C");
        controllers.paymentController().pay(customer, waiting.id(), PaymentMethodType.CREDIT,
                new PaymentDetails("Jo Smith", "4111 1111 1111 1111", "12/30"));
        view.table.setRowSelectionInterval(rowOf(number), rowOf(number));
        view.removeButton.doClick(0);
        assertTrue(view.message().contains("active reservations"), view.message());
        assertTrue(rowOf(number) >= 0);
    }

    @Test
    void anAgentAndACustomerCannotReachTheScreen() {
        controllers.session().login(controllers.loginController()
                .authenticate(DatabaseSeeder.AGENT_USERNAME, DatabaseSeeder.AGENT_PASSWORD));
        view.onShow();
        assertEquals(ViewId.LOGIN, navigator.last());
        navigator.shown.clear();
        controllers.session().login(controllers.loginController()
                .authenticate(DatabaseSeeder.CUSTOMER_USERNAME, DatabaseSeeder.CUSTOMER_PASSWORD));
        view.onShow();
        assertEquals(ViewId.LOGIN, navigator.last());
    }

    @Test
    void backGoesToTheAdministratorDashboard() {
        view.backButton.doClick(0);
        assertEquals(ViewId.ADMIN_DASHBOARD, navigator.last());
    }
}
