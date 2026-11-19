package ca.ucalgary.chinookair.data;

import ca.ucalgary.chinookair.business.entity.Airport;
import ca.ucalgary.chinookair.business.entity.Route;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

class SqliteRouteDao implements RouteDao {

    // the airports are joined in, so a Route comes back with both of them complete
    private static final String SELECT = "SELECT r.id, o.code AS o_code, o.city AS o_city, "
            + "d.code AS d_code, d.city AS d_city FROM route r "
            + "JOIN airport o ON o.code = r.origin_code JOIN airport d ON d.code = r.destination_code";

    private final Connection connection;

    SqliteRouteDao(Connection connection) {
        this.connection = connection;
    }

    private static Route read(ResultSet row) throws SQLException {
        return new Route(row.getLong("id"), new Airport(row.getString("o_code"), row.getString("o_city")),
                new Airport(row.getString("d_code"), row.getString("d_city")));
    }

    @Override
    public List<Route> findAll() {
        return JdbcSupport.query(connection, SELECT + " ORDER BY o.code, d.code", JdbcSupport.NO_PARAMETERS,
                SqliteRouteDao::read);
    }

    @Override
    public Optional<Route> findById(long id) {
        return JdbcSupport.queryOne(connection, SELECT + " WHERE r.id = ?", st -> st.setLong(1, id),
                SqliteRouteDao::read);
    }

    @Override
    public Optional<Route> findByAirports(String originCode, String destinationCode) {
        if (originCode == null || destinationCode == null) {
            return Optional.empty();
        }
        return JdbcSupport.queryOne(connection, SELECT + " WHERE r.origin_code = ? AND r.destination_code = ?",
                st -> {
                    st.setString(1, originCode.trim().toUpperCase());
                    st.setString(2, destinationCode.trim().toUpperCase());
                }, SqliteRouteDao::read);
    }

    @Override
    public Route save(Route route) {
        if (route.id() != null) {
            throw new IllegalArgumentException("This route is already saved");
        }
        long id = JdbcSupport.insert(connection,
                "INSERT INTO route (origin_code, destination_code) VALUES (?, ?)", st -> {
                    st.setString(1, route.origin().code());
                    st.setString(2, route.destination().code());
                });
        return new Route(id, route.origin(), route.destination());
    }
}
