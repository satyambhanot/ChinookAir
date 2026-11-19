package ca.ucalgary.chinookair.business.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Administrator;
import ca.ucalgary.chinookair.business.entity.Aircraft;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.FlightAgent;
import ca.ucalgary.chinookair.data.DaoFactory;
import ca.ucalgary.chinookair.data.TestDatabase;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AircraftControllerTest {

    private static final String HASH = "pbkdf2$1$AAAA$BBBB";

    private Connection db;
    private DaoFactory daos;
    private AircraftController controller;
    private Administrator admin;
    private FlightAgent agent;
    private Customer customer;

    @BeforeEach
    void open() {
        db = TestDatabase.inMemory();
        daos = DaoFactory.create(db);
        controller = new AircraftController(daos.aircraftDao());
        admin = (Administrator) daos.userDao().save(new Administrator(null, "admin", HASH));
        agent = (FlightAgent) daos.userDao().save(new FlightAgent(null, "agent1", HASH));
        customer = daos.customerDao().save(new Customer(null, "jsmith", HASH, "Jo Smith", "j@x.ca", null));
    }

    @AfterEach
    void close() throws SQLException {
        db.close();
    }

    @Test
    void anAdministratorAddsAnAircraftAndItHasItsSeatCount() {
        Aircraft a320 = controller.addAircraft(admin, "A320", 30, 6);
        assertNotNull(a320.id());
        assertEquals(180, a320.seatCount());
        assertEquals("A320", daos.aircraftDao().findById(a320.id()).orElseThrow().model());
    }

    @Test
    void theLayoutMustBeSensible() {
        assertThrows(IllegalArgumentException.class, () -> controller.addAircraft(admin, "A320", 0, 6));
        assertThrows(IllegalArgumentException.class, () -> controller.addAircraft(admin, "A320", 61, 6));
        assertThrows(IllegalArgumentException.class, () -> controller.addAircraft(admin, "A320", 30, 7));
        assertThrows(IllegalArgumentException.class, () -> controller.addAircraft(admin, "A320", 30, 0));
        assertThrows(IllegalArgumentException.class, () -> controller.addAircraft(admin, " ", 30, 6));
        assertTrue(controller.list().isEmpty());
    }

    @Test
    void onlyAnAdministratorMayAddAnAircraft() {
        assertThrows(AccessDeniedException.class, () -> controller.addAircraft(customer, "A320", 30, 6));
        assertThrows(AccessDeniedException.class, () -> controller.addAircraft(agent, "A320", 30, 6));
        assertThrows(AccessDeniedException.class, () -> controller.addAircraft(null, "A320", 30, 6));
        assertTrue(controller.list().isEmpty());
    }

    @Test
    void listKeepsTheOrderTheyWereAdded() {
        Aircraft first = controller.addAircraft(admin, "A320", 30, 6);
        Aircraft second = controller.addAircraft(admin, "Dash 8", 20, 4);
        assertEquals(List.of(first, second), controller.list());
    }
}
