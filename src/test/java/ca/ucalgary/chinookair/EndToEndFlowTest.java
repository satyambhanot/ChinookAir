package ca.ucalgary.chinookair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.control.FlightDetails;
import ca.ucalgary.chinookair.business.control.FlightOffer;
import ca.ucalgary.chinookair.business.control.PaymentResult;
import ca.ucalgary.chinookair.business.control.SearchCriteria;
import ca.ucalgary.chinookair.business.entity.Aircraft;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.FlightAgent;
import ca.ucalgary.chinookair.business.entity.PaymentDetails;
import ca.ucalgary.chinookair.business.entity.PaymentMethodType;
import ca.ucalgary.chinookair.business.entity.PaymentStatus;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.ReservationStatus;
import ca.ucalgary.chinookair.business.entity.Route;
import ca.ucalgary.chinookair.business.entity.SeatClass;
import ca.ucalgary.chinookair.business.entity.User;
import ca.ucalgary.chinookair.data.DatabaseSeeder;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.io.TempDir;

/**
 * One walk through the real controllers on a seeded database: a customer books, pays and cancels,
 * the administrator adds a flight, and an agent moves a reservation to it. It checks that the parts
 * work together, which the tests of each part alone cannot show.
 */
class EndToEndFlowTest {

    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    private static final LocalDate TOMORROW = LocalDate.of(2026, 12, 2);
    private static final PaymentDetails GOOD_CARD = new PaymentDetails("Jo Smith", "4111 1111 1111 1111", "12/30");
    private static final PaymentDetails DECLINED_CARD = new PaymentDetails("Jo Smith", "4111 1111 1111 0000", "12/30");

    @TempDir
    Path confirmations;

    private Controllers controllers;

    @BeforeEach
    void startWithACleanDatabase() {
        controllers = AppContext.create("jdbc:sqlite::memory:", CLOCK, confirmations);
    }

    @AfterEach
    void shutDown() {
        controllers.shutdown().run();
    }

    private User logIn(String username, String password) {
        User user = controllers.loginController().authenticate(username, password);
        controllers.session().login(user);
        return user;
    }

    private FlightOffer firstOffer(LocalDate day) {
        List<FlightOffer> found = controllers.flightSearchController()
                .search(new SearchCriteria("YYC", "YVR", day, null));
        assertFalse(found.isEmpty(), "step search: no flight YYC to YVR on " + day);
        return found.get(0);
    }

    private Reservation bookAndPay(Customer customer, FlightOffer offer, String seat) {
        Reservation started = controllers.bookingController().startReservation(customer, customer,
                offer.flight().id());
        Reservation waiting = controllers.bookingController().selectSeat(customer, started.id(), seat);
        PaymentResult paid = controllers.paymentController().pay(customer, waiting.id(), PaymentMethodType.CREDIT,
                GOOD_CARD);
        assertEquals(PaymentStatus.APPROVED, paid.payment().status(), "step pay");
        return paid.reservation();
    }

    private ReservationStatus statusOf(Customer customer, Reservation reservation) {
        return controllers.reservationController().history(customer, customer.id()).stream()
                .filter(r -> r.id().equals(reservation.id())).findFirst().orElseThrow().status();
    }

