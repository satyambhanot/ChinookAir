package ca.ucalgary.chinookair.data;

import static ca.ucalgary.chinookair.data.TestFixtures.airport;
import static ca.ucalgary.chinookair.data.TestFixtures.route;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Route;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqliteRouteDaoTest {

    private Connection db;
    private RouteDao dao;

    @BeforeEach
    void open() {
        db = TestDatabase.inMemory();
        dao = new SqliteRouteDao(db);
        AirportDao airports = new SqliteAirportDao(db);
        airports.save(airport("YYC"));
        airports.save(airport("YVR"));
        airports.save(airport("YYZ"));
    }

    @AfterEach
    void close() throws SQLException {
        db.close();
    }

    @Test
    void savingGivesTheRouteAnIdAndKeepsItsAirports() {
        Route saved = dao.save(route("YYC", "YVR"));
        assertTrue(saved.id() > 0);
        assertEquals(airport("YYC"), saved.origin());
        assertEquals(airport("YVR"), saved.destination());
    }

    @Test
    void findByAirportsKnowsTheDirection() {
        Route saved = dao.save(route("YYC", "YVR"));
        assertEquals(saved, dao.findByAirports("YYC", "YVR").orElseThrow());
        assertTrue(dao.findByAirports("YVR", "YYC").isEmpty());
        assertEquals(saved, dao.findByAirports("yyc", " yvr ").orElseThrow());
        assertTrue(dao.findByAirports(null, "YVR").isEmpty());
    }

    @Test
    void findByIdBuildsTheCompleteRoute() {
        Route saved = dao.save(route("YVR", "YYZ"));
        Route found = dao.findById(saved.id()).orElseThrow();
        assertEquals(saved, found);
        assertEquals("Toronto", found.destination().city());
        assertTrue(dao.findById(99).isEmpty());
    }

    @Test
    void aRouteNeedsAirportsThatAreSaved() {
        assertThrows(DataAccessException.class, () -> dao.save(route("YYC", "YEG")));
        assertTrue(dao.findAll().isEmpty());
    }

    @Test
    void theSameRouteCannotBeSavedTwice() {
        dao.save(route("YYC", "YVR"));
        assertThrows(DataAccessException.class, () -> dao.save(route("YYC", "YVR")));
    }

    @Test
    void findAllIsSortedByOriginThenDestination() {
        dao.save(route("YVR", "YYC"));
        dao.save(route("YYC", "YYZ"));
        dao.save(route("YYC", "YVR"));
        List<String> pairs = dao.findAll().stream()
                .map(r -> r.origin().code() + "-" + r.destination().code()).toList();
        assertEquals(List.of("YVR-YYC", "YYC-YVR", "YYC-YYZ"), pairs);
    }

    @Test
    void aRouteThatIsAlreadySavedCannotBeSavedAgain() {
        Route saved = dao.save(route("YYC", "YVR"));
        assertThrows(IllegalArgumentException.class, () -> dao.save(saved));
    }
}
