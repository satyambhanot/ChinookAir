package ca.ucalgary.chinookair.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;

class DatabaseInitializerTest {

    private static int count(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static int tables(Connection c) throws SQLException {
        return count(c, "SELECT COUNT(*) FROM sqlite_master WHERE type='table'");
    }

    @Test
    void aNewDatabaseGetsTheSchemaAndVersionOne() throws SQLException {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            assertEquals(0, DatabaseInitializer.userVersion(c));
            DatabaseInitializer.initialize(c);
            assertEquals(1, DatabaseInitializer.userVersion(c));
            assertEquals(12, tables(c));
        }
    }

    @Test
    void runningItAgainChangesNothing() throws SQLException {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            DatabaseInitializer.initialize(c);
            try (Statement st = c.createStatement()) {
                st.execute("INSERT INTO airline VALUES ('AC', 'Air Canada')");
            }
            DatabaseInitializer.initialize(c);
            assertEquals(1, count(c, "SELECT COUNT(*) FROM airline"));
            assertEquals(1, DatabaseInitializer.userVersion(c));
        }
    }

    @Test
    void aDatabaseFromANewerVersionIsRefused() throws SQLException {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement st = c.createStatement()) {
                st.execute("PRAGMA user_version=2");
            }
            assertThrows(DataAccessException.class, () -> DatabaseInitializer.initialize(c));
            assertEquals(0, tables(c));
        }
    }

    @Test
    void aFailedSetupLeavesNoHalfBuiltDatabase() throws SQLException {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement st = c.createStatement()) {
                // a table with a schema name in the way makes the script fail part of the way through
                st.execute("CREATE TABLE route (x INTEGER)");
            }
            assertThrows(DataAccessException.class, () -> DatabaseInitializer.initialize(c));
            assertEquals(0, DatabaseInitializer.userVersion(c));
            assertEquals(1, tables(c));
            assertTrue(c.getAutoCommit());
        }
    }

    @Test
    void theScriptIsCutIntoStatementsWithoutComments() {
        List<String> statements = DatabaseInitializer.schemaStatements();
        assertTrue(statements.size() > 12);
        assertTrue(statements.stream().allMatch(s -> !s.startsWith("--") && !s.isBlank()));
        assertFalse(statements.stream().anyMatch(s -> s.contains(";")));
    }
}
