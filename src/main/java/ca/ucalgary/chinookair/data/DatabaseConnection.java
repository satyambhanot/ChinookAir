package ca.ucalgary.chinookair.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * The one connection to the database file (Singleton). It is opened on the first
 * call to {@link #getInstance()} and the file and its tables are created if they
 * are missing. Only the composition root calls getInstance; everything else gets
 * a Connection through its constructor.
 */
public final class DatabaseConnection {

    /** System property that moves the database file, for tests and demos. */
    public static final String FILE_PROPERTY = "chinookair.db";

    private static DatabaseConnection instance;

    private final Connection connection;

    private DatabaseConnection(String url) {
        try {
            connection = DriverManager.getConnection(url);
        } catch (SQLException e) {
            throw new DataAccessException("Could not open the database", e);
        }
        try {
            DatabaseInitializer.initialize(connection);
        } catch (RuntimeException e) {
            closeQuietly(connection);
            throw e;
        }
    }

    /** The shared instance, opened on first use. */
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection("jdbc:sqlite:" + prepareFile(databaseFile()));
        }
        return instance;
    }

    /** A separate instance for one URL, so tests do not touch the singleton. */
    static DatabaseConnection forUrl(String url) {
        return new DatabaseConnection(url);
    }

    public Connection connection() {
        return connection;
    }

    /** Closes the connection. The next getInstance opens it again. */
    public void close() {
        synchronized (DatabaseConnection.class) {
            if (instance == this) {
                instance = null;
            }
        }
        try {
            connection.close();
        } catch (SQLException e) {
            throw new DataAccessException("Could not close the database", e);
        }
    }

    private static Path databaseFile() {
        String custom = System.getProperty(FILE_PROPERTY);
        if (custom != null && !custom.isBlank()) {
            return Paths.get(custom);
        }
        return Paths.get(System.getProperty("user.home"), ".chinookair", "chinookair.db");
    }

    private static Path prepareFile(Path file) {
        Path absolute = file.toAbsolutePath();
        try {
            Path folder = absolute.getParent();
            if (folder != null) {
                Files.createDirectories(folder);
            }
        } catch (IOException e) {
            throw new DataAccessException("Could not create the folder for the database: " + absolute.getParent());
        }
        return absolute;
    }

    private static void closeQuietly(Connection connection) {
        try {
            connection.close();
        } catch (SQLException e) {
            // already failing with a better message
        }
    }
}
