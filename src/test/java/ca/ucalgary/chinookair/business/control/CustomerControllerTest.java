package ca.ucalgary.chinookair.business.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Administrator;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.FlightAgent;
import ca.ucalgary.chinookair.data.DaoFactory;
import ca.ucalgary.chinookair.data.TestDatabase;
import ca.ucalgary.chinookair.util.PasswordHasher;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerControllerTest {

    private static final String HASH = "pbkdf2$1$AAAA$BBBB";

    private Connection db;
    private DaoFactory daos;
    private CustomerController controller;
    private FlightAgent agent;
    private Administrator admin;

    @BeforeEach
    void open() {
        db = TestDatabase.inMemory();
        daos = DaoFactory.create(db);
        controller = new CustomerController(daos.customerDao(), daos.userDao());
        agent = (FlightAgent) daos.userDao().save(new FlightAgent(null, "agent1", HASH));
        admin = (Administrator) daos.userDao().save(new Administrator(null, "admin", HASH));
    }

    @AfterEach
    void close() throws SQLException {
        db.close();
    }

    private Customer registerJo() {
        return controller.register("jsmith", "pass1234", "Jo Smith", "jo@x.ca", "");
    }

    private Customer registerBo() {
        return controller.register("bwong", "pass1234", "Bo Wong", "bo@x.ca", "403-555-0101");
    }

    private String messageOf(Runnable action) {
        return assertThrows(IllegalArgumentException.class, action::run).getMessage();
    }

    private int count(String table) throws SQLException {
        try (Statement st = db.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    // ---- register

    @Test
    void aCustomerIsRegisteredWithAHashedPassword() {
        Customer jo = registerJo();
        assertNotNull(jo.id());
        assertEquals("jsmith", jo.username());
        assertNull(jo.phone());
        assertFalse(jo.passwordHash().contains("pass1234"));
        assertTrue(jo.passwordHash().startsWith("pbkdf2$"));
        assertTrue(PasswordHasher.verify("pass1234", daos.customerDao().findById(jo.id()).orElseThrow()
                .passwordHash()));
    }

    @Test
    void theUsernameIsTidiedAndTheNameAndPhoneAreKept() {
        Customer c = controller.register(" JSmith ", "pass1234", " Jo Smith ", " jo@x.ca ", " 403-555-0101 ");
        assertEquals("jsmith", c.username());
        assertEquals("Jo Smith", c.name());
        assertEquals("jo@x.ca", c.email());
        assertEquals("403-555-0101", c.phone());
    }

    @Test
    void thePasswordNeedsEightCharactersAndADigit() {
        assertTrue(messageOf(() -> controller.register("jsmith", "short1", "Jo", "jo@x.ca", "")).contains(
                "at least 8 characters"));
        assertTrue(messageOf(() -> controller.register("jsmith", "longpassword", "Jo", "jo@x.ca", "")).contains(
                "digit"));
        assertTrue(messageOf(() -> controller.register("jsmith", null, "Jo", "jo@x.ca", "")).contains(
                "at least 8 characters"));
        assertNotNull(controller.register("jsmith", "abcdefg1", "Jo", "jo@x.ca", "").id());
    }

    @Test
    void theOtherFieldsAreChecked() {
        assertTrue(messageOf(() -> controller.register("ab", "pass1234", "Jo", "jo@x.ca", "")).contains("Username"));
        assertTrue(messageOf(() -> controller.register("jsmith", "pass1234", " ", "jo@x.ca", "")).contains("Name"));
        assertTrue(messageOf(() -> controller.register("jsmith", "pass1234", "Jo", "a@b", "")).contains("Email"));
    }

    @Test
    void aDuplicateEmailIsRefusedWithAFriendlyMessage() throws SQLException {
        registerJo();
        int logins = count("app_user");
        String message = messageOf(() -> controller.register("another", "pass1234", "Jo Again", "JO@X.CA", ""));
        assertTrue(message.contains("Email already registered"), message);
        assertEquals(logins, count("app_user"));
        assertEquals(1, count("customer"));
    }

    @Test
    void aDuplicateUsernameIsRefusedWithAFriendlyMessage() throws SQLException {
        registerJo();
        int logins = count("app_user");
        String message = messageOf(() -> controller.register("JSmith", "pass1234", "Al", "al@x.ca", ""));
        assertTrue(message.contains("Username already taken"), message);
        assertEquals(logins, count("app_user"));
        assertEquals(1, count("customer"));
    }

    @Test
    void aUsernameTakenByAnAgentIsTakenForCustomersToo() {
        assertTrue(messageOf(() -> controller.register("agent1", "pass1234", "Al", "al@x.ca", ""))
                .contains("Username already taken"));
    }

    // ---- view

    @Test
    void aCustomerSeesThemselvesAnAgentSeesAnybodyAndAnotherCustomerIsRefused() {
        Customer jo = registerJo();
        Customer bo = registerBo();
        assertEquals("Jo Smith", controller.view(jo, jo.id()).name());
        assertEquals("Jo Smith", controller.view(agent, jo.id()).name());
        assertThrows(AccessDeniedException.class, () -> controller.view(bo, jo.id()));
        assertThrows(AccessDeniedException.class, () -> controller.view(admin, jo.id()));
        assertThrows(AccessDeniedException.class, () -> controller.view(null, jo.id()));
    }

    @Test
    void anUnknownCustomerIsAnError() {
        assertThrows(IllegalArgumentException.class, () -> controller.view(agent, 999));
    }

    // ---- update

    @Test
    void updateChangesTheNameAndThePhone() {
        Customer jo = registerJo();
        Customer changed = controller.update(jo, jo.id(), "Joanne Smith", "jo@x.ca", "403-555-0100");
        assertEquals("Joanne Smith", changed.name());
        Customer stored = daos.customerDao().findById(jo.id()).orElseThrow();
        assertEquals("Joanne Smith", stored.name());
        assertEquals("403-555-0100", stored.phone());
        assertEquals("jsmith", stored.username());
        assertTrue(PasswordHasher.verify("pass1234", stored.passwordHash()));
    }

    @Test
    void anAgentCanUpdateACustomerAndAPhoneCanBeTakenAway() {
        Customer bo = registerBo();
        controller.update(agent, bo.id(), "Bo Wong", "bo@x.ca", "");
        assertNull(daos.customerDao().findById(bo.id()).orElseThrow().phone());
    }

    @Test
    void anInvalidValueIsRefusedAndNothingChanges() {
        Customer jo = registerJo();
        assertTrue(messageOf(() -> controller.update(jo, jo.id(), "Jo Smith", "not-an-email", "")).contains("Email"));
        assertTrue(messageOf(() -> controller.update(jo, jo.id(), " ", "jo@x.ca", "")).contains("Name"));
        Customer stored = daos.customerDao().findById(jo.id()).orElseThrow();
        assertEquals("jo@x.ca", stored.email());
        assertEquals("Jo Smith", stored.name());
    }

    @Test
    void theEmailOfAnotherCustomerCannotBeTakenButYourOwnCanBeKept() {
        Customer jo = registerJo();
        registerBo();
        assertTrue(messageOf(() -> controller.update(jo, jo.id(), "Jo Smith", "BO@x.ca", ""))
                .contains("Email already registered"));
        assertEquals("jo@x.ca", controller.update(jo, jo.id(), "Jo S", "JO@x.ca", "").email().toLowerCase());
    }

    @Test
    void updateIsOnlyForTheCustomerThemselvesOrAnAgent() {
        Customer jo = registerJo();
        Customer bo = registerBo();
        assertThrows(AccessDeniedException.class, () -> controller.update(bo, jo.id(), "Hacked", "jo@x.ca", ""));
        assertThrows(AccessDeniedException.class, () -> controller.update(admin, jo.id(), "Hacked", "jo@x.ca", ""));
        assertEquals("Jo Smith", daos.customerDao().findById(jo.id()).orElseThrow().name());
        assertThrows(IllegalArgumentException.class, () -> controller.update(agent, 999, "X", "x@x.ca", ""));
    }

    // ---- search

    @Test
    void searchIsForAgentsOnly() {
        Customer jo = registerJo();
        registerBo();
        assertThrows(AccessDeniedException.class, () -> controller.search(jo, "smi"));
        assertThrows(AccessDeniedException.class, () -> controller.search(admin, "smi"));
        assertThrows(AccessDeniedException.class, () -> controller.search(null, "smi"));

        List<Customer> found = controller.search(agent, "smi");
        assertEquals(1, found.size());
        assertEquals("Jo Smith", found.get(0).name());
        assertTrue(controller.search(agent, "zzz").isEmpty());
        assertEquals(2, controller.search(agent, "").size());
    }
}
