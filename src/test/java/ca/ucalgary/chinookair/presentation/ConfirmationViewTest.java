package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.AppContext;
import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.control.FlightOffer;
import ca.ucalgary.chinookair.business.control.PaymentResult;
import ca.ucalgary.chinookair.business.control.SearchCriteria;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.PaymentDetails;
import ca.ucalgary.chinookair.business.entity.PaymentMethodType;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.data.DatabaseSeeder;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfirmationViewTest {

    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    static Path folder;

    private static Controllers controllers;
    private static int day;

    private Customer customer;
    private PaymentResult result;
    private RecordingNavigator navigator;
    private BookingSession booking;
    private ConfirmationView view;

    @BeforeAll
    static void startTheProgramParts() {
        controllers = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
    }

    @AfterAll
    static void shutDown() {
        controllers.shutdown().run();
    }

    @BeforeEach
    void payForAReservation() {
        customer = (Customer) controllers.loginController()
                .authenticate(DatabaseSeeder.CUSTOMER_USERNAME, DatabaseSeeder.CUSTOMER_PASSWORD);
        controllers.session().login(customer);
        FlightOffer offer = controllers.flightSearchController()
                .search(new SearchCriteria("YYC", "YVR", LocalDate.of(2026, 12, 2).plusDays(day++ % 20), null)).get(0);
        Reservation started = controllers.bookingController().startReservation(customer, customer,
                offer.flight().id());
        Reservation waiting = controllers.bookingController().selectSeat(customer, started.id(), "1A");
        result = controllers.paymentController().pay(customer, waiting.id(), PaymentMethodType.CREDIT,
                new PaymentDetails("Jo Smith", "4111 1111 1111 1111", "12/30"));
        navigator = new RecordingNavigator();
        booking = new BookingSession();
        booking.setReservation(result.reservation());
        booking.setPayment(result);
        view = new ConfirmationView(controllers, navigator, booking);
        view.onShow();
    }

    private String row(String label) {
        return view.tripRows().stream().filter(r -> r.label().equals(label)).findFirst().orElseThrow().value();
    }

    @Test
    void itShowsTheNumberTheTripAndWhereTheFileIsSaved() {
        assertEquals(result.reservation().confirmationNumber(), view.numberText());
        assertEquals(customer.name(), row("Passenger"));
        assertTrue(row("Flight").startsWith(result.reservation().flight().flightNumber()));
        assertEquals("YYC → YVR", row("Route"));
        assertEquals("1A (Business)", row("Seat"));
        assertTrue(row("Amount paid").startsWith("$"));
        assertTrue(view.savedText().contains(result.confirmationFile().getFileName().toString()));
        assertTrue(view.subtitleText().contains(customer.name()));
    }

    @Test
    void noRawEnumNamesOrFullPathsAreShown() {
        for (SummaryCard.Row row : view.tripRows()) {
            assertFalse(row.value().contains("_"), row.label());
        }
        assertFalse(view.savedText().contains(result.confirmationFile().getParent().toString()));
    }

    @Test
    void copyPutsTheNumberOnTheClipboardAndSaysSo() {
        List<String> copied = new ArrayList<>();
        view.setClipboard(copied::add);
        view.copyButton.doClick(0);
        assertEquals(List.of(result.reservation().confirmationNumber()), copied);
        assertEquals("Copied", view.copyButton.getText());
    }

    @Test
    void openFolderOpensTheFolderOfTheSavedFile() {
        List<Path> opened = new ArrayList<>();
        view.setFolderOpener(opened::add);
        assertTrue(view.folderButton.isVisible());
        view.folderButton.doClick(0);
        assertEquals(List.of(result.confirmationFile().getParent()), opened);
    }

    @Test
    void bookAnotherEndsThisBookingAndStartsANewSearch() {
        view.anotherButton.doClick(0);
        assertEquals(ViewId.FLIGHT_SEARCH, navigator.last());
        assertTrue(booking.payment().isEmpty());
    }

    @Test
    void theDashboardButtonEndsTheBookingAndGoesHome() {
        view.dashboardButton.doClick(0);
        assertEquals(ViewId.CUSTOMER_DASHBOARD, navigator.last());
        assertTrue(booking.payment().isEmpty());
        assertTrue(booking.reservation().isEmpty());
    }

    @Test
    void withoutAPaymentTheScreenSendsYouToTheSearch() {
        booking.clear();
        navigator.shown.clear();
        view.onShow();
        assertEquals(ViewId.FLIGHT_SEARCH, navigator.last());
    }
}
