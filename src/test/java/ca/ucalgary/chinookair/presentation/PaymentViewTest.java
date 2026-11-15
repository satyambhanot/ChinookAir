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
import ca.ucalgary.chinookair.business.entity.PaymentMethodType;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.ReservationStatus;
import ca.ucalgary.chinookair.data.DatabaseSeeder;
import java.io.IOException;
import java.nio.file.Files;
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

class PaymentViewTest {

    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    static Path folder;

    private static Controllers controllers;
    private static int day;

    private Customer customer;
    private RecordingNavigator navigator;
    private BookingSession booking;
    private PaymentView view;

    @BeforeAll
    static void startTheProgramParts() {
        controllers = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
    }

    @AfterAll
    static void shutDown() {
        controllers.shutdown().run();
    }

    @BeforeEach
    void openTheScreenWithAReservationWaitingForPayment() {
        customer = (Customer) controllers.loginController()
                .authenticate(DatabaseSeeder.CUSTOMER_USERNAME, DatabaseSeeder.CUSTOMER_PASSWORD);
        controllers.session().login(customer);
        FlightOffer offer = controllers.flightSearchController()
                .search(new SearchCriteria("YYC", "YVR", LocalDate.of(2026, 12, 2).plusDays(day++ % 20), null))
                .get(0);
        Reservation started = controllers.bookingController().startReservation(customer, customer,
                offer.flight().id());
        Reservation waiting = controllers.bookingController().selectSeat(started.id(), "3C");
        navigator = new RecordingNavigator();
        booking = new BookingSession();
        booking.select(offer);
        booking.setReservation(waiting);
        view = new PaymentView(controllers, navigator, booking);
        view.onShow();
    }

    private void fill(String name, String card, String expiry) {
        view.nameField.setText(name);
        view.cardField.setText(card);
        view.expiryField.setText(expiry);
    }

    @Test
    void theAmountIsTheReservationPriceAndCannotBeEdited() {
        String price = "$" + booking.reservation().orElseThrow().price().toPlainString();
        assertEquals(price, view.summaryTotal());
        assertEquals("Pay " + price, view.payButton.getText());
    }

    @Test
    void aGoodCardIsApprovedAndOpensTheConfirmationWithAFileThatHoldsTheNumber() throws IOException {
        fill("Jo Smith", "4111 1111 1111 1111", "12/30");
        view.payButton.doClick(0);
        assertEquals(ViewId.CONFIRMATION, navigator.last());
        PaymentResult result = booking.payment().orElseThrow();
        assertEquals(ReservationStatus.CONFIRMED, booking.reservation().orElseThrow().status());
        assertTrue(Files.readString(result.confirmationFile())
                .contains(result.reservation().confirmationNumber()));
        // the card number is gone from the screen
        assertEquals(0, view.cardField.getPassword().length);
    }

    @Test
    void aDeclinedCardStaysOnTheScreenAndALaterGoodCardWorks() {
        fill("Jo Smith", "4111 1111 1111 0000", "12/30");
        view.payButton.doClick(0);
        assertEquals("Card declined", view.message());
        assertTrue(navigator.shown.isEmpty());
        assertEquals(0, view.cardField.getPassword().length);
        fill("Jo Smith", "4111 1111 1111 1111", "12/30");
        view.payButton.doClick(0);
        assertEquals(ViewId.CONFIRMATION, navigator.last());
    }

    @Test
    void aShortCardNumberIsExplained() {
        fill("Jo Smith", "1234", "12/30");
        view.payButton.doClick(0);
        assertTrue(view.message().contains("16 digits"));
    }

    @Test
    void anExpiredCardIsExplained() {
        fill("Jo Smith", "4111 1111 1111 1111", "01/20");
        view.payButton.doClick(0);
        assertTrue(view.message().contains("expired"));
    }

    @Test
    void anEmptyNameIsExplained() {
        fill("", "4111 1111 1111 1111", "12/30");
        view.payButton.doClick(0);
        assertTrue(view.message().toLowerCase().contains("name"));
        assertTrue(navigator.shown.isEmpty());
    }

    @Test
    void aProblemIsShownUnderTheFieldItIsAbout() {
        fill("Jo Smith", "1234", "12/30");
        view.payButton.doClick(0);
        assertTrue(view.cardRow().hasError());
        assertTrue(view.cardRow().lineText().contains("16 digits"));
        assertFalse(view.expiryRow().hasError());
        assertFalse(view.nameRow().hasError());

        fill("Jo Smith", "4111 1111 1111 1111", "01/20");
        view.payButton.doClick(0);
        assertFalse(view.cardRow().hasError(), "the old error goes away");
        assertTrue(view.expiryRow().hasError());

        fill("", "4111 1111 1111 1111", "12/30");
        view.payButton.doClick(0);
        assertFalse(view.expiryRow().hasError());
        assertTrue(view.nameRow().hasError());
    }

