package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DayStripTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 12, 1);

    @BeforeAll
    static void install() {
        Theme.install();
    }

    @Test
    void itShowsTheChosenDayAndThreeEitherSideWithTheChosenOneOn() {
        DayStrip strip = new DayStrip();
        strip.show(LocalDate.of(2026, 12, 10), TODAY, day -> Optional.of(new BigDecimal("100.00")));
        assertEquals(7, strip.days().size());
        assertEquals(LocalDate.of(2026, 12, 7), strip.dates().get(0));
        assertEquals(LocalDate.of(2026, 12, 13), strip.dates().get(6));
        for (int i = 0; i < 7; i++) {
            assertEquals(i == 3, strip.days().get(i).isSelected(), "day " + i);
        }
    }

    @Test
    void aDayWithNoFlightsShowsADashAndNamesThatForAScreenReader() {
        DayStrip strip = new DayStrip();
        strip.show(LocalDate.of(2026, 12, 10), TODAY,
                day -> day.getDayOfMonth() == 11 ? Optional.empty() : Optional.of(new BigDecimal("100.00")));
        assertTrue(strip.days().get(4).getText().contains("–"));
        assertTrue(strip.days().get(4).getAccessibleContext().getAccessibleName().contains("no flights"));
        assertTrue(strip.days().get(5).getText().contains("$100.00"));
    }

    @Test
    void aDayBeforeTodayCannotBePicked() {
        DayStrip strip = new DayStrip();
        strip.show(LocalDate.of(2026, 12, 2), TODAY, day -> Optional.of(new BigDecimal("100.00")));
        assertFalse(strip.days().get(0).isEnabled(), "Nov 29");
        assertFalse(strip.days().get(1).isEnabled(), "Nov 30");
        assertTrue(strip.days().get(2).isEnabled(), "today");
    }

    @Test
    void pickingAnotherDayTellsTheListenerButThePickedOneDoesNot() {
        DayStrip strip = new DayStrip();
        List<LocalDate> picked = new ArrayList<>();
        strip.onPick(picked::add);
        strip.show(LocalDate.of(2026, 12, 10), TODAY, day -> Optional.of(new BigDecimal("100.00")));
        strip.days().get(3).doClick(0);
        assertTrue(picked.isEmpty(), "the chosen day is already chosen");
        strip.days().get(5).doClick(0);
        assertEquals(List.of(LocalDate.of(2026, 12, 12)), picked);
    }

    @Test
    void thePricesAreLookedUpOnlyForDaysThatCanBePicked() {
        DayStrip strip = new DayStrip();
        List<LocalDate> asked = new ArrayList<>();
        strip.show(LocalDate.of(2026, 12, 2), TODAY, day -> {
            asked.add(day);
            return Optional.empty();
        });
        assertEquals(5, asked.size(), "five days from today on, not the two in the past");
        assertTrue(asked.stream().noneMatch(d -> d.isBefore(TODAY)));
    }
}
