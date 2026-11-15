package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class MonthGridModelTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 12, 1);
    private static final YearMonth DECEMBER = YearMonth.of(2026, 12);

    private static MonthGridModel december(LocalDate selected, LocalDate min, LocalDate max) {
        return new MonthGridModel(DECEMBER, TODAY, selected, min, max);
    }

    private static MonthGridModel.Day day(MonthGridModel model, LocalDate date) {
        return model.weeks().stream().flatMap(List::stream).filter(d -> d.date().equals(date)).findFirst()
                .orElseThrow();
    }

    @Test
    void theTitleIsTheMonthAndTheYear() {
        assertEquals("December 2026", december(null, null, null).title());
        assertEquals("January 2027", december(null, null, null).next().title());
    }

    @Test
    void thereAreAlwaysSixWeeksOfSevenDaysStartingOnSunday() {
        for (int month = 1; month <= 12; month++) {
            MonthGridModel model = new MonthGridModel(YearMonth.of(2026, month), TODAY, null, null, null);
            assertEquals(6, model.weeks().size());
            for (List<MonthGridModel.Day> week : model.weeks()) {
                assertEquals(7, week.size());
                assertEquals(java.time.DayOfWeek.SUNDAY, week.get(0).date().getDayOfWeek());
            }
        }
        assertEquals(List.of("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"), december(null, null, null).weekdayNames());
    }

    @Test
    void december2026StartsOnATuesday() {
        List<MonthGridModel.Day> first = december(null, null, null).weeks().get(0);
        assertEquals(LocalDate.of(2026, 11, 29), first.get(0).date());
        assertFalse(first.get(0).inMonth());
        assertFalse(first.get(1).inMonth());
        assertEquals(LocalDate.of(2026, 12, 1), first.get(2).date());
        assertTrue(first.get(2).inMonth());
    }

    @Test
    void aMonthStartingOnASundayHasItsFirstDayInTheFirstColumn() {
        // 1 Nov 2026 is a Sunday
        MonthGridModel november = new MonthGridModel(YearMonth.of(2026, 11), TODAY, null, null, null);
        assertEquals(LocalDate.of(2026, 11, 1), november.weeks().get(0).get(0).date());
        assertTrue(november.weeks().get(0).get(0).inMonth());
    }

    @Test
    void everyDayOfTheMonthIsInTheGridOnce() {
        long inMonth = december(null, null, null).weeks().stream().flatMap(List::stream)
                .filter(MonthGridModel.Day::inMonth).count();
        assertEquals(31, inMonth);
    }

    @Test
    void todayAndTheSelectedDayAreMarked() {
        MonthGridModel model = december(LocalDate.of(2026, 12, 15), null, null);
        assertTrue(day(model, TODAY).today());
        assertFalse(day(model, TODAY).selected());
        assertTrue(day(model, LocalDate.of(2026, 12, 15)).selected());
        assertFalse(day(model, LocalDate.of(2026, 12, 16)).today());
    }

    @Test
    void daysOutsideTheLimitsAreNotEnabled() {
        MonthGridModel model = december(null, LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 20));
        assertFalse(day(model, LocalDate.of(2026, 12, 9)).enabled());
        assertTrue(day(model, LocalDate.of(2026, 12, 10)).enabled());
        assertTrue(day(model, LocalDate.of(2026, 12, 20)).enabled());
        assertFalse(day(model, LocalDate.of(2026, 12, 21)).enabled());
    }

    @Test
    void daysOfTheNeighbouringMonthsAreNeverEnabled() {
        assertFalse(day(december(null, null, null), LocalDate.of(2026, 11, 30)).enabled());
        assertFalse(day(december(null, null, null), LocalDate.of(2027, 1, 2)).enabled());
    }

    @Test
    void youCannotPageBeforeTheMinimumOrAfterTheMaximum() {
        MonthGridModel limited = december(null, TODAY, LocalDate.of(2026, 12, 31));
        assertFalse(limited.canGoPrevious());
        assertFalse(limited.canGoNext());
        assertTrue(december(null, null, null).canGoPrevious());
        assertTrue(december(null, null, null).canGoNext());
        // a minimum in the previous month still lets you go back one month
        assertTrue(december(null, LocalDate.of(2026, 11, 20), null).canGoPrevious());
    }

    @Test
    void pagingMovesOneMonthAndKeepsTheLimits() {
        MonthGridModel model = december(null, LocalDate.of(2026, 12, 10), null);
        assertEquals(YearMonth.of(2027, 1), model.next().month());
        assertEquals(YearMonth.of(2026, 11), model.previous().month());
        assertFalse(model.previous().weeks().stream().flatMap(List::stream).anyMatch(MonthGridModel.Day::enabled));
    }
}
