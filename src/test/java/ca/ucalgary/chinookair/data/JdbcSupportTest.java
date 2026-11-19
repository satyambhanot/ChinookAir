package ca.ucalgary.chinookair.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JdbcSupportTest {

    private Connection db;

    @BeforeEach
    void open() {
        db = TestDatabase.inMemory();
    }

    @AfterEach
    void close() throws SQLException {
        db.close();
    }

    private void insertAirline(String code) {
        JdbcSupport.update(db, "INSERT INTO airline (code, name) VALUES (?, ?)", st -> {
            st.setString(1, code);
            st.setString(2, "Airline " + code);
        });
    }

    private int airlines() {
        return JdbcSupport.queryOne(db, "SELECT COUNT(*) FROM airline", JdbcSupport.NO_PARAMETERS,
                row -> row.getInt(1)).orElse(-1);
    }

    @Test
    void aTransactionKeepsItsWorkWhenItSucceeds() throws SQLException {
        String result = JdbcSupport.transaction(db, () -> {
            insertAirline("AC");
            insertAirline("WS");
            return "done";
        });
        assertEquals("done", result);
        assertEquals(2, airlines());
        assertTrue(db.getAutoCommit());
    }

    @Test
    void aTransactionUndoesEverythingWhenItFails() throws SQLException {
        assertThrows(IllegalStateException.class, () -> JdbcSupport.transaction(db, () -> {
            insertAirline("AC");
            throw new IllegalStateException("boom");
        }));
        assertEquals(0, airlines());
        assertTrue(db.getAutoCommit());
    }

    @Test
    void aTransactionInsideATransactionJoinsIt() throws SQLException {
        assertThrows(IllegalStateException.class, () -> JdbcSupport.transaction(db, () -> {
            insertAirline("AC");
            // this one would commit on its own if it did not join the outer one
            JdbcSupport.transaction(db, () -> {
                insertAirline("WS");
                return null;
            });
            JdbcSupport.batch(db, "INSERT INTO airline (code, name) VALUES (?, ?)", List.of(st -> {
                st.setString(1, "PD");
                st.setString(2, "Porter");
            }));
            throw new IllegalStateException("outer failure");
        }));
        assertEquals(0, airlines());
        assertTrue(db.getAutoCommit());
    }

    @Test
    void aFailingBatchRollsBackAllOfItsRows() {
        List<JdbcSupport.Binder> rows = List.of(
                st -> {
                    st.setString(1, "AC");
                    st.setString(2, "Air Canada");
                },
                st -> {
                    st.setString(1, "AC");
                    st.setString(2, "Duplicate");
                });
        assertThrows(DataAccessException.class,
                () -> JdbcSupport.batch(db, "INSERT INTO airline (code, name) VALUES (?, ?)", rows));
        assertEquals(0, airlines());
    }
}
