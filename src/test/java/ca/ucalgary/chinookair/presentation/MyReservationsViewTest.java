package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.AppContext;
import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.control.FlightOffer;
import ca.ucalgary.chinookair.business.control.SearchCriteria;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.data.DatabaseSeeder;
import ca.ucalgary.chinookair.business.entity.PaymentDetails;
import ca.ucalgary.chinookair.business.entity.PaymentMethodType;
import ca.ucalgary.chinookair.business.entity.PaymentStatus;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.ReservationStatus;
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

class MyReservationsViewTest {

    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    static Path folder;

    private static Controllers controllers;
    private static int next;

    private Customer customer;
    private FlightOffer offer;
    private RecordingNavigator navigator;
    private BookingSession booking;
    private MyReservationsView view;
    private List<String> questions;

    @BeforeAll
    static void startTheProgramParts() {
        controllers = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
    }

    @AfterAll
    static void shutDown() {
        controllers.shutdown().run();
    }

    /** A new customer for every test, so each one starts with an empty list. */
    @BeforeEach
    void aNewCustomerOpensTheScreen() {
        int n = next++;
        customer = controllers.customerController().register("flyer" + n, "password1", "Flyer " + n,
                "flyer" + n + "@example.com", "4035550100");
        controllers.session().login(customer);
        offer = controllers.flightSearchController()
                .search(new SearchCriteria("YYC", "YVR", LocalDate.of(2026, 12, 2).plusDays(n % 20), null)).get(0);
        navigator = new RecordingNavigator();
        booking = new BookingSession();
        view = new MyReservationsView(controllers, navigator, booking);
        questions = new ArrayList<>();
        view.setConfirmer(q -> {
            questions.add(q);
            return true;
        });
    }

    private Reservation book(String seat, boolean pay) {
        Reservation started = controllers.bookingController().startReservation(customer, customer,
                offer.flight().id());
        Reservation waiting = controllers.bookingController().selectSeat(customer, started.id(), seat);
        if (!pay) {
            return waiting;
        }
        return controllers.paymentController().pay(customer, waiting.id(), PaymentMethodType.CREDIT,
                new PaymentDetails("Jo Smith", "4111 1111 1111 1111", "12/30")).reservation();
    }

    @Test
    void aNewCustomerSeesAnEmptyStateWithAWayToBook() {
        view.onShow();
        assertEquals(0, view.cards().size());
        assertEquals("No reservations yet", view.emptyState().title());
        view.emptyState().action().doClick(0);
        assertEquals(ViewId.FLIGHT_SEARCH, navigator.last());
    }

    @Test
    void aConfirmedReservationIsOneCardThatOffersOnlyCancel() {
        Reservation r = book("1A", true);
        view.onShow();
        assertEquals(1, view.cards().size());
        ReservationCard card = view.cardFor(r.confirmationNumber());
        assertTrue(card.cancelButton.isVisible());
        assertFalse(card.payButton.isVisible(), "nothing to pay");
        assertEquals(List.of("Upcoming (1)"), view.sectionTitles());
    }

    @Test
    void aReservationWaitingForPaymentOffersPayAndCancel() {
        Reservation waiting = book("2B", false);
        view.onShow();
        ReservationCard card = view.cardFor(waiting.confirmationNumber());
        assertTrue(card.payButton.isVisible());
        assertTrue(card.cancelButton.isVisible());
    }

    @Test
    void cancellingAfterYesRefundsAndTheCardMovesToCancelled() {
        Reservation r = book("1A", true);
        view.onShow();
        view.cardFor(r.confirmationNumber()).cancelButton.doClick(0);
        assertEquals("Cancel reservation " + r.confirmationNumber() + "? You will be refunded $"
                + r.price().toPlainString() + ".", questions.get(0));
        assertEquals(List.of("Cancelled (1)"), view.sectionTitles());
        assertFalse(view.cardFor(r.confirmationNumber()).cancelButton.isVisible());
        assertEquals(PaymentStatus.REFUNDED,
                controllers.paymentController().recordsForCustomer(customer, customer.id()).get(0).status());
        assertTrue(controllers.bookingController().availableSeats(offer.flight().id()).stream()
                .anyMatch(s -> s.number().equals("1A")));
    }

