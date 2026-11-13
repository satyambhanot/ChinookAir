package ca.ucalgary.chinookair.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SchemaTest {

    private Connection db;

    @BeforeEach
    void createDatabase() throws Exception {
        db = DriverManager.getConnection("jdbc:sqlite::memory:");
        run("PRAGMA foreign_keys=ON");
        for (String sql : schemaStatements()) {
            run(sql);
        }
        // the small world every test starts from
        run("INSERT INTO airline VALUES ('AC', 'Air Canada')");
        run("INSERT INTO airport VALUES ('YYC', 'Calgary'), ('YVR', 'Vancouver')");
        run("INSERT INTO aircraft VALUES (1, 'A320', 30, 6)");
        run("INSERT INTO route VALUES (1, 'YYC', 'YVR')");
        addFlight(1, "AC123");
        addFlight(2, "AC456");
        run("INSERT INTO app_user VALUES (1, 'jsmith', 'hash', 'CUSTOMER'), (2, 'asmith', 'hash', 'CUSTOMER')");
        run("INSERT INTO customer VALUES (1, 'Jo Smith', 'jo@x.ca', NULL), (2, 'Al Smith', 'al@x.ca', '403-555-0101')");
    }

    @AfterEach
    void closeDatabase() throws SQLException {
        db.close();
    }

    // ---- helpers

    private static List<String> schemaStatements() throws IOException {
        try (InputStream in = SchemaTest.class.getResourceAsStream("/db/schema.sql")) {
            assertTrue(in != null, "db/schema.sql is on the classpath");
            StringBuilder script = new StringBuilder();
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                if (!line.trim().startsWith("--")) {
                    script.append(line).append('\n');
                }
            }
            List<String> statements = new ArrayList<>();
            for (String part : script.toString().split(";")) {
                if (!part.isBlank()) {
                    statements.add(part.trim());
                }
            }
            return statements;
        }
    }

    private void run(String sql) throws SQLException {
        try (Statement st = db.createStatement()) {
            st.execute(sql);
        }
    }

    private void addFlight(int id, String number) throws SQLException {
        run("INSERT INTO flight (id, flight_number, route_id, airline_code, aircraft_id, departure, arrival, "
                + "base_price_cents) VALUES (" + id + ", '" + number + "', 1, 'AC', 1, "
                + "'2026-12-01T08:00:00', '2026-12-01T10:00:00', 20000)");
        for (String seat : new String[] {"1A", "1B", "2A", "3A"}) {
            run("INSERT INTO flight_seat VALUES (" + id + ", '" + seat + "', "
                    + (seat.startsWith("3") ? "'ECONOMY'" : "'BUSINESS'") + ")");
        }
    }

    private void reserve(String number, int customer, int flight, String seat, String status) throws SQLException {
        String seatValue = seat == null ? "NULL" : "'" + seat + "'";
        String priceValue = seat == null ? "NULL" : "20000";
        run("INSERT INTO reservation (confirmation_number, customer_id, flight_id, seat_no, price_cents, status, "
                + "created_at) VALUES ('" + number + "', " + customer + ", " + flight + ", " + seatValue + ", "
                + priceValue + ", '" + status + "', '2026-11-20T09:00:00')");
    }

    private void assertRefused(String sql) {
        SQLException e = assertThrows(SQLException.class, () -> run(sql), sql);
        assertTrue(e.getMessage().contains("SQLITE_CONSTRAINT"), e.getMessage());
    }

    private int count(String table) throws SQLException {
        try (Statement st = db.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    // ---- the tests

    @ParameterizedTest
    @ValueSource(strings = {"airline", "airport", "aircraft", "route", "flight", "flight_seat", "app_user",
        "customer", "reservation", "payment", "promotion", "app_state"})
    void everyTableExists(String table) throws SQLException {
        try (Statement st = db.createStatement();
             ResultSet rs = st.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + table
                     + "'")) {
            assertTrue(rs.next(), table);
        }
    }

    @Test
    void theActiveSeatIndexExists() throws SQLException {
        try (Statement st = db.createStatement();
             ResultSet rs = st.executeQuery("SELECT name FROM sqlite_master WHERE type='index' "
                     + "AND name='one_active_reservation_per_seat'")) {
            assertTrue(rs.next());
        }
    }

    // ---- double booking

    @Test
    void twoConfirmedReservationsCannotHoldTheSameSeat() throws SQLException {
        reserve("CA000001", 1, 1, "1A", "CONFIRMED");
        assertRefused("INSERT INTO reservation (confirmation_number, customer_id, flight_id, seat_no, price_cents, "
                + "status, created_at) VALUES ('CA000002', 2, 1, '1A', 20000, 'CONFIRMED', '2026-11-20T09:05:00')");
        assertEquals(1, count("reservation"));
    }

    @Test
    void aPendingAndAConfirmedReservationCannotShareASeatEither() throws SQLException {
        reserve("CA000001", 1, 1, "1A", "PENDING_PAYMENT");
        assertRefused("INSERT INTO reservation (confirmation_number, customer_id, flight_id, seat_no, price_cents, "
                + "status, created_at) VALUES ('CA000002', 2, 1, '1A', 20000, 'CONFIRMED', '2026-11-20T09:05:00')");
    }

    @Test
    void aSeatIsFreeAgainAfterTheReservationIsCancelled() throws SQLException {
        reserve("CA000001", 1, 1, "1A", "CANCELLED");
        reserve("CA000002", 2, 1, "1A", "CONFIRMED");
        assertEquals(2, count("reservation"));
    }

    @Test
    void manyCancelledReservationsMayShareASeat() throws SQLException {
        reserve("CA000001", 1, 1, "1A", "CANCELLED");
        reserve("CA000002", 2, 1, "1A", "CANCELLED");
        reserve("CA000003", 1, 1, "1A", "CANCELLED");
        assertEquals(3, count("reservation"));
    }

    @Test
    void theSameSeatNumberOnAnotherFlightIsAnotherSeat() throws SQLException {
        reserve("CA000001", 1, 1, "1A", "CONFIRMED");
        reserve("CA000002", 2, 2, "1A", "CONFIRMED");
        assertEquals(2, count("reservation"));
    }

    @Test
    void reservationsWithoutASeatDoNotCollide() throws SQLException {
        reserve("CA000001", 1, 1, null, "CREATED");
        reserve("CA000002", 2, 1, null, "CREATED");
        assertEquals(2, count("reservation"));
    }

    // ---- keys and checks

    @Test
    void aReservationNeedsAFlightThatExists() {
        assertRefused("INSERT INTO reservation (confirmation_number, customer_id, flight_id, seat_no, price_cents, "
                + "status, created_at) VALUES ('CA000001', 1, 99, NULL, NULL, 'CREATED', '2026-11-20T09:00:00')");
    }

    @Test
    void aReservationNeedsACustomerThatExists() {
        assertRefused("INSERT INTO reservation (confirmation_number, customer_id, flight_id, seat_no, price_cents, "
                + "status, created_at) VALUES ('CA000001', 99, 1, NULL, NULL, 'CREATED', '2026-11-20T09:00:00')");
    }

    @Test
    void aReservationNeedsASeatThatIsOnTheFlight() {
        assertRefused("INSERT INTO reservation (confirmation_number, customer_id, flight_id, seat_no, price_cents, "
                + "status, created_at) VALUES ('CA000001', 1, 1, '99Z', 20000, 'CONFIRMED', '2026-11-20T09:00:00')");
    }

    @Test
    void aSeatAndItsPriceComeTogether() {
        assertRefused("INSERT INTO reservation (confirmation_number, customer_id, flight_id, seat_no, price_cents, "
                + "status, created_at) VALUES ('CA000001', 1, 1, '1A', NULL, 'CONFIRMED', '2026-11-20T09:00:00')");
        assertRefused("INSERT INTO reservation (confirmation_number, customer_id, flight_id, seat_no, price_cents, "
                + "status, created_at) VALUES ('CA000002', 1, 1, NULL, 20000, 'CREATED', '2026-11-20T09:00:00')");
    }

    @Test
    void anUnknownStatusIsRefused() {
        assertRefused("INSERT INTO reservation (confirmation_number, customer_id, flight_id, seat_no, price_cents, "
                + "status, created_at) VALUES ('CA000001', 1, 1, NULL, NULL, 'BOGUS', '2026-11-20T09:00:00')");
    }

    @Test
    void confirmationNumbersAreUnique() throws SQLException {
        reserve("CA000001", 1, 1, "1A", "CONFIRMED");
        assertRefused("INSERT INTO reservation (confirmation_number, customer_id, flight_id, seat_no, price_cents, "
                + "status, created_at) VALUES ('CA000001', 2, 1, '1B', 20000, 'CONFIRMED', '2026-11-20T09:00:00')");
    }

    @Test
    void flightNumbersAreUnique() {
        assertRefused("INSERT INTO flight (flight_number, route_id, airline_code, aircraft_id, departure, arrival, "
                + "base_price_cents) VALUES ('AC123', 1, 'AC', 1, '2026-12-02T08:00:00', '2026-12-02T10:00:00', 100)");
    }

    @Test
    void aFlightNeedsAPositivePriceAndAnArrivalAfterTheDeparture() {
        assertRefused("INSERT INTO flight (flight_number, route_id, airline_code, aircraft_id, departure, arrival, "
                + "base_price_cents) VALUES ('AC777', 1, 'AC', 1, '2026-12-02T08:00:00', '2026-12-02T10:00:00', 0)");
        assertRefused("INSERT INTO flight (flight_number, route_id, airline_code, aircraft_id, departure, arrival, "
                + "base_price_cents) VALUES ('AC778', 1, 'AC', 1, '2026-12-02T10:00:00', '2026-12-02T08:00:00', 100)");
        assertRefused("INSERT INTO flight (flight_number, route_id, airline_code, aircraft_id, departure, arrival, "
                + "base_price_cents, archived) VALUES ('AC779', 1, 'AC', 1, '2026-12-02T08:00:00', "
                + "'2026-12-02T10:00:00', 100, 2)");
    }

    @Test
    void aRouteNeedsTwoDifferentAirportsAndIsUnique() {
        assertRefused("INSERT INTO route (origin_code, destination_code) VALUES ('YYC', 'YYC')");
        assertRefused("INSERT INTO route (origin_code, destination_code) VALUES ('YYC', 'YVR')");
        assertRefused("INSERT INTO route (origin_code, destination_code) VALUES ('YYC', 'XXX')");
    }

    @Test
    void anAircraftHasSensibleRowsAndSeats() {
        assertRefused("INSERT INTO aircraft (model, rows, seats_per_row) VALUES ('X', 0, 6)");
        assertRefused("INSERT INTO aircraft (model, rows, seats_per_row) VALUES ('X', 61, 6)");
        assertRefused("INSERT INTO aircraft (model, rows, seats_per_row) VALUES ('X', 30, 7)");
    }

    @Test
    void usernamesAndEmailsAreUniqueIgnoringCase() {
        assertRefused("INSERT INTO app_user (username, password_hash, role) VALUES ('JSmith', 'h', 'CUSTOMER')");
        assertRefused("INSERT INTO app_user (username, password_hash, role) VALUES ('boss', 'h', 'OWNER')");
    }

    @Test
    void aCustomerEmailIsUniqueIgnoringCase() throws SQLException {
        run("INSERT INTO app_user VALUES (3, 'third', 'hash', 'CUSTOMER')");
        assertRefused("INSERT INTO customer VALUES (3, 'Jo Again', 'JO@X.CA', NULL)");
    }

    @Test
    void onlyAMaskedCardCanBeStoredInAPayment() throws SQLException {
        reserve("CA000001", 1, 1, "1A", "PENDING_PAYMENT");
        run("INSERT INTO payment (reservation_id, amount_cents, method, status, masked_card, created_at) "
                + "VALUES (1, 20000, 'CREDIT', 'APPROVED', '**** **** **** 1111', '2026-11-20T09:01:00')");
        assertRefused("INSERT INTO payment (reservation_id, amount_cents, method, status, masked_card, created_at) "
                + "VALUES (1, 20000, 'CREDIT', 'APPROVED', '4111111111111111', '2026-11-20T09:01:00')");
        assertRefused("INSERT INTO payment (reservation_id, amount_cents, method, status, masked_card, created_at) "
                + "VALUES (1, 0, 'CREDIT', 'APPROVED', '**** **** **** 1111', '2026-11-20T09:01:00')");
        assertRefused("INSERT INTO payment (reservation_id, amount_cents, method, status, masked_card, created_at) "
                + "VALUES (1, 100, 'CASH', 'APPROVED', '**** **** **** 1111', '2026-11-20T09:01:00')");
        assertRefused("INSERT INTO payment (reservation_id, amount_cents, method, status, masked_card, created_at) "
                + "VALUES (99, 100, 'CREDIT', 'APPROVED', '**** **** **** 1111', '2026-11-20T09:01:00')");
    }

    @Test
    void deletingAFlightRemovesItsSeatsButNotWhileItIsReserved() throws SQLException {
        run("DELETE FROM flight WHERE id = 2");
        assertEquals(4, count("flight_seat"));

        reserve("CA000001", 1, 1, "1A", "CONFIRMED");
        assertRefused("DELETE FROM flight WHERE id = 1");
        assertEquals(1, count("flight"));
    }

    @Test
    void theSchemaCanBeLoadedAgainOnlyOnAnEmptyDatabase() throws Exception {
        // the initializer relies on this: running the script twice is an error, not a silent change
        assertThrows(SQLException.class, () -> run(schemaStatements().get(0)));
    }
}
