package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.AppContext;
import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Airport;
import ca.ucalgary.chinookair.business.entity.Route;
import ca.ucalgary.chinookair.data.DatabaseSeeder;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RouteManagementViewTest {

    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    static Path folder;

    private static Controllers controllers;

    private RecordingNavigator navigator;
    private RouteManagementView view;

    @BeforeAll
    static void startTheProgramParts() {
        controllers = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
    }

    @AfterAll
    static void shutDown() {
        controllers.shutdown().run();
    }

    @BeforeEach
    void theAdministratorOpensTheScreen() {
        controllers.session().login(controllers.loginController()
                .authenticate(DatabaseSeeder.ADMIN_USERNAME, DatabaseSeeder.ADMIN_PASSWORD));
        navigator = new RecordingNavigator();
        view = new RouteManagementView(controllers, navigator);
        view.onShow();
    }

    /** Chooses an origin and a destination that no route joins yet, and returns the pair as "XXX-YYY". */
    private String chooseAFreePair() {
        List<Airport> airports = controllers.flightSearchController().airports();
        List<Route> routes = controllers.routeController().list();
        for (int o = 0; o < airports.size(); o++) {
            for (int d = 0; d < airports.size(); d++) {
                String from = airports.get(o).code();
                String to = airports.get(d).code();
                boolean taken = routes.stream().anyMatch(
                        r -> r.origin().code().equals(from) && r.destination().code().equals(to));
                if (o != d && !taken) {
                    view.originCombo.setSelectedIndex(o);
                    view.destinationCombo.setSelectedIndex(d);
                    return from + "-" + to;
                }
            }
        }
        throw new IllegalStateException("every pair already has a route");
    }

    @Test
    void theTableShowsTheSeededRoutes() {
        assertTrue(view.tableModel.getRowCount() > 0);
    }

    @Test
    void aNewRouteAppearsInTheTable() {
        int before = view.tableModel.getRowCount();
        String pair = chooseAFreePair();
        view.addButton.doClick(0);
        assertEquals(before + 1, view.tableModel.getRowCount());
        assertEquals("Added route " + pair.replace("-", " to "), view.message());
    }

    @Test
    void theSameRouteAgainIsRefused() {
        chooseAFreePair();
        view.addButton.doClick(0);
        int rows = view.tableModel.getRowCount();
        view.addButton.doClick(0);
        assertEquals("Route already exists", view.message());
        assertEquals(rows, view.tableModel.getRowCount());
    }

    @Test
    void theSameAirportTwiceIsRefused() {
        view.originCombo.setSelectedIndex(0);
        view.destinationCombo.setSelectedIndex(0);
        view.addButton.doClick(0);
        assertTrue(view.message().contains("different"), view.message());
    }

    @Test
    void anAgentAndACustomerCannotReachTheScreen() {
        controllers.session().login(controllers.loginController()
                .authenticate(DatabaseSeeder.AGENT_USERNAME, DatabaseSeeder.AGENT_PASSWORD));
        view.onShow();
        assertEquals(ViewId.LOGIN, navigator.last());
        navigator.shown.clear();
        controllers.session().login(controllers.loginController()
                .authenticate(DatabaseSeeder.CUSTOMER_USERNAME, DatabaseSeeder.CUSTOMER_PASSWORD));
        view.onShow();
        assertEquals(ViewId.LOGIN, navigator.last());
    }

    @Test
    void backGoesToTheAdministratorDashboard() {
        view.backButton.doClick(0);
        assertEquals(ViewId.ADMIN_DASHBOARD, navigator.last());
    }
}
