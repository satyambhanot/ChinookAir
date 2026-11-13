package ca.ucalgary.chinookair.data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Gets a connection ready: turns foreign keys on and, on a brand new database
 * file, creates the tables. The schema version is kept in SQLite's user_version.
 */
final class DatabaseInitializer {

    static final int SCHEMA_VERSION = 1;
    private static final String SCHEMA = "/db/schema.sql";

    private DatabaseInitializer() {
    }

    static void initialize(Connection connection) {
        try {
            // foreign keys are off by default and only count for this connection
            execute(connection, "PRAGMA foreign_keys=ON");
            int version = userVersion(connection);
            if (version == 0) {
                createSchema(connection);
            } else if (version > SCHEMA_VERSION) {
                throw new DataAccessException("The database was made by a newer version of ChinookAir");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not prepare the database", e);
        }
    }

    private static void createSchema(Connection connection) throws SQLException {
        List<String> statements = schemaStatements();
        connection.setAutoCommit(false);
        try {
            for (String sql : statements) {
                execute(connection, sql);
            }
            execute(connection, "PRAGMA user_version=" + SCHEMA_VERSION);
            connection.commit();
        } catch (SQLException | RuntimeException e) {
            // the tables are all created or none are
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    static int userVersion(Connection connection) throws SQLException {
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery("PRAGMA user_version")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute(sql);
        }
    }

    /** The script without its comment lines, cut into single statements. */
    static List<String> schemaStatements() {
        String text;
        try (InputStream in = DatabaseInitializer.class.getResourceAsStream(SCHEMA)) {
            if (in == null) {
                throw new DataAccessException("The schema " + SCHEMA + " is missing from the program");
            }
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DataAccessException("Could not read the schema " + SCHEMA);
        }
        StringBuilder script = new StringBuilder();
        for (String line : text.split("\n")) {
            if (!line.trim().startsWith("--")) {
                script.append(line).append('\n');
            }
        }
        List<String> statements = new ArrayList<>();
        for (String part : script.toString().split(";")) {
            if (!part.isBlank()) {
                statements.add(part.trim());
            }
        }
        return statements;
    }
}
