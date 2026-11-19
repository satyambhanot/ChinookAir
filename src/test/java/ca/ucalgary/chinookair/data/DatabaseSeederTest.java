package ca.ucalgary.chinookair.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Administrator;
import ca.ucalgary.chinookair.business.entity.Aircraft;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.FlightAgent;
import ca.ucalgary.chinookair.business.entity.User;
import ca.ucalgary.chinookair.util.PasswordHasher;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DatabaseSeederTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 12, 1, 9, 30);

    private static Connection shared;
    private static DaoFactory daos;

    private static Clock clockAt(LocalDateTime time) {
        return Clock.fixed(time.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    }

    @BeforeAll
    static void seedOnce() {
        shared = TestDatabase.inMemory();
        new DatabaseSeeder(shared).seedIfEmpty(clockAt(NOW));
        daos = DaoFactory.create(shared);
    }

    @AfterAll
    static void closeIt() throws SQLException {
        shared.close();
    }

    private static int count(Connection db, String table) {
        return JdbcSupport.queryOne(db, "SELECT COUNT(*) FROM " + table, JdbcSupport.NO_PARAMETERS,
                row -> row.getInt(1)).orElse(-1);
    }

    private static List<Flight> allFlights() {
        return daos.flightDao().findDepartingBetween(NOW.minusDays(1), NOW.plusDays(400));
    }

    // ---- what is there

    @Test
    void theDemoDataIsThere() {
        assertEquals(3, count(shared, "airline"));
        assertEquals(6, count(shared, "airport"));
        assertEquals(3, count(shared, "aircraft"));
        assertEquals(10, count(shared, "route"));
        assertTrue(count(shared, "flight") >= 40);
        assertEquals(12, count(shared, "promotion"));
        assertEquals(3, count(shared, "app_user"));
        assertEquals(1, count(shared, "customer"));
        assertEquals(0, count(shared, "reservation"));
    }

    @Test
    void routesRunInBothDirectionsForFivePairs() {
        for (String[] pair : new String[][] {{"YYC", "YVR"}, {"YYC", "YYZ"}, {"YYC", "YEG"}, {"YYZ", "YUL"},
            {"YYZ", "YOW"}}) {
            assertTrue(daos.routeDao().findByAirports(pair[0], pair[1]).isPresent(), pair[0] + "-" + pair[1]);
            assertTrue(daos.routeDao().findByAirports(pair[1], pair[0]).isPresent(), pair[1] + "-" + pair[0]);
        }
    }

    // ---- the flights

    @Test
    void everyFlightDepartsAfterTheClockAndWithinSixtyDays() {
        List<Flight> flights = allFlights();
        LocalDate first = NOW.toLocalDate().plusDays(1);
        LocalDate last = NOW.toLocalDate().plusDays(DatabaseSeeder.DAYS_AHEAD);
        for (Flight flight : flights) {
            assertTrue(flight.departure().isAfter(NOW), flight.flightNumber());
            assertFalse(flight.departure().toLocalDate().isBefore(first), flight.flightNumber());
            assertFalse(flight.departure().toLocalDate().isAfter(last), flight.flightNumber());
        }
        assertEquals(count(shared, "flight"), flights.size());
    }

    @Test
    void theMainRouteHasAFlightOnEveryDay() {
        for (int day = 1; day <= DatabaseSeeder.DAYS_AHEAD; day++) {
            LocalDate date = NOW.toLocalDate().plusDays(day);
            assertEquals(1, daos.flightDao().search("YYC", "YVR", date, null).size(), date.toString());
        }
        assertTrue(daos.flightDao().search("YYC", "YVR", NOW.toLocalDate(), null).isEmpty());
        assertTrue(daos.flightDao().search("YYC", "YVR", NOW.toLocalDate().plusDays(61), null).isEmpty());
    }

    @Test
    void everyFlightHasExactlyTheSeatsOfItsAircraft() {
        for (Flight flight : allFlights()) {
            assertEquals(flight.aircraft().seatCount(), daos.seatDao().findByFlight(flight.id()).size(),
                    flight.flightNumber());
            assertEquals(flight.aircraft().seatCount(), daos.seatDao().countAvailable(flight.id()));
        }
    }

    @Test
    void noAircraftIsOnTwoOverlappingFlights() {
        for (Aircraft aircraft : daos.aircraftDao().findAll()) {
            List<Flight> flights = daos.flightDao().findByAircraft(aircraft.id());
            assertFalse(flights.isEmpty(), aircraft.model());
            for (int i = 0; i < flights.size(); i++) {
                for (int j = i + 1; j < flights.size(); j++) {
                    Flight a = flights.get(i);
                    Flight b = flights.get(j);
                    assertFalse(a.overlaps(b.departure(), b.arrival()),
                            a.flightNumber() + " overlaps " + b.flightNumber());
                }
            }
        }
    }

    @Test
    void flightNumbersAreUniqueAndPricesAreSensible() {
        Set<String> numbers = new HashSet<>();
        for (Flight flight : allFlights()) {
            assertTrue(numbers.add(flight.flightNumber()), flight.flightNumber());
            assertTrue(flight.basePrice().signum() > 0);
            assertEquals(2, flight.basePrice().scale());
            assertTrue(flight.arrival().isAfter(flight.departure()));
        }
    }

    // ---- the accounts

    @Test
    void theDemoAccountsLogInWithTheirDocumentedPasswords() {
        User admin = daos.userDao().findByUsername(DatabaseSeeder.ADMIN_USERNAME).orElseThrow();
        assertInstanceOf(Administrator.class, admin);
        assertTrue(PasswordHasher.verify("admin123", admin.passwordHash()));
        assertFalse(PasswordHasher.verify("wrong", admin.passwordHash()));

        User agent = daos.userDao().findByUsername(DatabaseSeeder.AGENT_USERNAME).orElseThrow();
        assertInstanceOf(FlightAgent.class, agent);
        assertTrue(PasswordHasher.verify("agent123", agent.passwordHash()));

        User customer = daos.userDao().findByUsername(DatabaseSeeder.CUSTOMER_USERNAME).orElseThrow();
        Customer demo = assertInstanceOf(Customer.class, customer);
        assertTrue(PasswordHasher.verify("customer123", demo.passwordHash()));
        assertEquals("customer1@example.com", demo.email());
    }

    @Test
    void noPlainPasswordIsStored() {
        for (String username : new String[] {"customer1", "agent1", "admin"}) {
            String hash = daos.userDao().findByUsername(username).orElseThrow().passwordHash();
            assertTrue(hash.startsWith("pbkdf2$"));
        }
    }

    @Test
    void thePromotionsAreAllDifferent() {
        Set<String> titles = new HashSet<>();
        daos.promotionDao().findAll().forEach(p -> titles.add(p.title()));
        assertEquals(12, titles.size());
    }

    // ---- idempotent, relative to the clock, all or nothing

    @Test
    void callingItAgainChangesNothing() {
        int flights = count(shared, "flight");
        new DatabaseSeeder(shared).seedIfEmpty(clockAt(NOW.plusDays(30)));
        assertEquals(flights, count(shared, "flight"));
        assertEquals(3, count(shared, "airline"));
        assertEquals(3, count(shared, "app_user"));
        assertEquals(12, count(shared, "promotion"));
        assertEquals(NOW.toLocalDate().plusDays(1), allFlights().get(0).departure().toLocalDate());
    }

    @Test
    void theFlightsFollowTheClockTheDatabaseWasSeededWith() throws SQLException {
        try (Connection db = TestDatabase.inMemory()) {
            LocalDateTime later = LocalDateTime.of(2027, 3, 10, 23, 45);
            new DatabaseSeeder(db).seedIfEmpty(clockAt(later));
            List<Flight> flights = DaoFactory.create(db).flightDao()
                    .findDepartingBetween(later.minusDays(1), later.plusDays(400));
            assertEquals(LocalDate.of(2027, 3, 11), flights.get(0).departure().toLocalDate());
            assertEquals(LocalDate.of(2027, 5, 9), flights.get(flights.size() - 1).departure().toLocalDate());
        }
    }

    @Test
    void aFailureLeavesTheDatabaseEmptySoTheNextStartCanTryAgain() throws SQLException {
        try (Connection db = TestDatabase.inMemory()) {
            // a login called admin already exists, so the last step of the seeding fails
            try (Statement st = db.createStatement()) {
                st.execute("INSERT INTO app_user (username, password_hash, role) VALUES ('admin', 'h', 'ADMIN')");
            }
            DatabaseSeeder seeder = new DatabaseSeeder(db);
            assertThrows(DataAccessException.class, () -> seeder.seedIfEmpty(clockAt(NOW)));
            assertEquals(0, count(db, "airline"));
            assertEquals(0, count(db, "flight"));
            assertEquals(0, count(db, "flight_seat"));
            assertEquals(0, count(db, "promotion"));
            assertEquals(1, count(db, "app_user"));
            assertTrue(db.getAutoCommit());

            try (Statement st = db.createStatement()) {
                st.execute("DELETE FROM app_user");
            }
            seeder.seedIfEmpty(clockAt(NOW));
            assertEquals(3, count(db, "airline"));
            assertEquals(3, count(db, "app_user"));
        }
    }
}
