package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.AppContext;
import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.control.FlightOffer;
import ca.ucalgary.chinookair.business.control.SearchCriteria;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.ReservationStatus;
import ca.ucalgary.chinookair.business.entity.SeatClass;
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

class SeatSelectionViewTest {

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
    private SeatSelectionView view;

    @BeforeAll
    static void startTheProgramParts() {
        controllers = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
    }

    @AfterAll
    static void shutDown() {
        controllers.shutdown().run();
    }

    @BeforeEach
    void openTheScreenOnAFreshFlight() {
        customer = (Customer) controllers.loginController()
                .authenticate(DatabaseSeeder.CUSTOMER_USERNAME, DatabaseSeeder.CUSTOMER_PASSWORD);
        controllers.session().login(customer);
        // every test takes another flight, so seats taken by one test do not disturb the next
        List<FlightOffer> flights = controllers.flightSearchController()
                .search(new SearchCriteria("YYC", "YVR", LocalDate.of(2026, 12, 2).plusDays(next++ % 20), null));
        offer = flights.get(0);
        navigator = new RecordingNavigator();
        booking = new BookingSession();
        booking.select(offer);
        view = new SeatSelectionView(controllers, navigator, booking);
        view.onShow();
    }

    private ReservationStatus statusOf(Reservation reservation) {
        return controllers.reservationController().history(customer, customer.id()).stream()
                .filter(r -> r.id().equals(reservation.id())).findFirst().orElseThrow().status();
    }

    @Test
    void openingTheScreenStartsAReservationAndDrawsTheMap() {
        Reservation reservation = booking.reservation().orElseThrow();
        assertEquals(ReservationStatus.CREATED, statusOf(reservation));
        assertTrue(view.seatButtonFor("1A").isEnabled());
        assertFalse(view.continueButton.isEnabled());
    }

    @Test
    void aBusinessSeatShowsTheBusinessPriceAndAnEconomySeatTheEconomyPrice() {
        view.seatButtonFor("1A").doClick(0);
        assertEquals("Seat 1A (Business) - $" + offer.flight().priceFor(SeatClass.BUSINESS).toPlainString(),
                view.chosenText());
        assertTrue(view.continueButton.isEnabled());
        view.seatButtonFor("3C").doClick(0);
        assertEquals("Seat 3C (Economy) - $" + offer.fromPrice().toPlainString(), view.chosenText());
    }

    @Test
    void theCardOnTheRightFollowsTheChosenSeat() {
        assertEquals("None yet", view.summaryRows().get(0).value());
        view.seatButtonFor("3C").doClick(0);
        java.util.Map<String, String> rows = new java.util.LinkedHashMap<>();
        view.summaryRows().forEach(r -> rows.put(r.label(), r.value()));
        assertEquals(offer.flight().flightNumber(), rows.get("Flight"));
        assertEquals("3C (Economy)", rows.get("Seat"));
        assertEquals("$" + offer.fromPrice().toPlainString(), rows.get("Price"));
        view.seatButtonFor("1A").doClick(0);
        assertEquals("1A (Business)", view.summaryRows().stream().filter(r -> r.label().equals("Seat"))
                .findFirst().orElseThrow().value());
    }

    @Test
    void aTakenSeatShowsACrossAndIsNamedTakenForAScreenReader() {
        view.seatButtonFor("1A").doClick(0);
        Customer other = controllers.customerController()
                .register("taker" + next, "password1", "Taker", "taker" + next + "@example.com", "4035550101");
        long theirs = controllers.bookingController().startReservation(other, other, offer.flight().id()).id();
        controllers.bookingController().selectSeat(theirs, "1A");
        view.continueButton.doClick(0);
        assertEquals("\u00D7", view.seatButtonFor("1A").getText());
        assertTrue(view.seatButtonFor("1A").getAccessibleContext().getAccessibleName().contains("taken"));
        assertEquals("3C", view.seatButtonFor("3C").getText());
    }

    @Test
    void theCardNamesThePassenger() {
        view.seatButtonFor("3C").doClick(0);
        assertEquals(customer.name(), view.summaryRows().stream().filter(r -> r.label().equals("Passenger"))
                .findFirst().orElseThrow().value());
    }

    @Test
    void everySeatHasATooltipWithItsClassAndPrice() {
        String economy = view.seatButtonFor("3C").getToolTipText();
        assertTrue(economy.contains("Economy") && economy.contains("$" + offer.fromPrice().toPlainString()), economy);
        String business = view.seatButtonFor("1A").getToolTipText();
        assertTrue(business.contains("Business"), business);
        assertTrue(view.seatButtonFor("1A").getAccessibleContext().getAccessibleName().contains("$"));
    }

