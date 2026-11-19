package ca.ucalgary.chinookair.business.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Administrator;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.FlightAgent;
import ca.ucalgary.chinookair.business.entity.User;
import ca.ucalgary.chinookair.data.DaoFactory;
import ca.ucalgary.chinookair.data.TestDatabase;
import ca.ucalgary.chinookair.util.PasswordHasher;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LoginControllerTest {

    private static Connection db;
    private static LoginController controller;

    @BeforeAll
    static void saveTheDemoUsers() {
        db = TestDatabase.inMemory();
        DaoFactory daos = DaoFactory.create(db);
        daos.customerDao().save(new Customer(null, "customer1", PasswordHasher.hash("customer123"), "Casey Customer",
                "customer1@example.com", null));
        daos.userDao().save(new FlightAgent(null, "agent1", PasswordHasher.hash("agent123")));
        daos.userDao().save(new Administrator(null, "admin", PasswordHasher.hash("admin123")));
        controller = new LoginController(daos.userDao());
    }

    @AfterAll
    static void close() throws SQLException {
        db.close();
    }

    @Test
    void adminLogsInAndGetsAnAdministrator() {
        User user = controller.authenticate("admin", "admin123");
        assertInstanceOf(Administrator.class, user);
        assertEquals("admin", user.username());
    }

    @Test
    void theUsernameIsTrimmedAndIgnoresCase() {
        assertEquals("admin", controller.authenticate(" ADMIN ", "admin123").username());
    }

    @Test
    void everyRoleGetsItsOwnClass() {
        assertInstanceOf(Customer.class, controller.authenticate("customer1", "customer123"));
        assertInstanceOf(FlightAgent.class, controller.authenticate("agent1", "agent123"));
    }

    @Test
    void aWrongPasswordAndAnUnknownUserLookTheSame() {
        AuthenticationException wrong = assertThrows(AuthenticationException.class,
                () -> controller.authenticate("admin", "wrong"));
        AuthenticationException unknown = assertThrows(AuthenticationException.class,
                () -> controller.authenticate("nobody", "x"));
        assertEquals("Invalid username or password", wrong.getMessage());
        assertEquals(wrong.getMessage(), unknown.getMessage());
    }

    @Test
    void thePasswordIsCaseSensitive() {
        assertThrows(AuthenticationException.class, () -> controller.authenticate("admin", "Admin123"));
    }

    @Test
    void aBlankUsernameOrPasswordIsAskedFor() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> controller.authenticate("", "x"))
                .getMessage().contains("Username is required"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> controller.authenticate("  ", "x"))
                .getMessage().contains("Username is required"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> controller.authenticate(null, "x"))
                .getMessage().contains("Username is required"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> controller.authenticate("admin", ""))
                .getMessage().contains("Password is required"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> controller.authenticate("admin", null))
                .getMessage().contains("Password is required"));
    }

    @Test
    void theUsernameIsCheckedBeforeThePassword() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> controller.authenticate("", ""))
                .getMessage().contains("Username is required"));
    }

    @Test
    void loggingInTwiceGivesTheSameUser() {
        User first = controller.authenticate("agent1", "agent123");
        User second = controller.authenticate("agent1", "agent123");
        assertEquals(first.id(), second.id());
        assertSame(first.getClass(), second.getClass());
    }
}
