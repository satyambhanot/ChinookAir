package ca.ucalgary.chinookair.data;

import static ca.ucalgary.chinookair.data.TestFixtures.airport;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Airport;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqliteAirportDaoTest {

    private Connection db;
    private AirportDao dao;

    @BeforeEach
    void open() {
        db = TestDatabase.inMemory();
        dao = new SqliteAirportDao(db);
    }

    @AfterEach
    void close() throws SQLException {
        db.close();
    }

    @Test
    void aSavedAirportIsFoundByItsCode() {
        dao.save(new Airport("YYC", "Calgary"));
        assertEquals(new Airport("YYC", "Calgary"), dao.findByCode("YYC").orElseThrow());
        assertTrue(dao.findByCode("ZZZ").isEmpty());
        assertTrue(dao.findByCode("yyc").isPresent());
    }

    @Test
    void findAllIsSortedByCode() {
        dao.save(airport("YVR"));
        dao.save(airport("YYC"));
        dao.save(airport("YEG"));
        List<String> codes = dao.findAll().stream().map(Airport::code).toList();
        assertEquals(List.of("YEG", "YVR", "YYC"), codes);
    }

    @Test
    void savingTheSameCodeTwiceFails() {
        dao.save(airport("YYC"));
        assertThrows(DataAccessException.class, () -> dao.save(new Airport("YYC", "Elsewhere")));
    }
}
