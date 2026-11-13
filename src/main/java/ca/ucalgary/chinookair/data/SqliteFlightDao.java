package ca.ucalgary.chinookair.data;

import ca.ucalgary.chinookair.business.entity.Aircraft;
import ca.ucalgary.chinookair.business.entity.Airline;
import ca.ucalgary.chinookair.business.entity.Airport;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Route;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class SqliteFlightDao implements FlightDao {

    // one query builds the whole flight: route with both airports, airline and aircraft
    private static final String SELECT = "SELECT f.id, f.flight_number, f.departure, f.arrival, "
            + "f.base_price_cents, r.id AS route_id, o.code AS o_code, o.city AS o_city, d.code AS d_code, "
            + "d.city AS d_city, a.code AS airline_code, a.name AS airline_name, ac.id AS aircraft_id, "
            + "ac.model, ac.rows, ac.seats_per_row "
            + "FROM flight f JOIN route r ON r.id = f.route_id "
            + "JOIN airport o ON o.code = r.origin_code JOIN airport d ON d.code = r.destination_code "
            + "JOIN airline a ON a.code = f.airline_code JOIN aircraft ac ON ac.id = f.aircraft_id";

    private final Connection connection;

    SqliteFlightDao(Connection connection) {
        this.connection = connection;
    }

    private static Flight read(ResultSet row) throws SQLException {
        Route route = new Route(row.getLong("route_id"), new Airport(row.getString("o_code"), row.getString("o_city")),
                new Airport(row.getString("d_code"), row.getString("d_city")));
        Airline airline = new Airline(row.getString("airline_code"), row.getString("airline_name"));
        Aircraft aircraft = new Aircraft(row.getLong("aircraft_id"), row.getString("model"), row.getInt("rows"),
                row.getInt("seats_per_row"));
        return new Flight(row.getLong("id"), row.getString("flight_number"), route, airline, aircraft,
                DbDates.parse(row.getString("departure")), DbDates.parse(row.getString("arrival")),
                BigDecimal.valueOf(row.getLong("base_price_cents"), 2));
    }

    private static void checkSavedParts(Flight flight) {
        if (flight.route().id() == null || flight.aircraft().id() == null) {
            throw new IllegalArgumentException("Save the route and the aircraft before the flight");
        }
    }

    private static JdbcSupport.Binder fields(Flight flight) {
        return st -> {
            st.setString(1, flight.flightNumber());
            st.setLong(2, flight.route().id());
            st.setString(3, flight.airline().code());
            st.setLong(4, flight.aircraft().id());
            st.setString(5, DbDates.format(flight.departure()));
            st.setString(6, DbDates.format(flight.arrival()));
            st.setLong(7, flight.basePrice().movePointRight(2).longValueExact());
        };
    }

    @Override
    public Flight save(Flight flight) {
        if (flight.id() != null) {
            throw new IllegalArgumentException("This flight is already saved");
        }
        checkSavedParts(flight);
        long id = JdbcSupport.insert(connection, "INSERT INTO flight (flight_number, route_id, airline_code, "
                + "aircraft_id, departure, arrival, base_price_cents) VALUES (?, ?, ?, ?, ?, ?, ?)", fields(flight));
        return new Flight(id, flight.flightNumber(), flight.route(), flight.airline(), flight.aircraft(),
                flight.departure(), flight.arrival(), flight.basePrice());
    }

    @Override
    public void update(Flight flight) {
        if (flight.id() == null) {
            throw new IllegalArgumentException("This flight is not saved yet");
        }
        checkSavedParts(flight);
        int changed = JdbcSupport.update(connection, "UPDATE flight SET flight_number = ?, route_id = ?, "
                + "airline_code = ?, aircraft_id = ?, departure = ?, arrival = ?, base_price_cents = ? "
                + "WHERE id = ?", st -> {
                    fields(flight).bind(st);
                    st.setLong(8, flight.id());
                });
        requireOne(changed, flight.id());
    }

    @Override
    public void delete(long flightId) {
        requireOne(JdbcSupport.update(connection, "DELETE FROM flight WHERE id = ?", st -> st.setLong(1, flightId)),
                flightId);
    }

    @Override
    public void archive(long flightId) {
        requireOne(JdbcSupport.update(connection, "UPDATE flight SET archived = 1 WHERE id = ?",
                st -> st.setLong(1, flightId)), flightId);
    }

    private static void requireOne(int changed, long flightId) {
        if (changed != 1) {
            throw new DataAccessException("There is no flight with id " + flightId);
        }
    }

    @Override
    public Optional<Flight> findById(long flightId) {
        return JdbcSupport.queryOne(connection, SELECT + " WHERE f.id = ?", st -> st.setLong(1, flightId),
                SqliteFlightDao::read);
    }

    @Override
    public boolean existsByFlightNumber(String flightNumber) {
        if (flightNumber == null) {
            return false;
        }
        return JdbcSupport.queryOne(connection, "SELECT 1 FROM flight WHERE flight_number = ?",
                st -> st.setString(1, flightNumber.trim().toUpperCase()), row -> 1).isPresent();
    }

    @Override
    public List<Flight> search(String origin, String destination, LocalDate date, String airlineCodeOrNull) {
        if (origin == null || destination == null || date == null) {
            throw new IllegalArgumentException("An origin, a destination and a date are required");
        }
        boolean byAirline = airlineCodeOrNull != null && !airlineCodeOrNull.isBlank();
        String sql = SELECT + " WHERE o.code = ? AND d.code = ? AND f.departure >= ? AND f.departure < ? "
                + "AND f.archived = 0" + (byAirline ? " AND f.airline_code = ?" : "")
                + " ORDER BY f.departure, f.flight_number";
        return JdbcSupport.query(connection, sql, st -> {
            st.setString(1, origin.trim().toUpperCase());
            st.setString(2, destination.trim().toUpperCase());
            st.setString(3, DbDates.startOf(date));
            st.setString(4, DbDates.startOf(date.plusDays(1)));
            if (byAirline) {
                st.setString(5, airlineCodeOrNull.trim().toUpperCase());
            }
        }, SqliteFlightDao::read);
    }

    @Override
    public List<Flight> findDepartingBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("A start and an end are required");
        }
        return JdbcSupport.query(connection, SELECT + " WHERE f.departure >= ? AND f.departure < ? "
                + "AND f.archived = 0 ORDER BY f.departure, f.flight_number", st -> {
                    st.setString(1, DbDates.format(from));
                    st.setString(2, DbDates.format(to));
                }, SqliteFlightDao::read);
    }

    @Override
    public List<Flight> findByAircraft(long aircraftId) {
        return JdbcSupport.query(connection, SELECT + " WHERE f.aircraft_id = ? AND f.archived = 0 "
                + "ORDER BY f.departure", st -> st.setLong(1, aircraftId), SqliteFlightDao::read);
    }
}