    @Test
    void answeringNoLeavesTheReservationAsItWas() {
        Reservation r = book("1A", true);
        view.setConfirmer(q -> false);
        view.onShow();
        view.cardFor(r.confirmationNumber()).cancelButton.doClick(0);
        assertEquals(List.of("Upcoming (1)"), view.sectionTitles());
        assertEquals(Status.of(ReservationStatus.CONFIRMED),
                Status.of(view.cardFor(r.confirmationNumber()).reservation().status()));
    }

    @Test
    void aReservationWaitingForPaymentCanBePaidFromItsCard() {
        Reservation waiting = book("2B", false);
        view.onShow();
        view.cardFor(waiting.confirmationNumber()).payButton.doClick(0);
        assertEquals(ViewId.PAYMENT, navigator.last());
        assertEquals(waiting.id(), booking.reservation().orElseThrow().id());
        assertEquals(ReservationStatus.PENDING_PAYMENT, booking.reservation().orElseThrow().status());
    }

    @Test
    void cancellingAPendingReservationDoesNotTalkAboutARefund() {
        Reservation waiting = book("2B", false);
        view.onShow();
        view.cardFor(waiting.confirmationNumber()).cancelButton.doClick(0);
        assertEquals("Cancel reservation " + waiting.confirmationNumber() + "?", questions.get(0));
    }

    @Test
    void theListIsInSectionsWithWhatIsAheadFirst() {
        Reservation keep = book("3A", true);
        Reservation drop = book("3B", true);
        view.onShow();
        view.cardFor(drop.confirmationNumber()).cancelButton.doClick(0);
        assertEquals(List.of("Upcoming (1)", "Cancelled (1)"), view.sectionTitles());
        assertEquals(keep.confirmationNumber(), view.cards().get(0).reservation().confirmationNumber());
        assertEquals(drop.confirmationNumber(), view.cards().get(1).reservation().confirmationNumber());
    }

    @Test
    void typingNarrowsTheListToTheMatchingReservations() {
        Reservation first = book("4A", true);
        Reservation second = book("4B", true);
        view.onShow();
        assertEquals(2, view.cards().size());
        view.filterField.setText(first.confirmationNumber().toLowerCase());
        assertEquals(1, view.cards().size());
        assertEquals(first.confirmationNumber(), view.cards().get(0).reservation().confirmationNumber());
        view.filterField.setText("zzzz");
        assertEquals(0, view.cards().size());
        assertEquals("No reservation matches", view.emptyState().title());
        view.filterField.setText("");
        assertEquals(2, view.cards().size());
        assertTrue(second.id() > 0);
    }

    @Test
    void anAgentChoosesTheCustomerAndSeesTheirReservations() {
        book("1A", true);
        controllers.session().login(controllers.loginController().authenticate("agent1", "agent123"));
        booking.bookFor(customer);
        view.onShow();
        assertEquals(1, view.cards().size());
    }

    @Test
    void anAgentWithoutAChosenCustomerIsToldToChooseOne() {
        controllers.session().login(controllers.loginController().authenticate("agent1", "agent123"));
        view.onShow();
        assertEquals("Choose a customer first", view.message());
        assertEquals(0, view.cards().size());
    }

    @Test
    void aCustomerHasNoBreadcrumbBecauseTheTabsTakeThemAnywhere() {
        view.onShow();
        assertFalse(view.breadcrumb.isVisible());
        assertEquals(List.of(), view.breadcrumb.texts());
    }

    @Test
    void anAgentSeesTheWayBackToTheCustomers() {
        controllers.session().login(controllers.loginController()
                .authenticate(DatabaseSeeder.AGENT_USERNAME, DatabaseSeeder.AGENT_PASSWORD));
        booking.bookFor(customer);
        view.onShow();
        assertTrue(view.breadcrumb.isVisible());
        assertEquals(List.of("Customers", customer.name(), "Reservations"), view.breadcrumb.texts());
        view.breadcrumb.links().get(0).doClick(0);
        assertEquals(ViewId.CUSTOMER_MANAGEMENT, navigator.last());
    }

    @Test
    void withNobodyLoggedInTheScreenSendsYouToLogin() {
        controllers.session().logout();
        view.onShow();
        assertEquals(ViewId.LOGIN, navigator.last());
    }
}
