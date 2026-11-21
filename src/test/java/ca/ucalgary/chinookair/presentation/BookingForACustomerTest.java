package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.AppContext;
import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.control.FlightDetails;
import ca.ucalgary.chinookair.business.control.FlightOffer;
import ca.ucalgary.chinookair.business.control.PaymentResult;
import ca.ucalgary.chinookair.business.control.SearchCriteria;
import ca.ucalgary.chinookair.business.entity.Aircraft;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.PaymentDetails;
import ca.ucalgary.chinookair.business.entity.PaymentMethodType;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.Route;
import ca.ucalgary.chinookair.business.entity.User;
import ca.ucalgary.chinookair.data.DatabaseSeeder;
import java.math.BigDecimal;
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

/** The screens an agent walks through to book a flight for a customer, and a flight that is full. */
class BookingForACustomerTest {

    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    static Path folder;

    private static Controllers controllers;
    private static int next;

    private Customer jo;
    private User agent;
    private RecordingNavigator navigator;
    private BookingSession booking;

    @BeforeAll
    static void startTheProgramParts() {
        controllers = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
    }

    @AfterAll
    static void shutDown() {
        controllers.shutdown().run();
    }

    @BeforeEach
    void anAgentIsLoggedIn() {
        int n = next++;
        jo = controllers.customerController().register("booked" + n, "password1", "Book Ed " + n,
                "booked" + n + "@x.ca", null);
        agent = controllers.loginController().authenticate(DatabaseSeeder.AGENT_USERNAME,
                DatabaseSeeder.AGENT_PASSWORD);
        controllers.session().login(agent);
        navigator = new RecordingNavigator();
        booking = new BookingSession();
    }

    @Test
    void theSearchKeepsTheCustomerTheAgentChoseAndTheSeatScreenBooksForThatCustomer() {
        CustomerManagementView customers = new CustomerManagementView(controllers, navigator, booking);
        customers.onShow();
        customers.searchField.setText(jo.email());
        customers.search();
        customers.table.setRowSelectionInterval(0, 0);
        customers.bookButton.doClick(0);
        assertEquals(ViewId.FLIGHT_SEARCH, navigator.last());

        FlightSearchView search = new FlightSearchView(controllers, navigator, booking);
        search.onShow();
        assertEquals(jo.id(), booking.customer().orElseThrow().id(), "the search must not forget the customer");
        assertTrue(search.results.offers().size() > 0);
        search.results.setSelectedIndex(0);
        search.selectButton.doClick(0);
        assertEquals(ViewId.SEAT_SELECTION, navigator.last());

        SeatSelectionView seats = new SeatSelectionView(controllers, navigator, booking);
        seats.onShow();
        Reservation started = booking.reservation().orElseThrow();
        assertEquals(jo.id(), started.customer().id());
        assertEquals("", seats.message());
    }

    @Test
    void bookingAnotherFlightAfterAConfirmationKeepsTheCustomerForAnAgent() {
        FlightOffer offer = controllers.flightSearchController()
                .search(new SearchCriteria("YYC", "YVR", LocalDate.of(2026, 12, 2).plusDays(next % 20), null)).get(0);
        Reservation started = controllers.bookingController().startReservation(agent, jo, offer.flight().id());
        Reservation waiting = controllers.bookingController().selectSeat(agent, started.id(), "3C");
        PaymentResult paid = controllers.paymentController().pay(agent, waiting.id(), PaymentMethodType.CREDIT,
                new PaymentDetails("Jo Smith", "4111 1111 1111 1111", "12/30"));
        booking.bookFor(jo);
        booking.setReservation(paid.reservation());
        booking.setPayment(paid);
        ConfirmationView confirmation = new ConfirmationView(controllers, navigator, booking);
        confirmation.onShow();

        confirmation.anotherButton.doClick(0);

        assertEquals(ViewId.FLIGHT_SEARCH, navigator.last());
        assertEquals(jo.id(), booking.customer().orElseThrow().id());
    }

    @Test
    void aFlightWithNoSeatsLeftCannotBeSelected() {
        // a one-seat aircraft whose only seat a customer takes
        controllers.session().login(controllers.loginController().authenticate(DatabaseSeeder.ADMIN_USERNAME,
                DatabaseSeeder.ADMIN_PASSWORD));
        User admin = controllers.session().currentUser().orElseThrow();
        Aircraft tiny = controllers.aircraftController().addAircraft(admin, "Tiny " + next, 1, 1);
        Route route = controllers.routeController().list().stream()
                .filter(r -> r.origin().code().equals("YYC") && r.destination().code().equals("YVR")).findFirst()
                .orElseThrow();
        LocalDate day = LocalDate.of(2026, 12, 30);
        controllers.flightManagementController().addFlight(admin, new FlightDetails("TY" + (100 + next), route.id(),
                "AC", tiny.id(), day.atTime(23, 0), day.atTime(23, 55), new BigDecimal("100.00")));
        FlightOffer offer = controllers.flightSearchController()
                .search(new SearchCriteria("YYC", "YVR", day, "AC")).stream()
                .filter(o -> o.flight().aircraft().id().equals(tiny.id())).findFirst().orElseThrow();
        Reservation started = controllers.bookingController().startReservation(agent, jo, offer.flight().id());
        controllers.session().login(agent);
        controllers.bookingController().selectSeat(agent, started.id(), "1A");

        FlightSearchView search = new FlightSearchView(controllers, navigator, booking);
        search.onShow();
        search.dateField.setText(day.toString());
        search.originCombo.setSelectedItem(route.origin());
        search.destinationCombo.setSelectedItem(route.destination());
        search.searchButton.doClick(0);
        List<FlightOffer> offers = search.results.offers();
        int index = -1;
        for (int i = 0; i < offers.size(); i++) {
            if (offers.get(i).flight().id().equals(offer.flight().id())) {
                index = i;
            }
        }
        assertTrue(index >= 0, "the full flight is still listed");
        search.results.setSelectedIndex(index);
        search.selectButton.doClick(0);

        assertEquals("No seats left on this flight", search.message());
        assertFalse(navigator.shown.contains(ViewId.SEAT_SELECTION));
    }
}
