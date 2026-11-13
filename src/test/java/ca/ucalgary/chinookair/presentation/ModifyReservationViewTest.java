package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.AppContext;
import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.control.FlightOffer;
import ca.ucalgary.chinookair.business.control.SearchCriteria;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.PaymentDetails;
import ca.ucalgary.chinookair.business.entity.PaymentMethodType;
import ca.ucalgary.chinookair.business.entity.Reservation;
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

class ModifyReservationViewTest {

    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    static Path folder;

    private static Controllers controllers;
    private static int next;

    private Customer customer;
    private Reservation confirmed;
    private RecordingNavigator navigator;
    private BookingSession booking;
    private ModifyReservationView view;

    @BeforeAll
    static void startTheProgramParts() {
        controllers = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
    }

    @AfterAll
    static void shutDown() {
        controllers.shutdown().run();
    }

    @BeforeEach
    void anAgentOpensTheScreenForACustomerWithAConfirmedReservation() {
        int n = next++;
        customer = controllers.customerController().register("mod" + n, "password1", "Mo Dify " + n,
                "mod" + n + "@x.ca", null);
        FlightOffer offer = controllers.flightSearchController()
                .search(new SearchCriteria("YYC", "YVR", LocalDate.of(2026, 12, 2).plusDays(n % 20), null)).get(0);
        Reservation waiting = controllers.bookingController().selectSeat(controllers.bookingController()
                .startReservation(customer, customer, offer.flight().id()).id(), "3C");
        confirmed = controllers.paymentController().pay(customer, waiting.id(), PaymentMethodType.CREDIT,
                new PaymentDetails("Jo Smith", "4111 1111 1111 1111", "12/30")).reservation();
        controllers.session().login(controllers.loginController()
                .authenticate(DatabaseSeeder.AGENT_USERNAME, DatabaseSeeder.AGENT_PASSWORD));
        navigator = new RecordingNavigator();
        booking = new BookingSession();
        booking.bookFor(customer);
        view = new ModifyReservationView(controllers, navigator, booking);
        view.onShow();
    }

    @Test
    void itListsTheReservationsOfTheChosenCustomer() {
        assertEquals(1, view.tableModel.getRowCount());
        assertEquals(confirmed.confirmationNumber(), view.tableModel.getValueAt(0, 0));
        assertFalse(view.modifyButton.isEnabled());
    }

    @Test
    void modifyStartsTheChangeAndOpensTheFlightSearch() {
        view.table.setRowSelectionInterval(0, 0);
        assertTrue(view.modifyButton.isEnabled());
        view.modifyButton.doClick(0);
        assertEquals(ViewId.FLIGHT_SEARCH, navigator.last());
        assertEquals(confirmed.id(), booking.modifying().orElseThrow().id());
    }

    @Test
    void aCancelledReservationCannotBeChosenForModify() {
        controllers.reservationController().cancel(customer, confirmed.id());
        view.onShow();
        view.table.setRowSelectionInterval(0, 0);
        assertFalse(view.modifyButton.isEnabled());
    }

    @Test
    void aNoticeFromAChangeIsShownOnce() {
        booking.setNotice("Done");
        view.onShow();
        assertEquals("Done", view.message());
        view.onShow();
        assertEquals("", view.message());
    }

    @Test
    void withoutAChosenCustomerTheAgentIsToldToChooseOne() {
        booking.clear();
        view.onShow();
        assertEquals("Choose a customer first", view.message());
        assertEquals(0, view.tableModel.getRowCount());
    }

    @Test
    void aCustomerCannotReachTheScreen() {
        controllers.session().login(customer);
        navigator.shown.clear();
        view.onShow();
        assertEquals(ViewId.LOGIN, navigator.last());
    }

    @Test
    void backGoesToTheCustomerList() {
        view.backButton.doClick(0);
        assertEquals(ViewId.CUSTOMER_MANAGEMENT, navigator.last());
    }
}
