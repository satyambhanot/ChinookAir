package ca.ucalgary.chinookair.data;

import static ca.ucalgary.chinookair.data.TestFixtures.airline;
import static ca.ucalgary.chinookair.data.TestFixtures.airport;
import static ca.ucalgary.chinookair.data.TestFixtures.route;

import ca.ucalgary.chinookair.business.entity.Aircraft;
import ca.ucalgary.chinookair.business.entity.Airline;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Route;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

/**
 * The reference data that flight tests build on, already saved: three airports, three airlines,
 * two aircraft and two routes. It also inserts a customer and reservations with plain SQL,
 * so flight and seat tests do not depend on the reservation DAO.
 */
final class TestWorld {

    final Connection db;
    final Route yycYvr;
    final Route yycYeg;
    final Airline ac;
    final Airline ws;
    final Airline pd;
    /** 3 rows of 4 seats. */
    final Aircraft small;
    final Aircraft big;

    TestWorld(Connection db) {
        this.db = db;
        AirportDao airports = new SqliteAirportDao(db);
        airports.save(airport("YYC"));
        airports.save(airport("YVR"));
        airports.save(airport("YEG"));
        AirlineDao airlines = new SqliteAirlineDao(db);
        ac = airlines.save(airline("AC"));
        ws = airlines.save(airline("WS"));
        pd = airlines.save(airline("PD"));
        AircraftDao aircraft = new SqliteAircraftDao(db);
        small = aircraft.save(new Aircraft(null, "Small", 3, 4));
        big = aircraft.save(new Aircraft(null, "A320", 30, 6));
        RouteDao routes = new SqliteRouteDao(db);
        yycYvr = routes.save(route("YYC", "YVR"));
        yycYeg = routes.save(route("YYC", "YEG"));
        run("INSERT INTO app_user VALUES (1, 'jsmith', 'hash', 'CUSTOMER')");
        run("INSERT INTO customer VALUES (1, 'Jo Smith', 'jo@x.ca', NULL)");
    }

    /** A two hour flight at 200.00 that is not saved yet. */
    Flight flight(String number, Route route, Airline airline, Aircraft aircraft, LocalDateTime departure) {
        return new Flight(null, number, route, airline, aircraft, departure, departure.plusHours(2),
                new BigDecimal("200.00"));
    }

    /** Inserts a reservation of the first customer with plain SQL and returns its id. A null seat means none yet. */
    long reserve(String confirmationNumber, long flightId, String seat, String status) {
        return reserveFor(1, confirmationNumber, flightId, seat, status);
    }

    /** Inserts a reservation of the given customer with plain SQL and returns its id. */
    long reserveFor(long customerId, String confirmationNumber, long flightId, String seat, String status) {
        String seatValue = seat == null ? "NULL" : "'" + seat + "'";
        String priceValue = seat == null ? "NULL" : "20000";
        run("INSERT INTO reservation (confirmation_number, customer_id, flight_id, seat_no, price_cents, status, "
                + "created_at) VALUES ('" + confirmationNumber + "', " + customerId + ", " + flightId + ", "
                + seatValue + ", " + priceValue + ", '" + status + "', '2026-11-20T09:00:00')");
        try (Statement st = db.createStatement(); var rs = st.executeQuery("SELECT last_insert_rowid()")) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    void setStatus(long reservationId, String status) {
        run("UPDATE reservation SET status = '" + status + "' WHERE id = " + reservationId);
    }

    private void run(String sql) {
        try (Statement st = db.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
