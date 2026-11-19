package ca.ucalgary.chinookair.data;

import static ca.ucalgary.chinookair.data.TestFixtures.aircraft;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Aircraft;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqliteAircraftDaoTest {

    private Connection db;
    private AircraftDao dao;

    @BeforeEach
    void open() {
        db = TestDatabase.inMemory();
        dao = new SqliteAircraftDao(db);
    }

    @AfterEach
    void close() throws SQLException {
        db.close();
    }

    @Test
    void savingGivesTheAircraftAnId() {
        Aircraft saved = dao.save(aircraft());
        assertTrue(saved.id() > 0);
        assertEquals("A320", saved.model());
        assertEquals(180, saved.seatCount());
    }

    @Test
    void findByIdReturnsTheSameModelAndSize() {
        Aircraft saved = dao.save(new Aircraft(null, "Dash 8", 20, 4));
        Aircraft found = dao.findById(saved.id()).orElseThrow();
        assertEquals(saved, found);
        assertEquals("Dash 8", found.model());
        assertEquals(80, found.seatCount());
    }

    @Test
    void anUnknownIdGivesAnEmptyResult() {
        assertTrue(dao.findById(99).isEmpty());
    }

    @Test
    void everyAircraftGetsItsOwnIdAndFindAllKeepsTheOrder() {
        Aircraft first = dao.save(aircraft());
        Aircraft second = dao.save(new Aircraft(null, "B737", 25, 6));
        assertNotEquals(first.id(), second.id());
        assertEquals(List.of(first, second), dao.findAll());
    }

    @Test
    void anAircraftThatIsAlreadySavedCannotBeSavedAgain() {
        Aircraft saved = dao.save(aircraft());
        assertThrows(IllegalArgumentException.class, () -> dao.save(saved));
    }
}
