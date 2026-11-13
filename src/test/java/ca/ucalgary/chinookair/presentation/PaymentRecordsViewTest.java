package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class PaymentRecordsViewTest {

    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    static Path folder;

    private static Controllers controllers;
    private static int next;

    private Customer payer;
    private RecordingNavigator navigator;
    private PaymentRecordsView view;

    @BeforeAll
    static void startTheProgramParts() {
        controllers = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
    }

    @AfterAll
    static void shutDown() {
        controllers.shutdown().run();
    }

    @BeforeEach
    void aCustomerWithADeclinedAndAnApprovedPayment() {
        int n = next++;
        payer = controllers.customerController().register("payer" + n, "password1", "Pay Er " + n,
                "payer" + n + "@x.ca", null);
        FlightOffer offer = controllers.flightSearchController()
                .search(new SearchCriteria("YYC", "YVR", LocalDate.of(2026, 12, 2).plusDays(n % 20), null)).get(0);
        Reservation waiting = controllers.bookingController().selectSeat(controllers.bookingController()
                .startReservation(payer, payer, offer.flight().id()).id(), "3C");
        controllers.paymentController().pay(payer, waiting.id(), PaymentMethodType.CREDIT,
                new PaymentDetails("Jo Smith", "4111 1111 1111 0000", "12/30"));
        controllers.paymentController().pay(payer, waiting.id(), PaymentMethodType.CREDIT,
                new PaymentDetails("Jo Smith", "4111 1111 1111 1111", "12/30"));
        controllers.session().login(controllers.loginController()
                .authenticate(DatabaseSeeder.AGENT_USERNAME, DatabaseSeeder.AGENT_PASSWORD));
        navigator = new RecordingNavigator();
        view = new PaymentRecordsView(controllers, navigator);
        view.onShow();
    }

    private void search(String text) {
        view.searchField.setText(text);
        view.searchButton.doClick(0);
    }

    @Test
    void choosingTheCustomerShowsTheirTwoPaymentsNewestFirst() {
        search(payer.email());
        assertEquals(1, view.customerModel.getRowCount());
        view.customerTable.setRowSelectionInterval(0, 0);
        assertEquals(2, view.paymentModel.getRowCount());
        assertEquals("APPROVED", view.paymentModel.getValueAt(0, 4));
        assertEquals("DECLINED", view.paymentModel.getValueAt(1, 4));
        assertEquals("**** **** **** 1111", view.paymentModel.getValueAt(0, 5));
    }

    @Test
    void aCustomerWithoutPaymentsShowsAMessage() {
        Customer none = controllers.customerController().register("nopay" + next, "password1", "No Pay",
                "nopay" + next + "@x.ca", null);
        search(none.email());
        view.customerTable.setRowSelectionInterval(0, 0);
        assertEquals(0, view.paymentModel.getRowCount());
        assertEquals("No Pay has no payments.", view.message());
    }

    @Test
    void searchingForNobodyShowsAnEmptyList() {
        search("zzzzqq");
        assertEquals(0, view.customerModel.getRowCount());
        assertEquals("No customers found.", view.message());
    }

    @Test
    void aCustomerIsRefusedTheSearch() {
        controllers.session().login(payer);
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
        assertTrue(view.paymentModel.getRowCount() >= 0);
    }
}
