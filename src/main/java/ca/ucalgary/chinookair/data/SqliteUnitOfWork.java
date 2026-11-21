package ca.ucalgary.chinookair.data;

import java.sql.Connection;
import java.util.function.Supplier;

/** The SQLite way to keep several changes together: one transaction on the shared connection. */
final class SqliteUnitOfWork implements UnitOfWork {

    private final Connection connection;

    SqliteUnitOfWork(Connection connection) {
        this.connection = connection;
    }

    @Override
    public <T> T call(Supplier<T> work) {
        return JdbcSupport.transaction(connection, work);
    }
}
