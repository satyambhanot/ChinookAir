package ca.ucalgary.chinookair.data;

import java.sql.Connection;
import java.util.Optional;

class SqliteAppStateDao implements AppStateDao {

    private final Connection connection;

    SqliteAppStateDao(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<String> get(String key) {
        if (key == null) {
            return Optional.empty();
        }
        return JdbcSupport.queryOne(connection, "SELECT value FROM app_state WHERE key = ?",
                st -> st.setString(1, key), row -> row.getString("value"));
    }

    @Override
    public void put(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("A key is required");
        }
        if (value == null) {
            throw new IllegalArgumentException("A value is required");
        }
        JdbcSupport.update(connection, "INSERT INTO app_state (key, value) VALUES (?, ?) "
                + "ON CONFLICT(key) DO UPDATE SET value = excluded.value", st -> {
                    st.setString(1, key);
                    st.setString(2, value);
                });
    }
}
