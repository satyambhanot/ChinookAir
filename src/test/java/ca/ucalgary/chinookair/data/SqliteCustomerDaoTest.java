package ca.ucalgary.chinookair.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Role;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqliteCustomerDaoTest {

    private static final String HASH = "pbkdf2$1$AAAA$BBBB";

    private Connection db;
    private CustomerDao dao;

    @BeforeEach
    void open() {
        db = TestDatabase.inMemory();
        dao = new SqliteCustomerDao(db);
    }

    @AfterEach
    void close() throws SQLException {
        db.close();
    }

    private static Customer customer(String username, String name, String email) {
        return new Customer(null, username, HASH, name, email, null);
    }

    private int count(String table) throws SQLException {
        try (Statement st = db.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    @Test
    void aSavedCustomerGetsAnIdAndCanBeFoundByIdAndEmail() {
        Customer saved = dao.save(new Customer(null, "jsmith", HASH, "Jo Smith", "j@x.ca", "403-555-0101"));
        assertNotNull(saved.id());
        Customer found = dao.findById(saved.id()).orElseThrow();
        assertEquals("jsmith", found.username());
        assertEquals("Jo Smith", found.name());
        assertEquals("403-555-0101", found.phone());
        assertEquals(Role.CUSTOMER, found.role());
        assertEquals(HASH, found.passwordHash());
        assertEquals(saved.id(), dao.findByEmail("J@X.CA").orElseThrow().id());
        assertTrue(dao.findById(99).isEmpty());
        assertTrue(dao.findByEmail("nobody@x.ca").isEmpty());
        assertTrue(dao.findByEmail(null).isEmpty());
    }

    @Test
    void theLoginAndTheProfileAreSavedTogether() throws SQLException {
        dao.save(customer("jsmith", "Jo Smith", "j@x.ca"));
        assertEquals(1, count("app_user"));
        assertEquals(1, count("customer"));
    }

    @Test
    void aSecondCustomerWithTheSameEmailIsRefusedAndLeavesNoRows() throws SQLException {
        dao.save(customer("jsmith", "Jo Smith", "j@x.ca"));
        DataAccessException e = assertThrows(DataAccessException.class,
                () -> dao.save(customer("another", "Jo Again", "J@X.CA")));
        assertTrue(e.getMessage().contains("email"), e.getMessage());
        assertEquals(1, count("app_user"));
        assertEquals(1, count("customer"));
    }

    @Test
    void aSecondCustomerWithTheSameUsernameIsRefused() throws SQLException {
        dao.save(customer("jsmith", "Jo Smith", "j@x.ca"));
        DataAccessException e = assertThrows(DataAccessException.class,
                () -> dao.save(customer("JSmith", "Al Smith", "a@x.ca")));
        assertTrue(e.getMessage().contains("username"), e.getMessage());
        assertEquals(1, count("app_user"));
        assertEquals(1, count("customer"));
    }

    @Test
    void autoCommitIsBackOnAfterASaveEvenWhenItFails() throws SQLException {
        dao.save(customer("jsmith", "Jo Smith", "j@x.ca"));
        assertThrows(DataAccessException.class, () -> dao.save(customer("jsmith", "Jo", "z@x.ca")));
        assertTrue(db.getAutoCommit());
    }

    @Test
    void searchFindsByNameOrEmailWithoutCase() {
        dao.save(customer("jsmith", "Jo Smith", "j@x.ca"));
        dao.save(customer("bwong", "Bo Wong", "bo@example.com"));
        assertEquals(List.of("jsmith"), names(dao.search("smi")));
        assertEquals(List.of("jsmith"), names(dao.search("SMITH")));
        assertEquals(List.of("jsmith"), names(dao.search("J@X")));
        assertEquals(List.of("bwong"), names(dao.search("example")));
        assertTrue(dao.search("zzz").isEmpty());
    }

    @Test
    void searchSortsByNameAndBlankFindsEverybody() {
        dao.save(customer("bwong", "Bo Wong", "bo@x.ca"));
        dao.save(customer("asmith", "Al Smith", "al@x.ca"));
        assertEquals(List.of("asmith", "bwong"), names(dao.search("")));
        assertEquals(List.of("asmith", "bwong"), names(dao.search(null)));
        assertEquals(List.of("asmith", "bwong"), names(dao.search("  ")));
    }

    @Test
    void aPercentOrUnderscoreInTheSearchIsPlainText() {
        dao.save(customer("jsmith", "Jo Smith", "j@x.ca"));
        dao.save(customer("underscore", "Jo_Doe", "jd@x.ca"));
        dao.save(customer("space", "Jo Doe", "jo.doe@x.ca"));
        assertTrue(dao.search("%").isEmpty());
        // as a wildcard "o_d" would also match "Jo Doe"
        assertEquals(List.of("underscore"), names(dao.search("o_d")));
    }

    @Test
    void updateChangesNameEmailAndPhoneButNotTheLogin() {
        Customer saved = dao.save(customer("jsmith", "Jo Smith", "j@x.ca"));
        dao.update(new Customer(saved.id(), "jsmith", HASH, "Joanne Smith", "joanne@x.ca", "403-555-0100"));
        Customer found = dao.findById(saved.id()).orElseThrow();
        assertEquals("Joanne Smith", found.name());
        assertEquals("joanne@x.ca", found.email());
        assertEquals("403-555-0100", found.phone());
        assertEquals("jsmith", found.username());
    }

    @Test
    void updateCanTakeThePhoneAway() {
        Customer saved = dao.save(new Customer(null, "jsmith", HASH, "Jo", "j@x.ca", "403-555-0100"));
        dao.update(new Customer(saved.id(), "jsmith", HASH, "Jo", "j@x.ca", null));
        assertNull(dao.findById(saved.id()).orElseThrow().phone());
    }

    @Test
    void updateCannotTakeAnEmailOfAnotherCustomer() {
        dao.save(customer("asmith", "Al Smith", "al@x.ca"));
        Customer jo = dao.save(customer("jsmith", "Jo Smith", "j@x.ca"));
        assertThrows(DataAccessException.class,
                () -> dao.update(new Customer(jo.id(), "jsmith", HASH, "Jo Smith", "AL@x.ca", null)));
        assertEquals("j@x.ca", dao.findById(jo.id()).orElseThrow().email());
    }

    @Test
    void updateNeedsASavedCustomer() {
        assertThrows(IllegalArgumentException.class, () -> dao.update(customer("jsmith", "Jo Smith", "j@x.ca")));
        assertThrows(DataAccessException.class,
                () -> dao.update(new Customer(99L, "jsmith", HASH, "Jo", "j@x.ca", null)));
    }

    @Test
    void aCustomerThatIsAlreadySavedCannotBeSavedAgain() {
        Customer saved = dao.save(customer("jsmith", "Jo Smith", "j@x.ca"));
        assertThrows(IllegalArgumentException.class, () -> dao.save(saved));
    }

    @Test
    void customerSearchIgnoresAgentsAndAdministrators() throws SQLException {
        dao.save(customer("jsmith", "Jo Smith", "j@x.ca"));
        try (Statement st = db.createStatement()) {
            st.execute("INSERT INTO app_user (username, password_hash, role) VALUES ('smith.admin', 'h', 'ADMIN')");
        }
        assertEquals(1, dao.search("smi").size());
        assertTrue(dao.findById(2).isEmpty());
    }

    private static List<String> names(List<Customer> customers) {
        return customers.stream().map(Customer::username).toList();
    }
}
