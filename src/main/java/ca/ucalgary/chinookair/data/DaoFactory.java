package ca.ucalgary.chinookair.data;

import java.sql.Connection;

/**
 * The one public way into the data layer (Factory). It builds every DAO for one
 * connection and hands them out as interfaces, so nothing outside the data package
 * ever names a SQLite class. Each DAO is built once and shared.
 */
public final class DaoFactory {

    private final AirlineDao airlineDao;
    private final AirportDao airportDao;
    private final AircraftDao aircraftDao;
    private final RouteDao routeDao;
    private final FlightDao flightDao;
    private final SeatDao seatDao;
    private final UserDao userDao;
    private final CustomerDao customerDao;
    private final ReservationDao reservationDao;
    private final PaymentDao paymentDao;
    private final PromotionDao promotionDao;
    private final AppStateDao appStateDao;

    private DaoFactory(Connection connection) {
        airlineDao = new SqliteAirlineDao(connection);
        airportDao = new SqliteAirportDao(connection);
        aircraftDao = new SqliteAircraftDao(connection);
        routeDao = new SqliteRouteDao(connection);
        flightDao = new SqliteFlightDao(connection);
        seatDao = new SqliteSeatDao(connection);
        userDao = new SqliteUserDao(connection);
        customerDao = new SqliteCustomerDao(connection);
        // a reservation is rebuilt with its customer and its flight, so it uses those two DAOs
        reservationDao = new SqliteReservationDao(connection, customerDao, flightDao);
        paymentDao = new SqlitePaymentDao(connection);
        promotionDao = new SqlitePromotionDao(connection);
        appStateDao = new SqliteAppStateDao(connection);
    }

    /** @param connection an open connection to a database that is already set up */
    public static DaoFactory create(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("A connection is required");
        }
        return new DaoFactory(connection);
    }

    public AirlineDao airlineDao() {
        return airlineDao;
    }

    public AirportDao airportDao() {
        return airportDao;
    }

    public AircraftDao aircraftDao() {
        return aircraftDao;
    }

    public RouteDao routeDao() {
        return routeDao;
    }

    public FlightDao flightDao() {
        return flightDao;
    }

    public SeatDao seatDao() {
        return seatDao;
    }

    public UserDao userDao() {
        return userDao;
    }

    public CustomerDao customerDao() {
        return customerDao;
    }

    public ReservationDao reservationDao() {
        return reservationDao;
    }

    public PaymentDao paymentDao() {
        return paymentDao;
    }

    public PromotionDao promotionDao() {
        return promotionDao;
    }

    public AppStateDao appStateDao() {
        return appStateDao;
    }
}
