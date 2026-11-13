package ca.ucalgary.chinookair;

import ca.ucalgary.chinookair.business.control.AircraftController;
import ca.ucalgary.chinookair.business.control.BookingController;
import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.control.CustomerController;
import ca.ucalgary.chinookair.business.control.FlightManagementController;
import ca.ucalgary.chinookair.business.control.FlightSearchController;
import ca.ucalgary.chinookair.business.control.LoginController;
import ca.ucalgary.chinookair.business.control.PaymentController;
import ca.ucalgary.chinookair.business.control.PromotionController;
import ca.ucalgary.chinookair.business.control.RandomConfirmationNumberGenerator;
import ca.ucalgary.chinookair.business.control.ReservationController;
import ca.ucalgary.chinookair.business.control.RouteController;
import ca.ucalgary.chinookair.business.control.ScheduleController;
import ca.ucalgary.chinookair.business.control.SessionContext;
import ca.ucalgary.chinookair.data.ConfirmationWriter;
import ca.ucalgary.chinookair.data.DaoFactory;
import ca.ucalgary.chinookair.data.DatabaseConnection;
import ca.ucalgary.chinookair.data.DatabaseSeeder;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Random;

/**
 * The composition root: the one place that builds the database, the DAOs and the controllers and
 * hands them out. No other class creates a DAO or a controller, so the layers stay separate.
 */
public final class AppContext {

    /** System property that fixes the date of the program, for manual testing only, like 2027-02-01. */
    public static final String TODAY_PROPERTY = "chinookair.today";

    private AppContext() {
    }

    /** Opens the real database file, seeds it on first use and builds everything. */
    public static Controllers create() {
        DatabaseConnection database = DatabaseConnection.getInstance();
        try {
            return build(database, defaultClock(), null);
        } catch (RuntimeException e) {
            database.close();
            throw e;
        }
    }

    /** For tests: a separate database, like jdbc:sqlite::memory:, and a clock of the test's choosing. */
    public static Controllers create(String jdbcUrl, Clock clock) {
        return create(jdbcUrl, clock, null);
    }

    /**
     * For tests that must not write confirmations into the home folder.
     *
     * @param confirmations the folder for the confirmation files, or null for the usual one
     */
    public static Controllers create(String jdbcUrl, Clock clock, Path confirmations) {
        DatabaseConnection database = DatabaseConnection.forUrl(jdbcUrl);
        try {
            return build(database, clock, confirmations);
        } catch (RuntimeException e) {
            database.close();
            throw e;
        }
    }

    /** The system clock, or a clock fixed at 09:00 on the day of the chinookair.today property if it is set. */
    static Clock defaultClock() {
        String today = System.getProperty(TODAY_PROPERTY);
        if (today == null || today.isBlank()) {
            return Clock.systemDefaultZone();
        }
        try {
            ZoneId zone = ZoneId.systemDefault();
            return Clock.fixed(LocalDate.parse(today.trim()).atTime(9, 0).atZone(zone).toInstant(), zone);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(TODAY_PROPERTY + " must look like 2027-02-01, not " + today);
        }
    }

    private static Controllers build(DatabaseConnection database, Clock clock, Path confirmations) {
        new DatabaseSeeder(database.connection()).seedIfEmpty(clock);
        DaoFactory daos = DaoFactory.create(database.connection());
        ConfirmationWriter writer = confirmations == null ? new ConfirmationWriter()
                : new ConfirmationWriter(confirmations);
        Random random = new SecureRandom();
        return new Controllers(
                new LoginController(daos.userDao()),
                new CustomerController(daos.customerDao(), daos.userDao()),
                new FlightSearchController(daos.flightDao(), daos.seatDao(), daos.airportDao(),
                        daos.airlineDao(), clock),
                new BookingController(daos.flightDao(), daos.seatDao(), daos.reservationDao(),
                        new RandomConfirmationNumberGenerator(random), clock),
                new PaymentController(daos.reservationDao(), daos.paymentDao(), writer, clock),
                new ReservationController(daos.reservationDao(), daos.paymentDao(), daos.flightDao(),
                        daos.seatDao(), clock),
                new FlightManagementController(daos.flightDao(), daos.seatDao(), daos.routeDao(),
                        daos.airlineDao(), daos.aircraftDao(), daos.reservationDao(), clock),
                new RouteController(daos.routeDao(), daos.airportDao()),
                new AircraftController(daos.aircraftDao()),
                new ScheduleController(daos.flightDao(), clock),
                new PromotionController(daos.promotionDao(), daos.appStateDao(), clock, random::nextInt),
                new SessionContext(),
                closeOnce(database));
    }

    /** Closing twice is harmless, because both the window and a shutdown hook may ask. */
    private static Runnable closeOnce(DatabaseConnection database) {
        boolean[] closed = {false};
        return () -> {
            synchronized (closed) {
                if (!closed[0]) {
                    closed[0] = true;
                    database.close();
                }
            }
        };
    }
}
