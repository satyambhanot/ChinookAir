package ca.ucalgary.chinookair.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Promotion;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqlitePromotionDaoTest {

    private Connection db;
    private PromotionDao dao;

    @BeforeEach
    void open() {
        db = TestDatabase.inMemory();
        dao = new SqlitePromotionDao(db);
    }

    @AfterEach
    void close() throws SQLException {
        db.close();
    }

    @Test
    void aSavedPromotionGetsAnIdAndIsFoundById() {
        Promotion saved = dao.save(new Promotion(null, "10% off", "Fly in March"));
        assertTrue(saved.id() > 0);
        assertEquals(saved, dao.findById(saved.id()).orElseThrow());
        assertEquals("Fly in March", dao.findById(saved.id()).orElseThrow().description());
    }

    @Test
    void findAllKeepsTheOrderTheyWereSaved() {
        Promotion b = dao.save(new Promotion(null, "B title", "b"));
        Promotion a = dao.save(new Promotion(null, "A title", "a"));
        Promotion c = dao.save(new Promotion(null, "C title", "c"));
        assertEquals(List.of(b, a, c), dao.findAll());
    }

    @Test
    void anUnknownIdGivesAnEmptyResult() {
        assertTrue(dao.findById(999).isEmpty());
        assertTrue(dao.findAll().isEmpty());
    }

    @Test
    void aPromotionThatIsAlreadySavedCannotBeSavedAgain() {
        Promotion saved = dao.save(new Promotion(null, "10% off", "Fly in March"));
        assertThrows(IllegalArgumentException.class, () -> dao.save(saved));
    }
}
