package ca.ucalgary.chinookair.data;

import ca.ucalgary.chinookair.business.entity.Aircraft;
import ca.ucalgary.chinookair.business.entity.Airline;
import ca.ucalgary.chinookair.business.entity.Airport;
import ca.ucalgary.chinookair.business.entity.Administrator;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.FlightAgent;
import ca.ucalgary.chinookair.business.entity.Promotion;
import ca.ucalgary.chinookair.business.entity.Route;
import ca.ucalgary.chinookair.business.entity.Seat;
import ca.ucalgary.chinookair.business.entity.SeatLayout;
import ca.ucalgary.chinookair.util.PasswordHasher;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

/**
 * Fills a brand new database with demo data, so the program has flights to search, accounts to
 * log in with and a promotion to show the first time it starts. It does nothing if the database
 * already has airlines, so it is safe to call on every start.
 */
public final class DatabaseSeeder {

    /** The demo accounts, also written in the README. */
    public static final String CUSTOMER_USERNAME = "customer1";
    public static final String CUSTOMER_PASSWORD = "customer123";
    public static final String AGENT_USERNAME = "agent1";
    public static final String AGENT_PASSWORD = "agent123";
    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "admin123";

    /** Flights are planned for tomorrow and the days after, up to this many days. */
    static final int DAYS_AHEAD = 60;

    /** One service of the timetable: a route flown at a fixed time on the days when it runs. */
    private record Leg(int number, String from, String to, String airlineCode, int aircraftIndex, LocalTime departure,
                       int minutes, int basePrice, IntPredicate runsOn) {
    }

    // aircraft 0 flies Calgary-Vancouver and Toronto-Ottawa, 1 flies Calgary-Toronto, 2 flies the short hops;
    // the times are chosen so that no aircraft is ever in two places at once
    private static final List<Leg> TIMETABLE = List.of(
            new Leg(1, "YYC", "YVR", "AC", 0, LocalTime.of(7, 0), 90, 179, day -> true),
            new Leg(2, "YVR", "YYC", "AC", 0, LocalTime.of(9, 30), 90, 179, day -> true),
            new Leg(3, "YYC", "YYZ", "WS", 1, LocalTime.of(11, 0), 255, 329, day -> true),
            new Leg(4, "YYZ", "YYC", "WS", 1, LocalTime.of(16, 30), 255, 329, day -> true),
            new Leg(5, "YYC", "YEG", "PD", 2, LocalTime.of(13, 0), 65, 129, day -> day % 2 == 0),
            new Leg(6, "YEG", "YYC", "PD", 2, LocalTime.of(15, 30), 65, 129, day -> day % 2 == 0),
            new Leg(7, "YYZ", "YUL", "AC", 2, LocalTime.of(7, 30), 80, 149, day -> day % 3 == 0),
            new Leg(8, "YUL", "YYZ", "AC", 2, LocalTime.of(18, 0), 80, 149, day -> day % 3 == 0),
            new Leg(9, "YYZ", "YOW", "WS", 0, LocalTime.of(12, 0), 65, 139, day -> day % 4 == 0),
            new Leg(10, "YOW", "YYZ", "WS", 0, LocalTime.of(14, 30), 65, 139, day -> day % 4 == 0));

    private static final String[][] PROMOTIONS = {
        {"New Year, New Trips", "Start the year with 10% off any flight booked in January."},
        {"Winter Escape", "Beat the cold: 12% off flights to Vancouver in February."},
        {"Spring Break Sale", "Book a March flight and save 15% on economy seats."},
        {"April Showers, Fare Flowers", "Free seat selection on every April booking."},
        {"May Long Weekend", "Business class seats at 20% off for the long weekend."},
        {"Summer Starts Here", "Save 10% when you reserve a June flight before the 15th."},
        {"Canada Day Getaway", "Special fares to Toronto, Ottawa and Montreal in July."},
        {"Stampede Special", "Fly to Calgary in July with 15% off economy."},
        {"Back to School", "Students save 10% on any September flight."},
        {"Thanksgiving Fares", "Family fares for the October long weekend."},
        {"November Deals", "Save 12% on flights booked at least 14 days ahead."},
        {"Holiday Season", "Reserve a December flight early and save 15%."}};

    private final Connection connection;
    private final DaoFactory daos;

    public DatabaseSeeder(Connection connection) {
        this.connection = connection;
        this.daos = DaoFactory.create(connection);
    }

