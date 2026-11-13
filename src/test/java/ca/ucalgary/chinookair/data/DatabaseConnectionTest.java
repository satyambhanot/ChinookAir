package ca.ucalgary.chinookair.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseConnectionTest {

    @TempDir
    Path folder;

    @AfterEach
    void putThePropertyBack() {
        // every test closes its own connection, this only removes the file setting
        System.clearProperty(DatabaseConnection.FILE_PROPERTY);
    }

    private Path useDatabaseFile(String relative) {
        Path file = folder.resolve(relative);
        System.setProperty(DatabaseConnection.FILE_PROPERTY, file.toString());
        return file;
    }

    private static String queryString(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    @Test
    void getInstanceReturnsTheSameObjectTwice() {
        useDatabaseFile("one.db");
        DatabaseConnection first = DatabaseConnection.getInstance();
        try {
            assertSame(first, DatabaseConnection.getInstance());
        } finally {
            first.close();
        }
    }

    @Test
    void theFolderTheFileAndTheTablesAreCreatedOnFirstUse() throws SQLException {
        Path file = useDatabaseFile("deeper/still/x.db");
        DatabaseConnection db = DatabaseConnection.getInstance();
        try {
            assertTrue(Files.isDirectory(file.getParent()));
            assertTrue(Files.exists(file));
            assertEquals("airline", queryString(db.connection(),
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='airline'"));
        } finally {
            db.close();
        }
    }

    @Test
    void dataSurvivesCloseAndReopenAndTheSchemaIsNotAppliedTwice() throws SQLException {
        useDatabaseFile("keep.db");
        DatabaseConnection first = DatabaseConnection.getInstance();
        try (Statement st = first.connection().createStatement()) {
            st.execute("INSERT INTO airline VALUES ('AC', 'Air Canada')");
        }
        assertEquals("1", queryString(first.connection(), "PRAGMA user_version"));
        first.close();

        DatabaseConnection second = DatabaseConnection.getInstance();
        try {
            assertNotSame(first, second);
            assertEquals("Air Canada", queryString(second.connection(), "SELECT name FROM airline WHERE code='AC'"));
            assertEquals("1", queryString(second.connection(), "PRAGMA user_version"));
        } finally {
            second.close();
        }
    }

    @Test
    void foreignKeysAreOn() throws SQLException {
        useDatabaseFile("fk.db");
        DatabaseConnection db = DatabaseConnection.getInstance();
        try {
            assertEquals("1", queryString(db.connection(), "PRAGMA foreign_keys"));
        } finally {
            db.close();
        }
    }

    @Test
    void inMemoryTestDatabaseHasAllTablesAndForeignKeys() throws SQLException {
        try (Connection c = TestDatabase.inMemory()) {
            for (String table : new String[] {"airline", "airport", "aircraft", "route", "flight", "flight_seat",
                "app_user", "customer", "reservation", "payment", "promotion", "app_state"}) {
                assertEquals(table, queryString(c,
                        "SELECT name FROM sqlite_master WHERE type='table' AND name='" + table + "'"));
            }
            assertEquals("1", queryString(c, "PRAGMA foreign_keys"));
        }
    }

    @Test
    void everyInMemoryDatabaseIsItsOwn() throws SQLException {
        try (Connection a = TestDatabase.inMemory(); Connection b = TestDatabase.inMemory()) {
            try (Statement st = a.createStatement()) {
                st.execute("INSERT INTO airline VALUES ('AC', 'Air Canada')");
            }
            assertNull(queryString(b, "SELECT name FROM airline"));
        }
    }

    @Test
    void anUnusableFolderGivesADataAccessException() throws IOException {
        Path notAFolder = Files.createFile(folder.resolve("plain-file"));
        useDatabaseFile("plain-file/inside/x.db");
        assertTrue(Files.exists(notAFolder));
        assertThrows(DataAccessException.class, DatabaseConnection::getInstance);
    }
}