    @Test
    void eachCabinHasAHeadingOverItsFirstRow() {
        List<String> headings = new java.util.ArrayList<>();
        collectLabels(view, headings);
        assertEquals(1, headings.stream().filter(t -> t.startsWith("Business \u00b7 rows 1 to 2")).count(),
                headings.toString());
        assertEquals(1, headings.stream().filter(t -> t.startsWith("Economy \u00b7 rows 3 to")).count(),
                headings.toString());
    }

    private static void collectLabels(java.awt.Container root, List<String> out) {
        for (java.awt.Component c : root.getComponents()) {
            if (c instanceof javax.swing.JLabel && ((javax.swing.JLabel) c).getText() != null) {
                out.add(((javax.swing.JLabel) c).getText());
            }
            if (c instanceof java.awt.Container) {
                collectLabels((java.awt.Container) c, out);
            }
        }
    }

    @Test
    void theWayOnSitsRightUnderTheSummaryCard() {
        view.setSize(1100, 640);
        layoutAll(view);
        int cardBottom = view.summaryHolder().getY() + view.summaryHolder().getHeight();
        java.awt.Point onScreen = javax.swing.SwingUtilities.convertPoint(view.continueButton.getParent(), 0,
                view.continueButton.getY(), view.summaryHolder().getParent());
        assertTrue(onScreen.y - cardBottom < 120, "the button is " + (onScreen.y - cardBottom) + " px below the card");
    }

    private static void layoutAll(java.awt.Container c) {
        c.doLayout();
        for (java.awt.Component k : c.getComponents()) {
            if (k instanceof java.awt.Container) {
                layoutAll((java.awt.Container) k);
            }
        }
    }

    @Test
    void theMapHasAnAisleInTheMiddleOfEachRow() {
        assertEquals(3, view.grid().aisleAfter());
    }

    @Test
    void continueGivesTheReservationTheSeatAndOpensPayment() {
        view.seatButtonFor("3C").doClick(0);
        view.continueButton.doClick(0);
        assertEquals(ViewId.PAYMENT, navigator.last());
        Reservation reservation = booking.reservation().orElseThrow();
        assertEquals("3C", reservation.seatNo());
        assertEquals(ReservationStatus.PENDING_PAYMENT, statusOf(reservation));
    }

    @Test
    void whenSomeoneTakesTheSeatFirstTheMessageShowsAndTheSeatTurnsGrey() {
        view.seatButtonFor("1A").doClick(0);
        // another customer gets 1A while this screen is open
        Customer other = controllers.customerController()
                .register("racer" + next, "password1", "Racer", "racer" + next + "@example.com", "4035550100");
        long theirs = controllers.bookingController().startReservation(other, other, offer.flight().id()).id();
        controllers.bookingController().selectSeat(theirs, "1A");

        view.continueButton.doClick(0);

        assertEquals(ErrorMessages.SEAT_TAKEN, view.message());
        assertFalse(view.seatButtonFor("1A").isEnabled());
        assertFalse(view.continueButton.isEnabled());
        assertTrue(navigator.shown.isEmpty());
    }

    @Test
    void backGivesTheReservationUpAndGoesToTheSearch() {
        Reservation reservation = booking.reservation().orElseThrow();
        view.backButton.doClick(0);
        assertEquals(ViewId.FLIGHT_SEARCH, navigator.last());
        assertEquals(ReservationStatus.CANCELLED, statusOf(reservation));
    }

    @Test
    void comingBackFromPaymentFreesTheEarlierSeat() {
        view.seatButtonFor("1B").doClick(0);
        view.continueButton.doClick(0);
        Reservation held = booking.reservation().orElseThrow();
        view.onShow();
        assertEquals(ReservationStatus.CANCELLED, statusOf(held));
        assertTrue(view.seatButtonFor("1B").isEnabled());
    }

    @Test
    void withoutAChosenFlightTheScreenSendsYouToTheSearch() {
        booking.clear();
        navigator.shown.clear();
        view.onShow();
        assertEquals(ViewId.FLIGHT_SEARCH, navigator.last());
    }

    @Test
    void nobodyLoggedInSendsYouToLogin() {
        controllers.session().logout();
        navigator.shown.clear();
        view.onShow();
        assertEquals(ViewId.LOGIN, navigator.last());
    }

    @Test
    void anAgentWithoutAChosenCustomerIsToldToChooseOne() {
        controllers.session().login(controllers.loginController()
                .authenticate(DatabaseSeeder.AGENT_USERNAME, DatabaseSeeder.AGENT_PASSWORD));
        view.onShow();
        assertEquals("Choose a customer to book for first", view.message());
        assertTrue(booking.reservation().isEmpty());
    }
}