    /**
     * Adds the demo data if the database has no airlines yet. Everything is saved in one
     * transaction, so a failure leaves the database empty and the next start tries again.
     *
     * @param clock the flights are planned from tomorrow on, counted from this clock
     */
    public void seedIfEmpty(Clock clock) {
        if (!daos.airlineDao().findAll().isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now(clock);
        JdbcSupport.transaction(connection, () -> {
            seedAirlinesAndAirports();
            List<Aircraft> aircraft = seedAircraft();
            Map<String, Route> routes = seedRoutes();
            seedFlights(today, aircraft, routes);
            seedPromotions();
            seedUsers();
            return null;
        });
    }

    private void seedAirlinesAndAirports() {
        daos.airlineDao().save(new Airline("AC", "Air Canada"));
        daos.airlineDao().save(new Airline("WS", "WestJet"));
        daos.airlineDao().save(new Airline("PD", "Porter Airlines"));
        daos.airportDao().save(new Airport("YYC", "Calgary"));
        daos.airportDao().save(new Airport("YVR", "Vancouver"));
        daos.airportDao().save(new Airport("YYZ", "Toronto"));
        daos.airportDao().save(new Airport("YEG", "Edmonton"));
        daos.airportDao().save(new Airport("YUL", "Montreal"));
        daos.airportDao().save(new Airport("YOW", "Ottawa"));
    }

    private List<Aircraft> seedAircraft() {
        return List.of(
                daos.aircraftDao().save(new Aircraft(null, "Airbus A320", 30, 6)),
                daos.aircraftDao().save(new Aircraft(null, "Boeing 737-800", 28, 6)),
                daos.aircraftDao().save(new Aircraft(null, "Dash 8 Q400", 20, 4)));
    }

    /** Both directions of five airport pairs, found by "YYC-YVR". */
    private Map<String, Route> seedRoutes() {
        Map<String, Route> routes = new HashMap<>();
        for (Leg leg : TIMETABLE) {
            String key = leg.from() + "-" + leg.to();
            Airport from = daos.airportDao().findByCode(leg.from()).orElseThrow();
            Airport to = daos.airportDao().findByCode(leg.to()).orElseThrow();
            routes.put(key, daos.routeDao().save(new Route(null, from, to)));
        }
        return routes;
    }

    private void seedFlights(LocalDate today, List<Aircraft> aircraft, Map<String, Route> routes) {
        // the seats follow from the aircraft, so they are worked out once for each
        Map<Long, List<Seat>> seatsOfAircraft = new HashMap<>();
        for (Aircraft plane : aircraft) {
            seatsOfAircraft.put(plane.id(), SeatLayout.generate(plane));
        }
        for (int day = 1; day <= DAYS_AHEAD; day++) {
            LocalDate date = today.plusDays(day);
            for (Leg leg : TIMETABLE) {
                if (!leg.runsOn().test(day)) {
                    continue;
                }
                Aircraft plane = aircraft.get(leg.aircraftIndex());
                LocalDateTime departure = date.atTime(leg.departure());
                // the number carries the route and the day, so every flight has its own: AC101 to AC160, ...
                String number = leg.airlineCode() + (leg.number() * 100 + day);
                BigDecimal price = BigDecimal.valueOf(leg.basePrice() + 5L * (day % 7)).setScale(2);
                Airline airline = daos.airlineDao().findByCode(leg.airlineCode()).orElseThrow();
                Flight flight = daos.flightDao().save(new Flight(null, number, routes.get(leg.from() + "-" + leg.to()),
                        airline, plane, departure, departure.plusMinutes(leg.minutes()), price));
                daos.seatDao().createSeats(flight.id(), seatsOfAircraft.get(plane.id()));
            }
        }
    }

    private void seedPromotions() {
        for (String[] promotion : PROMOTIONS) {
            daos.promotionDao().save(new Promotion(null, promotion[0], promotion[1]));
        }
    }

    private void seedUsers() {
        daos.customerDao().save(new Customer(null, CUSTOMER_USERNAME, PasswordHasher.hash(CUSTOMER_PASSWORD),
                "Casey Customer", "customer1@example.com", "403-555-0101"));
        daos.userDao().save(new FlightAgent(null, AGENT_USERNAME, PasswordHasher.hash(AGENT_PASSWORD)));
        daos.userDao().save(new Administrator(null, ADMIN_USERNAME, PasswordHasher.hash(ADMIN_PASSWORD)));
    }
}
