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
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CustomerHomeTest {

    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    static Path folder;

    private static Controllers controllers;
    private static int next;

    private Customer customer;
    private RecordingNavigator navigator;
    private BookingSession booking;
    private CustomerHome home;

    @BeforeAll
    static void startTheProgramParts() {
        controllers = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
    }

    @AfterAll
    static void shutDown() {
        controllers.shutdown().run();
    }

    @BeforeEach
    void aNewCustomerOpensHome() {
        int n = next++;
        customer = controllers.customerController().register("home" + n, "password1", "Casey Home" + n,
                "home" + n + "@x.ca", null);
        controllers.session().login(customer);
        navigator = new RecordingNavigator();
        booking = new BookingSession();
        home = new CustomerHome(controllers, navigator, booking);
    }

    private FlightOffer flightOnDay(int day) {
        return controllers.flightSearchController()
                .search(new SearchCriteria("YYC", "YVR", LocalDate.of(2026, 12, day), null)).get(0);
    }

    /** Books and pays a seat on the first YYC to YVR flight of a December day. */
    private Reservation book(int day, String seat, boolean pay) {
        Reservation reservation = controllers.bookingController().selectSeat(customer, controllers.bookingController()
                .startReservation(customer, customer, flightOnDay(day).flight().id()).id(), seat);
        if (!pay) {
            return reservation;
        }
        return controllers.paymentController().pay(customer, reservation.id(), PaymentMethodType.CREDIT,
                new PaymentDetails("Jo Smith", "4111 1111 1111 1111", "12/30")).reservation();
    }

    // ---- the greeting and the lists

    @Test
    void itGreetsTheCustomerByFirstName() {
        home.onShow();
        assertTrue(home.greetingText().startsWith("Hello, Casey"), home.greetingText());
        assertFalse(home.greetingText().contains("Home"), "only the first name: " + home.greetingText());
    }

    @Test
    void withoutBookingsThereIsNoNextTripAndNothingRecent() {
        home.onShow();
        assertTrue(home.shownTrip().isEmpty());
        assertTrue(home.shownRecent().isEmpty());
        assertFalse(home.allButton.isEnabled());
    }

    @Test
    void theNextTripIsTheConfirmedReservationThatLeavesFirst() {
        Reservation later = book(9, "3C", true);
        Reservation sooner = book(4, "3D", true);
        home.onShow();
        assertEquals(sooner.id(), home.shownTrip().orElseThrow().id());
        assertEquals(2, home.shownRecent().size());
        assertTrue(home.allButton.isEnabled());
        assertTrue(later.id() > 0);
    }

    @Test
    void aReservationStillWaitingForPaymentIsNotTheNextTrip() {
        book(3, "4A", false);
        home.onShow();
        assertTrue(home.shownTrip().isEmpty());
        assertEquals(1, home.shownRecent().size(), "but it is among the recent bookings");
    }

    @Test
    void aCancelledReservationIsNotTheNextTrip() {
        Reservation paid = book(5, "4B", true);
        controllers.reservationController().cancel(customer, paid.id());
        home.onShow();
        assertTrue(home.shownTrip().isEmpty());
    }

    @Test
    void atMostThreeRecentBookingsAreShown() {
        for (int i = 0; i < 5; i++) {
            book(2 + i, "5" + (char) ('A' + i), false);
        }
        home.onShow();
        assertEquals(3, home.shownRecent().size());
    }

    // ---- finding a flight from Home

    @Test
    void searchingRemembersTheChoicesAndOpensTheFlightSearch() {
        home.onShow();
        home.originCombo.setSelectedIndex(1);
        home.destinationCombo.setSelectedIndex(0);
        home.dateField.setText("2026-12-05");
        home.searchButton.doClick(0);
        assertEquals(ViewId.FLIGHT_SEARCH, navigator.last());
        BookingSession.SearchMemory memory = booking.lastSearch().orElseThrow();
        assertEquals(home.originCombo.getItemAt(1).code(), memory.origin());
        assertEquals(home.destinationCombo.getItemAt(0).code(), memory.destination());
        assertEquals(LocalDate.of(2026, 12, 5), memory.date());
    }

    @Test
    void aBadDateIsExplainedAndNothingOpens() {
        home.onShow();
        home.dateField.setText("tomorrow");
        home.searchButton.doClick(0);
        assertTrue(navigator.shown.isEmpty());
        assertFalse(home.alertText().isBlank());
    }

    @Test
    void theLastSearchIsPutBackInTheBoxes() {
        booking.rememberSearch("YVR", "YYC", null, LocalDate.of(2026, 12, 8));
        home.onShow();
        assertEquals("YVR", home.originCombo.getItemAt(home.originCombo.getSelectedIndex()).code());
        assertEquals("YYC", home.destinationCombo.getItemAt(home.destinationCombo.getSelectedIndex()).code());
        assertEquals("2026-12-08", home.dateField.getText());
    }

    @Test
    void withNoSearchYetTheBoxesStartOnARouteThatHasAFlight() {
        home.onShow();
        List<FlightOffer> found = controllers.flightSearchController().search(new SearchCriteria(
                home.originCombo.getItemAt(home.originCombo.getSelectedIndex()).code(),
                home.destinationCombo.getItemAt(home.destinationCombo.getSelectedIndex()).code(),
                LocalDate.parse(home.dateField.getText()), null));
        assertFalse(found.isEmpty(), "the first search from Home is not an empty one");
    }

    @Test
    void seeAllOpensTheReservations() {
        book(3, "6A", false);
        home.onShow();
        home.allButton.doClick(0);
        assertEquals(ViewId.MY_RESERVATIONS, navigator.last());
    }

    @Test
    void onlyACustomerSeesTheCustomerHome() {
        controllers.session().login(controllers.loginController()
                .authenticate(DatabaseSeeder.AGENT_USERNAME, DatabaseSeeder.AGENT_PASSWORD));
        home.onShow();
        assertEquals(ViewId.LOGIN, navigator.last());
    }

    @Test
    void theNextTripLogicHandlesAnEmptyList() {
        assertTrue(HomeTrips.next(List.of(), LocalDateTime.of(2026, 12, 1, 9, 0)).isEmpty());
        assertTrue(HomeTrips.recent(List.of(), 3).isEmpty());
    }

    @Test
    void theFirstNameFallsBackToTheUsername() {
        assertEquals("Casey", HomeTrips.firstName("Casey  Customer", "customer1"));
        assertEquals("customer1", HomeTrips.firstName("", "customer1"));
        assertEquals("customer1", HomeTrips.firstName(null, "customer1"));
    }
}
