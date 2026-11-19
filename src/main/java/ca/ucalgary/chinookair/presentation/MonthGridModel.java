package ca.ucalgary.chinookair.presentation;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The days of one month laid out as a calendar: always six weeks of seven days, starting on Sunday, so
 * the picture never changes height. Days of the neighbouring months are in the grid but not enabled.
 * It holds no Swing, so the rules (which days can be picked, where "today" is) are easy to test.
 */
final class MonthGridModel {

    static final int WEEKS = 6;

    /** One cell of the grid. */
    record Day(LocalDate date, boolean inMonth, boolean enabled, boolean today, boolean selected) {
    }

    private final YearMonth month;
    private final LocalDate today;
    private final LocalDate selected;
    private final LocalDate min;
    private final LocalDate max;

    /**
     * @param selected the chosen date, or null
     * @param min      the first date that may be chosen, or null for no limit
     * @param max      the last date that may be chosen, or null for no limit
     */
    MonthGridModel(YearMonth month, LocalDate today, LocalDate selected, LocalDate min, LocalDate max) {
        this.month = month;
        this.today = today;
        this.selected = selected;
        this.min = min;
        this.max = max;
    }

    YearMonth month() {
        return month;
    }

    /** Like "December 2026". */
    String title() {
        return month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.getYear();
    }

    /** The two-letter names above the columns, Sunday first. */
    List<String> weekdayNames() {
        List<String> names = new ArrayList<>();
        DayOfWeek day = DayOfWeek.SUNDAY;
        for (int i = 0; i < 7; i++) {
            names.add(day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).substring(0, 2));
            day = day.plus(1);
        }
        return names;
    }

    /** Six weeks, each with seven days. */
    List<List<Day>> weeks() {
        LocalDate first = month.atDay(1);
        // Sunday is 7 in java.time, and should be column 0
        LocalDate cell = first.minusDays(first.getDayOfWeek().getValue() % 7);
        List<List<Day>> weeks = new ArrayList<>();
        for (int w = 0; w < WEEKS; w++) {
            List<Day> week = new ArrayList<>();
            for (int d = 0; d < 7; d++) {
                boolean inMonth = YearMonth.from(cell).equals(month);
                week.add(new Day(cell, inMonth, inMonth && allowed(cell), cell.equals(today), cell.equals(selected)));
                cell = cell.plusDays(1);
            }
            weeks.add(List.copyOf(week));
        }
        return List.copyOf(weeks);
    }

    private boolean allowed(LocalDate date) {
        return (min == null || !date.isBefore(min)) && (max == null || !date.isAfter(max));
    }

    boolean canGoPrevious() {
        return min == null || !month.minusMonths(1).atEndOfMonth().isBefore(min);
    }

    boolean canGoNext() {
        return max == null || !month.plusMonths(1).atDay(1).isAfter(max);
    }

    MonthGridModel previous() {
        return new MonthGridModel(month.minusMonths(1), today, selected, min, max);
    }

    MonthGridModel next() {
        return new MonthGridModel(month.plusMonths(1), today, selected, min, max);
    }
}
