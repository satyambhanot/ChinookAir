package ca.ucalgary.chinookair.data;

import ca.ucalgary.chinookair.business.entity.Airport;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

class SqliteAirportDao implements AirportDao {

    private final Connection connection;

    SqliteAirportDao(Connection connection) {
        this.connection = connection;
    }

    @Override
    public List<Airport> findAll() {
        return JdbcSupport.query(connection, "SELECT code, city FROM airport ORDER BY code",
                JdbcSupport.NO_PARAMETERS, row -> new Airport(row.getString("code"), row.getString("city")));
    }

    @Override
    public Optional<Airport> findByCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return JdbcSupport.queryOne(connection, "SELECT code, city FROM airport WHERE code = ?",
                st -> st.setString(1, code.trim().toUpperCase()),
                row -> new Airport(row.getString("code"), row.getString("city")));
    }

    @Override
    public Airport save(Airport airport) {
        JdbcSupport.update(connection, "INSERT INTO airport (code, city) VALUES (?, ?)", st -> {
            st.setString(1, airport.code());
            st.setString(2, airport.city());
        });
        return airport;
    }
}
