package ca.ucalgary.chinookair.business.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Promotion;
import ca.ucalgary.chinookair.data.AppStateDao;
import ca.ucalgary.chinookair.data.DaoFactory;
import ca.ucalgary.chinookair.data.PromotionDao;
import ca.ucalgary.chinookair.data.TestDatabase;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PromotionControllerTest {

    private Connection db;
    private PromotionDao promotions;
    private AppStateDao state;
    private final List<Promotion> shown = new ArrayList<>();

    @BeforeEach
    void open() {
        db = TestDatabase.inMemory();
        DaoFactory daos = DaoFactory.create(db);
        promotions = daos.promotionDao();
        state = daos.appStateDao();
    }

    @AfterEach
    void close() throws SQLException {
        db.close();
    }

    private static Clock on(String date) {
        return Clock.fixed(LocalDate.parse(date).atTime(9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    }

    private void saveThreePromotions() {
        promotions.save(new Promotion(null, "First", "one"));
        promotions.save(new Promotion(null, "Second", "two"));
        promotions.save(new Promotion(null, "Third", "three"));
    }

    private PromotionController controller(String date, IntUnaryOperator choose) {
        PromotionController controller = new PromotionController(promotions, state, on(date), choose);
        controller.addListener(shown::add);
        return controller;
    }

    private PromotionController alwaysTheSecond(String date) {
        return controller(date, bound -> 1);
    }

    @Test
    void theFirstStartOfTheMonthShowsOnePromotionAndRemembersTheMonth() {
        saveThreePromotions();
        alwaysTheSecond("2026-12-01").checkOnStartup();
        assertEquals(1, shown.size());
        assertEquals("Second", shown.get(0).title());
        assertEquals("2026-12", state.get("last_promo_month").orElseThrow());
    }

    @Test
    void aSecondStartTheSameDayShowsNothing() {
        saveThreePromotions();
        PromotionController controller = alwaysTheSecond("2026-12-01");
        controller.checkOnStartup();
        controller.checkOnStartup();
        assertEquals(1, shown.size());
    }

    @Test
    void aLaterDayOfTheSameMonthShowsNothing() {
        saveThreePromotions();
        alwaysTheSecond("2026-12-01").checkOnStartup();
        alwaysTheSecond("2026-12-15").checkOnStartup();
        alwaysTheSecond("2026-12-31").checkOnStartup();
        assertEquals(1, shown.size());
    }

    @Test
    void theNextMonthShowsAPromotionAgain() {
        saveThreePromotions();
        alwaysTheSecond("2026-12-01").checkOnStartup();
        alwaysTheSecond("2027-01-02").checkOnStartup();
        assertEquals(2, shown.size());
        assertEquals("2027-01", state.get("last_promo_month").orElseThrow());
    }

    @Test
    void theSameMonthOfAnotherYearIsANewMonth() {
        saveThreePromotions();
        alwaysTheSecond("2026-12-01").checkOnStartup();
        alwaysTheSecond("2027-12-01").checkOnStartup();
        assertEquals(2, shown.size());
    }

    @Test
    void withoutPromotionsNothingIsShownAndNoMonthIsSaved() {
        alwaysTheSecond("2026-12-01").checkOnStartup();
        assertTrue(shown.isEmpty());
        assertTrue(state.get("last_promo_month").isEmpty());
    }

    @Test
    void aPromotionAddedLaterIsStillShownThatMonth() {
        PromotionController controller = alwaysTheSecond("2026-12-01");
        controller.checkOnStartup();
        promotions.save(new Promotion(null, "Only", "one"));
        promotions.save(new Promotion(null, "Another", "two"));
        controller.checkOnStartup();
        assertEquals(1, shown.size());
    }

    @Test
    void everyListenerHearsAboutIt() {
        saveThreePromotions();
        List<Promotion> second = new ArrayList<>();
        PromotionController controller = alwaysTheSecond("2026-12-01");
        controller.addListener(second::add);
        controller.checkOnStartup();
        assertEquals(1, shown.size());
        assertEquals(1, second.size());
        assertEquals(shown.get(0), second.get(0));
    }

    @Test
    void listenersAreToldInTheOrderTheyWereAdded() {
        saveThreePromotions();
        List<String> order = new ArrayList<>();
        PromotionController controller = new PromotionController(promotions, state, on("2026-12-01"), bound -> 0);
        controller.addListener(p -> order.add("a"));
        controller.addListener(p -> order.add("b"));
        controller.addListener(p -> order.add("c"));
        controller.checkOnStartup();
        assertEquals(List.of("a", "b", "c"), order);
    }

    @Test
    void noListenerIsNotAProblemAndTheMonthIsStillSaved() {
        saveThreePromotions();
        PromotionController controller = new PromotionController(promotions, state, on("2026-12-01"), bound -> 0);
        assertDoesNotThrow(controller::checkOnStartup);
        assertEquals("2026-12", state.get("last_promo_month").orElseThrow());
    }

    @Test
    void theChoiceIsAskedForTheNumberOfPromotions() {
        saveThreePromotions();
        List<Integer> bounds = new ArrayList<>();
        controller("2026-12-01", bound -> {
            bounds.add(bound);
            return 2;
        }).checkOnStartup();
        assertEquals(List.of(3), bounds);
        assertEquals("Third", shown.get(0).title());
    }

    @Test
    void aCrashWhileShowingCannotCauseASecondShowing() {
        saveThreePromotions();
        PromotionController crashing = new PromotionController(promotions, state, on("2026-12-01"), bound -> 0);
        crashing.addListener(p -> {
            throw new IllegalStateException("window failed");
        });
        assertThrows(IllegalStateException.class, crashing::checkOnStartup);

        alwaysTheSecond("2026-12-01").checkOnStartup();
        assertTrue(shown.isEmpty());
    }

    @Test
    void aBadChoiceDoesNotUseUpTheMonth() {
        saveThreePromotions();
        PromotionController broken = controller("2026-12-01", bound -> 7);
        assertThrows(IndexOutOfBoundsException.class, broken::checkOnStartup);
        assertTrue(state.get("last_promo_month").isEmpty());
        assertTrue(shown.isEmpty());
    }

    @Test
    void aStoredValueThatIsNotAMonthDoesNotBlockThePromotion() {
        saveThreePromotions();
        state.put("last_promo_month", "garbage");
        alwaysTheSecond("2026-12-01").checkOnStartup();
        assertEquals(1, shown.size());
        assertEquals("2026-12", state.get("last_promo_month").orElseThrow());
    }

    @Test
    void aListenerIsRequired() {
        PromotionController controller = alwaysTheSecond("2026-12-01");
        assertThrows(IllegalArgumentException.class, () -> controller.addListener(null));
    }
}
