package ca.ucalgary.chinookair.data;

import ca.ucalgary.chinookair.business.entity.Aircraft;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

class SqliteAircraftDao implements AircraftDao {

    private static final String COLUMNS = "SELECT id, model, rows, seats_per_row FROM aircraft";

    private final Connection connection;

    SqliteAircraftDao(Connection connection) {
        this.connection = connection;
    }

    private static Aircraft read(ResultSet row) throws SQLException {
        return new Aircraft(row.getLong("id"), row.getString("model"), row.getInt("rows"),
                row.getInt("seats_per_row"));
    }

    @Override
    public List<Aircraft> findAll() {
        return JdbcSupport.query(connection, COLUMNS + " ORDER BY id", JdbcSupport.NO_PARAMETERS,
                SqliteAircraftDao::read);
    }

    @Override
    public Optional<Aircraft> findById(long id) {
        return JdbcSupport.queryOne(connection, COLUMNS + " WHERE id = ?", st -> st.setLong(1, id),
                SqliteAircraftDao::read);
    }

    @Override
    public Aircraft save(Aircraft aircraft) {
        if (aircraft.id() != null) {
            throw new IllegalArgumentException("This aircraft is already saved");
        }
        long id = JdbcSupport.insert(connection,
                "INSERT INTO aircraft (model, rows, seats_per_row) VALUES (?, ?, ?)", st -> {
                    st.setString(1, aircraft.model());
                    st.setInt(2, aircraft.rows());
                    st.setInt(3, aircraft.seatsPerRow());
                });
        return new Aircraft(id, aircraft.model(), aircraft.rows(), aircraft.seatsPerRow());
    }
}