    @RepeatedTest(2)
    void aCustomerBooksPaysAndCancelsThenTheAdministratorAndAnAgentChangeThings() throws IOException {
        // ---- the customer registers and logs in
        controllers.customerController().register("e2eflyer", "password1", "Jo Smith", "jo.e2e@example.com",
                "403-555-0100");
        Customer jo = (Customer) logIn("e2eflyer", "password1");
        assertEquals("Jo Smith", jo.name(), "step register and log in");

        // ---- search, reserve, choose a seat
        FlightOffer offer = firstOffer(TOMORROW);
        int seatsBefore = offer.seatsLeft();
        Reservation started = controllers.bookingController().startReservation(jo, jo, offer.flight().id());
        assertEquals(ReservationStatus.CREATED, started.status(), "step start reservation");
        Reservation waiting = controllers.bookingController().selectSeat(jo, started.id(), "3C");
        assertEquals(ReservationStatus.PENDING_PAYMENT, waiting.status(), "step select seat");
        assertEquals(0, offer.flight().priceFor(SeatClass.ECONOMY)
                .compareTo(waiting.price()), "step seat price");
        assertEquals(seatsBefore - 1, firstOffer(TOMORROW).seatsLeft(), "step seat is taken");

        // ---- a declined card, then a good one
        PaymentResult declined = controllers.paymentController().pay(jo, waiting.id(), PaymentMethodType.CREDIT,
                DECLINED_CARD);
        assertEquals(PaymentStatus.DECLINED, declined.payment().status(), "step declined card");
        assertEquals(ReservationStatus.PENDING_PAYMENT, statusOf(jo, waiting), "step declined keeps waiting");
        PaymentResult approved = controllers.paymentController().pay(jo, waiting.id(), PaymentMethodType.CREDIT,
                GOOD_CARD);
        assertEquals(PaymentStatus.APPROVED, approved.payment().status(), "step good card");

        // ---- the confirmation file and the history
        Path file = approved.confirmationFile();
        assertNotNull(file, "step confirmation file path");
        assertTrue(Files.readString(file).contains(approved.reservation().confirmationNumber()),
                "step confirmation file holds the number");
        assertEquals(ReservationStatus.CONFIRMED, statusOf(jo, waiting), "step history shows CONFIRMED");

        // ---- cancel: refund and a free seat
        controllers.reservationController().cancel(jo, waiting.id());
        assertEquals(ReservationStatus.CANCELLED, statusOf(jo, waiting), "step cancel");
        assertTrue(controllers.paymentController().recordsForCustomer(jo, jo.id()).stream()
                .filter(p -> p.status() != PaymentStatus.DECLINED)
                .allMatch(p -> p.status() == PaymentStatus.REFUNDED), "step payment refunded");
        assertTrue(controllers.bookingController().availableSeats(offer.flight().id()).stream()
                .anyMatch(s -> s.number().equals("3C")), "step seat is free again");

        // ---- the administrator adds a pricier flight and a customer can find it
        User admin = logIn(DatabaseSeeder.ADMIN_USERNAME, DatabaseSeeder.ADMIN_PASSWORD);
        Aircraft plane = controllers.aircraftController().addAircraft(admin, "E2E jet", 6, 4);
        Route route = controllers.routeController().list().stream()
                .filter(r -> r.origin().code().equals("YYC") && r.destination().code().equals("YVR"))
                .findFirst().orElseThrow();
        LocalDate far = LocalDate.of(2026, 12, 20);
        Flight added = controllers.flightManagementController().addFlight(admin, new FlightDetails("AC9901",
                route.id(), "AC", plane.id(), far.atTime(10, 0), far.atTime(12, 0), new BigDecimal("500.00")));
        logIn("e2eflyer", "password1");
        FlightOffer pricier = controllers.flightSearchController()
                .search(new SearchCriteria("YYC", "YVR", far, null)).stream()
                .filter(o -> o.flight().id().equals(added.id())).findFirst()
                .orElseThrow(() -> new AssertionError("step customer finds the new flight"));
        assertEquals(24, pricier.seatsLeft(), "step new flight has seats");

        // ---- an agent finds the customer and moves a second reservation to the pricier flight
        Reservation second = bookAndPay(jo, firstOffer(TOMORROW), "4A");
        FlightAgent agent = (FlightAgent) logIn(DatabaseSeeder.AGENT_USERNAME, DatabaseSeeder.AGENT_PASSWORD);
        List<Customer> found = controllers.customerController().search(agent, "jo.e2e@example.com");
        assertEquals(1, found.size(), "step agent finds the customer");
        Reservation moved = controllers.reservationController().modify(agent, second.id(), pricier.flight().id(),
                "4A");
        assertEquals(ReservationStatus.PENDING_PAYMENT, moved.status(), "step modify to a pricier flight");
        assertEquals(pricier.flight().id(), moved.flight().id(), "step modify moved the flight");
        assertTrue(controllers.paymentController().recordsForCustomer(agent, jo.id()).stream()
                .anyMatch(p -> p.reservationId().equals(second.id()) && p.status() == PaymentStatus.REFUNDED),
                "step old payment refunded after modify");
    }

    /**
     * Two runs of the program on one database file: first a customer books and an agent moves the seat, then, after
     * take-off, the administrator removes the flight that nobody has opened the history of.
     */
    @org.junit.jupiter.api.Test
    void aMovedSeatKeepsTheConfirmationCurrentAndADepartedFlightWithABookingCanBeRemoved() throws IOException {
        String url = "jdbc:sqlite:" + confirmations.resolve("e2e.db");
        long flightId;
        Path file;
        Controllers first = AppContext.create(url, CLOCK, confirmations);
        try {
            User jo = first.loginController().authenticate(DatabaseSeeder.CUSTOMER_USERNAME,
                    DatabaseSeeder.CUSTOMER_PASSWORD);
            FlightOffer offer = first.flightSearchController().search(new SearchCriteria("YYC", "YVR", TOMORROW, null))
                    .get(0);
            flightId = offer.flight().id();
            Reservation started = first.bookingController().startReservation(jo, (Customer) jo, flightId);
            Reservation waiting = first.bookingController().selectSeat(jo, started.id(), "3C");
            PaymentResult paid = first.paymentController().pay(jo, waiting.id(), PaymentMethodType.CREDIT, GOOD_CARD);
            file = paid.confirmationFile();
            assertTrue(Files.readString(file).contains("Seat: 3C"), "step first confirmation");

            User agent = first.loginController().authenticate(DatabaseSeeder.AGENT_USERNAME,
                    DatabaseSeeder.AGENT_PASSWORD);
            Reservation moved = first.reservationController().modify(agent, waiting.id(), flightId, "3D");
            assertEquals(ReservationStatus.CONFIRMED, moved.status(), "step same price stays confirmed");
            String text = Files.readString(file);
            assertTrue(text.contains("Seat: 3D") && !text.contains("Seat: 3C"), "step file shows the new seat");
        } finally {
            first.shutdown().run();
        }

        Clock afterTakeOff = Clock.fixed(LocalDateTime.of(2026, 12, 3, 10, 0).toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC);
        Controllers second = AppContext.create(url, afterTakeOff, confirmations);
        try {
            User admin = second.loginController().authenticate(DatabaseSeeder.ADMIN_USERNAME,
                    DatabaseSeeder.ADMIN_PASSWORD);
            second.flightManagementController().removeFlight(admin, flightId);
            User jo = second.loginController().authenticate(DatabaseSeeder.CUSTOMER_USERNAME,
                    DatabaseSeeder.CUSTOMER_PASSWORD);
            List<Reservation> history = second.reservationController().history(jo, jo.id());
            assertEquals(ReservationStatus.COMPLETED, history.get(0).status(), "step history keeps the trip");
        } finally {
            second.shutdown().run();
        }
    }
}