    @Test
    void aDeclinedCardIsNotBlamedOnAField() {
        fill("Jo Smith", "4111 1111 1111 0000", "12/30");
        view.payButton.doClick(0);
        assertFalse(view.cardRow().hasError());
        assertFalse(view.expiryRow().hasError());
        assertFalse(view.nameRow().hasError());
        assertEquals("Card declined", view.message());
    }

    @Test
    void theSummaryNamesThePassengerAndTheSeatClass() {
        java.util.Map<String, String> rows = new java.util.LinkedHashMap<>();
        view.summaryRows().forEach(r -> rows.put(r.label(), r.value()));
        assertEquals(customer.name(), rows.get("Passenger"));
        assertTrue(rows.get("Seat").endsWith("(Economy)") || rows.get("Seat").endsWith("(Business)"), rows.get("Seat"));
    }

    @Test
    void theCardNumberHasAHintAndAnEyeToCheckIt() {
        assertEquals("1234 5678 9012 3456", view.cardField.getClientProperty("JTextField.placeholderText"));
        assertEquals(Boolean.TRUE, view.cardField.getClientProperty("JPasswordField.showRevealButton"));
    }

    @Test
    void theOrderSummaryListsTheTripAndTheTotal() {
        java.util.List<String> labels = view.summaryRows().stream().map(SummaryCard.Row::label).toList();
        assertEquals(java.util.List.of("Passenger", "Flight", "Route", "Departs", "Seat", "Total"), labels);
        assertTrue(view.summaryRows().get(view.summaryRows().size() - 1).total());
    }

    @Test
    void theDebitTypeIsUsedWhenChosen() {
        view.typeCombo.setSelectedItem(PaymentMethodType.DEBIT);
        fill("Jo Smith", "4111 1111 1111 1111", "12/30");
        view.payButton.doClick(0);
        assertEquals(PaymentMethodType.DEBIT, booking.payment().orElseThrow().payment().type());
    }

    @Test
    void backGoesToTheSeatScreen() {
        view.backButton.doClick(0);
        assertEquals(ViewId.SEAT_SELECTION, navigator.last());
    }

    @Test
    void backGoesToTheReservationListWhenThePaymentWasStartedFromThere() {
        BookingSession fromList = new BookingSession();
        fromList.setReservation(booking.reservation().orElseThrow());
        PaymentView other = new PaymentView(controllers, navigator, fromList);
        other.backButton.doClick(0);
        assertEquals(ViewId.MY_RESERVATIONS, navigator.last());
    }

    @Test
    void aPaidReservationCannotBePaidAgain() {
        fill("Jo Smith", "4111 1111 1111 1111", "12/30");
        view.payButton.doClick(0);
        navigator.shown.clear();
        view.onShow();
        assertEquals(ViewId.FLIGHT_SEARCH, navigator.last());
    }

    @Test
    void withNobodyLoggedInTheScreenSendsYouToLogin() {
        controllers.session().logout();
        navigator.shown.clear();
        view.onShow();
        assertEquals(ViewId.LOGIN, navigator.last());
    }

    @Test
    void aNoticeFromAChangeIsShownOnTheScreenOnce() {
        booking.setNotice("The old payment was refunded.");
        view.onShow();
        assertEquals("The old payment was refunded.", view.message());
        view.onShow();
        assertEquals("", view.message());
    }

    @Test
    void theFieldsAreInTheOrderTabVisitsThem() {
        java.util.List<java.awt.Component> order = new java.util.ArrayList<>();
        collectFields(view, order);
        int name = order.indexOf(view.nameField);
        int card = order.indexOf(view.cardField);
        int type = order.indexOf(view.typeCombo);
        int expiry = order.indexOf(view.expiryField);
        assertTrue(name >= 0 && name < card, "name before number");
        assertTrue(card < type, "number before the card type");
        assertTrue(type < expiry, "type before the expiry date");
    }

    private static void collectFields(java.awt.Container root, java.util.List<java.awt.Component> out) {
        for (java.awt.Component c : root.getComponents()) {
            if (c instanceof javax.swing.JTextField || c instanceof javax.swing.JComboBox) {
                out.add(c);
            }
            if (c instanceof java.awt.Container) {
                collectFields((java.awt.Container) c, out);
            }
        }
    }
}
