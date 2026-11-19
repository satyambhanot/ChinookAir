package ca.ucalgary.chinookair;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.control.AuthenticationException;
import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.control.SearchCriteria;
import ca.ucalgary.chinookair.business.entity.Administrator;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Promotion;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppContextTest {

    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    Path folder;

    private Controllers controllers;

    @AfterEach
    void closeAndClearProperty() {
        if (controllers != null) {
            controllers.shutdown().run();
        }
        System.clearProperty(AppContext.TODAY_PROPERTY);
    }

    private Controllers create() {
        controllers = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
        return controllers;
    }

    @Test
    void everyControllerIsBuilt() {
        Controllers c = create();
        assertNotNull(c.loginController());
        assertNotNull(c.customerController());
        assertNotNull(c.flightSearchController());
        assertNotNull(c.bookingController());
        assertNotNull(c.paymentController());
        assertNotNull(c.reservationController());
        assertNotNull(c.flightManagementController());
        assertNotNull(c.routeController());
        assertNotNull(c.aircraftController());
        assertNotNull(c.scheduleController());
        assertNotNull(c.promotionController());
        assertNotNull(c.session());
        assertNotNull(c.shutdown());
    }

    @Test
    void theDatabaseIsSeededSoTheDemoLoginsWork() {
        Controllers c = create();
        assertInstanceOf(Administrator.class, c.loginController().authenticate("admin", "admin123"));
        assertInstanceOf(Customer.class, c.loginController().authenticate("customer1", "customer123"));
        assertThrows(AuthenticationException.class, () -> c.loginController().authenticate("admin", "wrong"));
    }

    @Test
    void theSeededFlightsFollowTheClockOfTheContext() {
        Controllers c = create();
        LocalDate tomorrow = LocalDate.of(2026, 12, 2);
        assertEquals(1, c.flightSearchController().search(new SearchCriteria("YYC", "YVR", tomorrow, null)).size());
    }

    @Test
    void theControllersWorkTogetherThroughTheSharedSession() {
        Controllers c = create();
        assertTrue(c.session().currentUser().isEmpty());
        c.session().login(c.loginController().authenticate("agent1", "agent123"));
        assertEquals("agent1", c.session().currentUser().orElseThrow().username());
        assertEquals(1, c.customerController().search(c.session().currentUser().orElseThrow(), "casey").size());
    }

    @Test
    void thePromotionControllerFindsTheSeededPromotions() {
        Controllers c = create();
        List<Promotion> shown = new ArrayList<>();
        c.promotionController().addListener(shown::add);
        c.promotionController().checkOnStartup();
        assertEquals(1, shown.size());
    }

    @Test
    void shutdownClosesTheDatabaseAndMayBeCalledTwice() {
        Controllers c = create();
        c.shutdown().run();
        assertDoesNotThrow(() -> c.shutdown().run());
        // the database is closed now, so a login can no longer reach it
        assertThrows(RuntimeException.class, () -> c.loginController().authenticate("admin", "admin123"));
    }

    @Test
    void twoContextsDoNotShareADatabase() {
        Controllers first = create();
        Controllers second = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
        try {
            first.customerController().register("someone", "pass1234", "Some One", "s@x.ca", "");
            assertNotNull(second.loginController().authenticate("admin", "admin123"));
            assertThrows(AuthenticationException.class,
                    () -> second.loginController().authenticate("someone", "pass1234"));
        } finally {
            second.shutdown().run();
        }
    }

    // ---- the clock for manual testing

    @Test
    void theTodayPropertyFixesTheDateOfTheClock() {
        System.setProperty(AppContext.TODAY_PROPERTY, "2027-02-01");
        Clock clock = AppContext.defaultClock();
        assertEquals(LocalDate.of(2027, 2, 1), LocalDate.now(clock));
        // fixed: it does not tick
        assertEquals(clock.instant(), clock.instant());
    }

    @Test
    void withoutTheTodayPropertyTheClockIsTheRealOne() {
        System.clearProperty(AppContext.TODAY_PROPERTY);
        LocalDate before = LocalDate.now();
        LocalDate used = LocalDate.now(AppContext.defaultClock());
        assertTrue(!used.isBefore(before));
        System.setProperty(AppContext.TODAY_PROPERTY, "  ");
        assertTrue(!LocalDate.now(AppContext.defaultClock()).isBefore(before));
    }

    @Test
    void aBadTodayPropertyIsExplained() {
        System.setProperty(AppContext.TODAY_PROPERTY, "tomorrow");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, AppContext::defaultClock);
        assertTrue(e.getMessage().contains("2027-02-01"), e.getMessage());
    }
}
