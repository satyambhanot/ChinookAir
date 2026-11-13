package ca.ucalgary.chinookair.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The JDBC boilerplate that every DAO would otherwise repeat: prepare the
 * statement, bind the parameters, read the rows and turn a SQLException into
 * a DataAccessException. Always uses a PreparedStatement, never string joining.
 */
final class JdbcSupport {

    /** Puts the parameters into the statement. */
    @FunctionalInterface
    interface Binder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    /** Turns the current row into an object. */
    @FunctionalInterface
    interface RowMapper<T> {
        T map(ResultSet row) throws SQLException;
    }

    static final Binder NO_PARAMETERS = statement -> { };

    private JdbcSupport() {
    }

    static <T> List<T> query(Connection connection, String sql, Binder binder, RowMapper<T> mapper) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet rows = statement.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rows.next()) {
                    result.add(mapper.map(rows));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Query failed: " + e.getMessage(), e);
        }
    }

    static <T> Optional<T> queryOne(Connection connection, String sql, Binder binder, RowMapper<T> mapper) {
        List<T> rows = query(connection, sql, binder, mapper);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /** Runs an INSERT, UPDATE or DELETE and returns how many rows it changed. */
    static int update(Connection connection, String sql, Binder binder) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Update failed: " + e.getMessage(), e);
        }
    }

    /** Runs an INSERT and returns the id the database gave the new row. */
    static long insert(Connection connection, String sql, Binder binder) {
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binder.bind(statement);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DataAccessException("The database gave no id for the new row");
                }
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Insert failed: " + e.getMessage(), e);
        }
    }

    /**
     * Runs the work in one transaction: everything it does is kept, or nothing is. Any exception
     * undoes the work and is passed on. Inside a running transaction it joins that one instead.
     */
    static <T> T transaction(Connection connection, Supplier<T> work) {
        try {
            boolean before = connection.getAutoCommit();
            if (!before) {
                // already inside a transaction: join it, the outer one decides to keep or undo
                return work.get();
            }
            connection.setAutoCommit(false);
            try {
                T result = work.get();
                connection.commit();
                return result;
            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(before);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Transaction failed: " + e.getMessage(), e);
        }
    }

    /** Runs the same INSERT or UPDATE once for every binder, all in one transaction: all of them or none. */
    static void batch(Connection connection, String sql, List<Binder> binders) {
        transaction(connection, () -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (Binder binder : binders) {
                    binder.bind(statement);
                    statement.addBatch();
                }
                statement.executeBatch();
                return null;
            } catch (SQLException e) {
                throw new DataAccessException("Batch failed: " + e.getMessage(), e);
            }
        });
    }
}
