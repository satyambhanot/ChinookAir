package ca.ucalgary.chinookair.data;

import static ca.ucalgary.chinookair.data.TestFixtures.airline;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Airline;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqliteAirlineDaoTest {

    private Connection db;
    private AirlineDao dao;

    @BeforeEach
    void open() {
        db = TestDatabase.inMemory();
        dao = new SqliteAirlineDao(db);
    }

    @AfterEach
    void close() throws SQLException {
        db.close();
    }

    @Test
    void aSavedAirlineIsFoundByItsCode() {
        Airline saved = dao.save(new Airline("AC", "Air Canada"));
        assertEquals(new Airline("AC", "Air Canada"), saved);
        assertEquals(new Airline("AC", "Air Canada"), dao.findByCode("AC").orElseThrow());
    }

    @Test
    void anUnknownCodeGivesAnEmptyResult() {
        dao.save(airline("AC"));
        assertTrue(dao.findByCode("ZZ").isEmpty());
        assertTrue(dao.findByCode(null).isEmpty());
    }

    @Test
    void theCodeIsFoundInAnyCase() {
        dao.save(airline("AC"));
        assertTrue(dao.findByCode(" ac ").isPresent());
    }

    @Test
    void savingTheSameCodeTwiceFails() {
        dao.save(airline("AC"));
        assertThrows(DataAccessException.class, () -> dao.save(new Airline("AC", "Another Name")));
    }

    @Test
    void findAllIsSortedByCode() {
        dao.save(airline("WS"));
        dao.save(airline("AC"));
        dao.save(airline("PD"));
        List<String> codes = dao.findAll().stream().map(Airline::code).toList();
        assertEquals(List.of("AC", "PD", "WS"), codes);
    }

    @Test
    void findAllOnAnEmptyTableIsEmpty() {
        assertTrue(dao.findAll().isEmpty());
    }
}
