package ca.ucalgary.chinookair.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqliteAppStateDaoTest {

    private Connection db;
    private AppStateDao dao;

    @BeforeEach
    void open() {
        db = TestDatabase.inMemory();
        dao = new SqliteAppStateDao(db);
    }

    @AfterEach
    void close() throws SQLException {
        db.close();
    }

    private int rows() throws SQLException {
        try (Statement st = db.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM app_state")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    @Test
    void aKeyThatWasNeverSetIsEmpty() {
        assertTrue(dao.get("last_promo_month").isEmpty());
        assertTrue(dao.get(null).isEmpty());
    }

    @Test
    void aValueComesBackAsSaved() {
        dao.put("last_promo_month", "2026-12");
        assertEquals("2026-12", dao.get("last_promo_month").orElseThrow());
    }

    @Test
    void settingAKeyAgainOverwritesItWithStillOneRow() throws SQLException {
        dao.put("last_promo_month", "2026-12");
        dao.put("last_promo_month", "2027-01");
        assertEquals("2027-01", dao.get("last_promo_month").orElseThrow());
        assertEquals(1, rows());
    }

    @Test
    void keysAreIndependent() throws SQLException {
        dao.put("last_promo_month", "2026-12");
        dao.put("other", "x");
        assertEquals("2026-12", dao.get("last_promo_month").orElseThrow());
        assertEquals("x", dao.get("other").orElseThrow());
        assertEquals(2, rows());
    }

    @Test
    void aKeyAndAValueAreRequired() {
        assertThrows(IllegalArgumentException.class, () -> dao.put(null, "x"));
        assertThrows(IllegalArgumentException.class, () -> dao.put(" ", "x"));
        assertThrows(IllegalArgumentException.class, () -> dao.put("key", null));
    }

    @Test
    void anEmptyTextIsAValueToo() {
        dao.put("note", "");
        assertEquals("", dao.get("note").orElseThrow());
    }
}
