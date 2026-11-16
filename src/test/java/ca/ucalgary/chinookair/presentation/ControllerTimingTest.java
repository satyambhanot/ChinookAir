package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.AppContext;
import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.control.SearchCriteria;
import ca.ucalgary.chinookair.business.entity.User;
import ca.ucalgary.chinookair.data.DatabaseSeeder;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The screens call the controllers on the thread that draws the window, which is fine while a call
 * takes less than about a tenth of a second. If one ever takes longer, it has to move to a background
 * thread so the window does not freeze, and this test says so.
 */
class ControllerTimingTest {

    private static final long LIMIT_MILLIS = 100;
    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    static Path folder;

    private static Controllers c;
    private static User admin;
    private static User customer;

    @BeforeAll
    static void start() {
        c = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
        admin = c.loginController().authenticate(DatabaseSeeder.ADMIN_USERNAME, DatabaseSeeder.ADMIN_PASSWORD);
        customer = c.loginController()
                .authenticate(DatabaseSeeder.CUSTOMER_USERNAME, DatabaseSeeder.CUSTOMER_PASSWORD);
    }

    @AfterAll
    static void stop() {
        c.shutdown().run();
    }

    /** The time of the quickest of a few runs, so the first run's class loading is not counted. */
    private static long millis(Runnable call) {
        long best = Long.MAX_VALUE;
        for (int run = 0; run < 5; run++) {
            long begin = System.nanoTime();
            call.run();
            best = Math.min(best, (System.nanoTime() - begin) / 1_000_000);
        }
        return best;
    }

    private static void underTheLimit(String what, Runnable call) {
        long took = millis(call);
        assertTrue(took < LIMIT_MILLIS, what + " took " + took + " ms, the limit is " + LIMIT_MILLIS);
    }

    @Test
    void aFlightSearchIsFastEnoughForTheWindowThread() {
        underTheLimit("a flight search", () -> c.flightSearchController()
                .search(new SearchCriteria("YYC", "YVR", LocalDate.of(2026, 12, 2), null)));
    }

    @Test
    void theSevenDaysOfTheDayStripAreFastEnoughForTheWindowThread() {
        underTheLimit("seven searches", () -> {
            for (int day = 0; day < 7; day++) {
                c.flightSearchController()
                        .search(new SearchCriteria("YYC", "YVR", LocalDate.of(2026, 12, 1).plusDays(day), null));
            }
        });
    }

    @Test
    void aMonthOfTheScheduleIsFastEnoughForTheWindowThread() {
        underTheLimit("the schedule of 30 days", () -> c.scheduleController()
                .schedule(admin, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 30)));
    }

    @Test
    void theListsOfRoutesAndAircraftAreFastEnoughForTheWindowThread() {
        underTheLimit("the routes", () -> c.routeController().list());
        underTheLimit("the aircraft", () -> c.aircraftController().list());
    }

    @Test
    void theReservationHistoryIsFastEnoughForTheWindowThread() {
        underTheLimit("the history of a customer", () -> c.reservationController().history(customer, customer.id()));
    }
}
