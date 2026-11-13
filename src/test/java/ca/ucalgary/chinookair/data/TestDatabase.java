package ca.ucalgary.chinookair.data;

import java.sql.Connection;

/** Gives tests a fresh, empty, fully set up database in memory. */
public final class TestDatabase {

    private TestDatabase() {
    }

    /** A new in-memory database with the schema and foreign keys on. Close the connection when done. */
    public static Connection inMemory() {
        return DatabaseConnection.forUrl("jdbc:sqlite::memory:").connection();
    }
}
