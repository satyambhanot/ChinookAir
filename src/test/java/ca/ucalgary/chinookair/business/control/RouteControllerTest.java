package ca.ucalgary.chinookair.business.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Administrator;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.FlightAgent;
import ca.ucalgary.chinookair.business.entity.Route;
import ca.ucalgary.chinookair.data.DaoFactory;
import ca.ucalgary.chinookair.data.TestDatabase;
import ca.ucalgary.chinookair.data.TestFixtures;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RouteControllerTest {

    private static final String HASH = "pbkdf2$1$AAAA$BBBB";

    private Connection db;
    private DaoFactory daos;
    private RouteController controller;
    private Administrator admin;
    private FlightAgent agent;
    private Customer customer;

    @BeforeEach
    void open() {
        db = TestDatabase.inMemory();
        daos = DaoFactory.create(db);
        controller = new RouteController(daos.routeDao(), daos.airportDao());
        daos.airportDao().save(TestFixtures.airport("YYC"));
        daos.airportDao().save(TestFixtures.airport("YVR"));
        daos.airportDao().save(TestFixtures.airport("YEG"));
        admin = (Administrator) daos.userDao().save(new Administrator(null, "admin", HASH));
        agent = (FlightAgent) daos.userDao().save(new FlightAgent(null, "agent1", HASH));
        customer = daos.customerDao().save(new Customer(null, "jsmith", HASH, "Jo Smith", "j@x.ca", null));
    }

    @AfterEach
    void close() throws SQLException {
        db.close();
    }

    private String messageOf(Runnable action) {
        return assertThrows(IllegalArgumentException.class, action::run).getMessage();
    }

    @Test
    void anAdministratorAddsARouteBetweenTwoKnownAirports() {
        Route route = controller.addRoute(admin, "YYC", "YVR");
        assertNotNull(route.id());
        assertEquals("Calgary", route.origin().city());
        assertEquals("Vancouver", route.destination().city());
        assertTrue(daos.routeDao().findByAirports("YYC", "YVR").isPresent());
    }

    @Test
    void theSameRouteTwiceIsRefusedButTheReverseIsAnotherRoute() {
        controller.addRoute(admin, "YYC", "YVR");
        assertTrue(messageOf(() -> controller.addRoute(admin, "YYC", "YVR")).contains("Route already exists"));
        assertTrue(messageOf(() -> controller.addRoute(admin, "yyc", " yvr ")).contains("Route already exists"));
        assertNotNull(controller.addRoute(admin, "YVR", "YYC").id());
        assertEquals(2, controller.list().size());
    }

    @Test
    void anUnknownAirportIsRefused() {
        assertTrue(messageOf(() -> controller.addRoute(admin, "YYC", "XXX")).contains("Airport not found"));
        assertTrue(messageOf(() -> controller.addRoute(admin, "ZZZ", "YVR")).contains("Airport not found"));
        assertTrue(daos.routeDao().findAll().isEmpty());
    }

    @Test
    void bothAirportsAreRequiredAndMustDiffer() {
        assertTrue(messageOf(() -> controller.addRoute(admin, null, "YVR")).contains("Origin is required"));
        assertTrue(messageOf(() -> controller.addRoute(admin, "YYC", " ")).contains("Destination is required"));
        assertTrue(messageOf(() -> controller.addRoute(admin, "YYC", "YYC")).contains("must be different"));
    }

    @Test
    void onlyAnAdministratorMayAddARoute() {
        assertThrows(AccessDeniedException.class, () -> controller.addRoute(customer, "YYC", "YVR"));
        assertThrows(AccessDeniedException.class, () -> controller.addRoute(agent, "YYC", "YVR"));
        assertThrows(AccessDeniedException.class, () -> controller.addRoute(null, "YYC", "YVR"));
        assertTrue(controller.list().isEmpty());
    }

    @Test
    void listIsSortedByOriginThenDestination() {
        controller.addRoute(admin, "YVR", "YYC");
        controller.addRoute(admin, "YYC", "YVR");
        controller.addRoute(admin, "YYC", "YEG");
        List<String> pairs = controller.list().stream()
                .map(r -> r.origin().code() + "-" + r.destination().code()).toList();
        assertEquals(List.of("YVR-YYC", "YYC-YEG", "YYC-YVR"), pairs);
    }
}
