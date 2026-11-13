package ca.ucalgary.chinookair.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Administrator;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.FlightAgent;
import ca.ucalgary.chinookair.business.entity.Role;
import ca.ucalgary.chinookair.business.entity.User;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqliteUserDaoTest {

    private static final String HASH = "pbkdf2$1$AAAA$BBBB";

    private Connection db;
    private UserDao dao;

    @BeforeEach
    void open() {
        db = TestDatabase.inMemory();
        dao = new SqliteUserDao(db);
    }

    @AfterEach
    void close() throws SQLException {
        db.close();
    }

    private int count(String table) throws SQLException {
        try (Statement st = db.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    @Test
    void anAdministratorComesBackAsAnAdministrator() {
        User saved = dao.save(new Administrator(null, "admin", HASH));
        assertNotNull(saved.id());
        User found = dao.findByUsername("admin").orElseThrow();
        assertInstanceOf(Administrator.class, found);
        assertEquals(Role.ADMIN, found.role());
        assertEquals(saved.id(), found.id());
        assertEquals(HASH, found.passwordHash());
    }

    @Test
    void anAgentComesBackAsAFlightAgent() {
        dao.save(new FlightAgent(null, "agent1", HASH));
        User found = dao.findByUsername("agent1").orElseThrow();
        assertInstanceOf(FlightAgent.class, found);
        assertEquals(Role.AGENT, found.role());
    }

    @Test
    void aCustomerSavedThroughUserDaoKeepsItsProfile() throws SQLException {
        User saved = dao.save(new Customer(null, "jsmith", HASH, "Jo Smith", "j@x.ca", "403-555-0101"));
        assertEquals(1, count("customer"));
        Customer found = assertInstanceOf(Customer.class, dao.findByUsername("JSMITH").orElseThrow());
        assertEquals("j@x.ca", found.email());
        assertEquals("403-555-0101", found.phone());
        assertEquals(saved.id(), found.id());
    }

    @Test
    void usernamesAreFoundIgnoringCaseAndSpaces() {
        dao.save(new Administrator(null, "admin", HASH));
        assertTrue(dao.findByUsername("ADMIN").isPresent());
        assertTrue(dao.findByUsername("  Admin ").isPresent());
        assertTrue(dao.findByUsername("nobody").isEmpty());
        assertTrue(dao.findByUsername(null).isEmpty());
    }

    @Test
    void findByIdReturnsTheRightSubclass() {
        User admin = dao.save(new Administrator(null, "admin", HASH));
        User customer = dao.save(new Customer(null, "jsmith", HASH, "Jo Smith", "j@x.ca", null));
        assertInstanceOf(Administrator.class, dao.findById(admin.id()).orElseThrow());
        assertInstanceOf(Customer.class, dao.findById(customer.id()).orElseThrow());
        assertTrue(dao.findById(99).isEmpty());
    }

    @Test
    void aUsernameCanOnlyBeUsedOnceWhateverTheRole() {
        dao.save(new Administrator(null, "admin", HASH));
        DataAccessException e = assertThrows(DataAccessException.class,
                () -> dao.save(new FlightAgent(null, "ADMIN", HASH)));
        assertTrue(e.getMessage().contains("username"), e.getMessage());
    }

    @Test
    void aUserThatIsAlreadySavedCannotBeSavedAgain() {
        User saved = dao.save(new Administrator(null, "admin", HASH));
        assertThrows(IllegalArgumentException.class, () -> dao.save(saved));
    }
}
