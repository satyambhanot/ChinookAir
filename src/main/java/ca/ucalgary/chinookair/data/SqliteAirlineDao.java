package ca.ucalgary.chinookair.data;

import ca.ucalgary.chinookair.business.entity.Airline;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

class SqliteAirlineDao implements AirlineDao {

    private final Connection connection;

    SqliteAirlineDao(Connection connection) {
        this.connection = connection;
    }

    @Override
    public List<Airline> findAll() {
        return JdbcSupport.query(connection, "SELECT code, name FROM airline ORDER BY code",
                JdbcSupport.NO_PARAMETERS, row -> new Airline(row.getString("code"), row.getString("name")));
    }

    @Override
    public Optional<Airline> findByCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return JdbcSupport.queryOne(connection, "SELECT code, name FROM airline WHERE code = ?",
                st -> st.setString(1, code.trim().toUpperCase()),
                row -> new Airline(row.getString("code"), row.getString("name")));
    }

    @Override
    public Airline save(Airline airline) {
        JdbcSupport.update(connection, "INSERT INTO airline (code, name) VALUES (?, ?)", st -> {
            st.setString(1, airline.code());
            st.setString(2, airline.name());
        });
        return airline;
    }
}
