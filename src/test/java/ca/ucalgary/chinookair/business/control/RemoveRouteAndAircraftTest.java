package ca.ucalgary.chinookair.business.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Administrator;
import ca.ucalgary.chinookair.business.entity.Aircraft;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.FlightAgent;
import ca.ucalgary.chinookair.business.entity.Route;
import ca.ucalgary.chinookair.data.DaoFactory;
import ca.ucalgary.chinookair.data.TestDatabase;
import ca.ucalgary.chinookair.data.TestWorld;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Removing a route or an aircraft: allowed while no flight uses it, refused afterwards. */
class RemoveRouteAndAircraftTest {

    private static final String HASH = "pbkdf2$1$AAAA$BBBB";

    private Connection db;
    private TestWorld world;
    private DaoFactory daos;
    private RouteController routes;
    private AircraftController aircraft;
    private Administrator admin;
    private FlightAgent agent;
    private Customer customer;

    @BeforeEach
    void open() {
        db = TestDatabase.inMemory();
        world = new TestWorld(db);
        daos = DaoFactory.create(db);
        routes = new RouteController(daos.routeDao(), daos.airportDao());
        aircraft = new AircraftController(daos.aircraftDao());
        admin = (Administrator) daos.userDao().save(new Administrator(null, "admin", HASH));
        agent = (FlightAgent) daos.userDao().save(new FlightAgent(null, "agent1", HASH));
        customer = daos.customerDao().findById(1).orElseThrow();
        // one flight uses the YYC to YVR route and the small aircraft
        daos.flightDao().save(world.flight("AC1", world.yycYvr, world.ac, world.small,
                LocalDateTime.of(2026, 12, 2, 8, 0)));
    }

    @AfterEach
    void close() throws SQLException {
        db.close();
    }

    @Test
    void aRouteNoFlightUsesCanBeRemoved() {
        routes.removeRoute(admin, world.yycYeg.id());
        assertTrue(daos.routeDao().findById(world.yycYeg.id()).isEmpty());
        assertEquals(1, routes.list().size());
    }

    @Test
    void aRouteThatAFlightUsesStays() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> routes.removeRoute(admin, world.yycYvr.id()));
        assertEquals("Cannot remove a route that flights are scheduled on", e.getMessage());
        assertTrue(daos.routeDao().findById(world.yycYvr.id()).isPresent());
    }

    @Test
    void aRouteOfARemovedFlightStaysToo() {
        long flight = daos.flightDao().search("YYC", "YVR", java.time.LocalDate.of(2026, 12, 2), null).get(0).id();
        daos.flightDao().archive(flight);
        assertThrows(IllegalStateException.class, () -> routes.removeRoute(admin, world.yycYvr.id()));
    }

    @Test
    void anAircraftNoFlightUsesCanBeRemoved() {
        aircraft.removeAircraft(admin, world.big.id());
        assertTrue(daos.aircraftDao().findById(world.big.id()).isEmpty());
        assertEquals(1, aircraft.list().size());
    }

    @Test
    void anAircraftThatAFlightUsesStays() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> aircraft.removeAircraft(admin, world.small.id()));
        assertEquals("Cannot remove an aircraft that flights use", e.getMessage());
        assertTrue(daos.aircraftDao().findById(world.small.id()).isPresent());
    }

    @Test
    void unknownOnesAreAnError() {
        assertThrows(IllegalArgumentException.class, () -> routes.removeRoute(admin, 999));
        assertThrows(IllegalArgumentException.class, () -> aircraft.removeAircraft(admin, 999));
    }

    @Test
    void onlyAnAdministratorMayRemoveThem() {
        Route unused = world.yycYeg;
        Aircraft free = world.big;
        for (var who : new ca.ucalgary.chinookair.business.entity.User[] {agent, customer, null}) {
            assertThrows(AccessDeniedException.class, () -> routes.removeRoute(who, unused.id()));
            assertThrows(AccessDeniedException.class, () -> aircraft.removeAircraft(who, free.id()));
        }
        assertTrue(daos.routeDao().findById(unused.id()).isPresent());
        assertTrue(daos.aircraftDao().findById(free.id()).isPresent());
    }
}
