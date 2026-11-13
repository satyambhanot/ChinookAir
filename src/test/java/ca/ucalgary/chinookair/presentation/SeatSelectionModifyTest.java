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
import ca.ucalgary.chinookair.business.entity.PaymentStatus;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.ReservationStatus;
import ca.ucalgary.chinookair.data.DatabaseSeeder;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The seat screen when an agent picks the new seat of a reservation instead of booking. */
class SeatSelectionModifyTest {

    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    static Path folder;

    private static Controllers controllers;
    private static int next;

    private int n;
    private Customer customer;
    private Reservation confirmed;
    private RecordingNavigator navigator;
    private BookingSession booking;
    private SeatSelectionView view;

    @BeforeAll
    static void startTheProgramParts() {
        controllers = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
    }

    @AfterAll
    static void shutDown() {
        controllers.shutdown().run();
    }

    private FlightOffer firstFlight(String from, String to, int dayOffset) {
        for (int d = dayOffset; d < dayOffset + 12; d++) {
            List<FlightOffer> found = controllers.flightSearchController()
                    .search(new SearchCriteria(from, to, LocalDate.of(2026, 12, 2).plusDays(d), null));
            if (!found.isEmpty()) {
                return found.get(0);
            }
        }
        throw new IllegalStateException("no flight " + from + " to " + to);
    }

    @BeforeEach
    void aConfirmedReservationAndTheAgentOnTheSeatScreenInModifyMode() {
        n = next++;
        customer = controllers.customerController().register("mv" + n, "password1", "Move Er " + n,
                "mv" + n + "@x.ca", null);
        FlightOffer first = firstFlight("YYC", "YVR", n % 7);
        Reservation waiting = controllers.bookingController().selectSeat(controllers.bookingController()
                .startReservation(customer, customer, first.flight().id()).id(), "3C");
        confirmed = controllers.paymentController().pay(customer, waiting.id(), PaymentMethodType.CREDIT,
                new PaymentDetails("Jo Smith", "4111 1111 1111 1111", "12/30")).reservation();
        controllers.session().login(controllers.loginController()
                .authenticate(DatabaseSeeder.AGENT_USERNAME, DatabaseSeeder.AGENT_PASSWORD));
        navigator = new RecordingNavigator();
        booking = new BookingSession();
        booking.bookFor(customer);
        booking.startModifying(confirmed);
    }

    /** Another YYC to YVR flight a week later: the same price, because prices repeat every 7 days. */
    private FlightOffer sameCostFlight() {
        return firstFlight("YYC", "YVR", n % 7 + 7);
    }

    private void openOn(FlightOffer target) {
        booking.select(target);
        view = new SeatSelectionView(controllers, navigator, booking);
        view.onShow();
    }

    private List<Reservation> history() {
        return controllers.reservationController().history(customer, customer.id());
    }

    @Test
    void openingItCreatesNoNewReservation() {
        openOn(sameCostFlight());
        assertEquals(1, history().size());
        assertEquals("Confirm change", view.continueButton.getText());
        assertTrue(booking.reservation().isEmpty());
    }

    @Test
    void aChangeToTheSamePriceKeepsItConfirmedAndGoesBackToTheList() {
        FlightOffer other = sameCostFlight();
        openOn(other);
        view.seatButtonFor("4A").doClick(0);
        view.continueButton.doClick(0);
        assertEquals(ViewId.MODIFY_RESERVATION, navigator.last());
        Reservation now = history().get(0);
        assertEquals(ReservationStatus.CONFIRMED, now.status());
        assertEquals("4A", now.seatNo());
        assertEquals(other.flight().id(), now.flight().id());
        assertTrue(booking.modifying().isEmpty());
        assertTrue(booking.takeNotice().contains("4A"));
    }

    @Test
    void aChangeToADifferentPriceRefundsTheOldPaymentAndOpensPayment() {
        FlightOffer dearer = firstFlight("YYZ", "YUL", 0);
        openOn(dearer);
        view.seatButtonFor("4A").doClick(0);
        view.continueButton.doClick(0);
        Reservation now = history().get(0);
        assertEquals(ReservationStatus.PENDING_PAYMENT, now.status());
        assertEquals(ViewId.PAYMENT, navigator.last());
        assertEquals(now.id(), booking.reservation().orElseThrow().id());
        assertTrue(booking.takeNotice().contains("refunded"));
        assertEquals(PaymentStatus.REFUNDED,
                controllers.paymentController().recordsForCustomer(customer, customer.id()).get(0).status());
    }

    @Test
    void aSeatTakenInTheMeantimeShowsTheMessageAndChangesNothing() {
        FlightOffer other = sameCostFlight();
        openOn(other);
        view.seatButtonFor("4A").doClick(0);
        Customer racer = controllers.customerController().register("racer" + next, "password1", "Racer",
                "racer" + next + "@x.ca", null);
        controllers.bookingController().selectSeat(controllers.bookingController()
                .startReservation(racer, racer, other.flight().id()).id(), "4A");
        view.continueButton.doClick(0);
        assertEquals(ErrorMessages.SEAT_TAKEN, view.message());
        assertFalse(view.seatButtonFor("4A").isEnabled());
        assertEquals("3C", history().get(0).seatNo());
        assertEquals(ReservationStatus.CONFIRMED, history().get(0).status());
    }

    @Test
    void backReturnsToTheSearchAndKeepsTheReservation() {
        openOn(sameCostFlight());
        view.backButton.doClick(0);
        assertEquals(ViewId.FLIGHT_SEARCH, navigator.last());
        assertEquals(ReservationStatus.CONFIRMED, history().get(0).status());
        assertTrue(booking.modifying().isPresent());
    }
}
