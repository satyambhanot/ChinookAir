package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import org.junit.jupiter.api.Test;

class DatePickerFieldTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 12, 1);

    private final DatePickerField field = new DatePickerField(10, () -> TODAY);

    private static void collect(Container root, List<JButton> out) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton) {
                out.add((JButton) c);
            }
            if (c instanceof Container) {
                collect((Container) c, out);
            }
        }
    }

    private JButton button(String name) {
        List<JButton> all = new ArrayList<>();
        collect(field.calendar(), all);
        return all.stream().filter(b -> name.equals(b.getName())).findFirst().orElseThrow();
    }

    private String monthTitle() {
        return titleOf(field.calendar());
    }

    private static String titleOf(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JLabel && "month title".equals(c.getName())) {
                return ((JLabel) c).getText();
            }
            if (c instanceof Container) {
                String found = titleOf((Container) c);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @Test
    void itShowsTheShapeOfADateAndACalendarButton() {
        assertEquals("yyyy-MM-dd", field.getClientProperty("JTextField.placeholderText"));
        assertNotNull(field.getClientProperty("JTextField.trailingComponent"));
    }

    @Test
    void theTextIsReadAsADateOrIsEmptyWhenItIsNot() {
        field.setText("2026-12-24");
        assertEquals(LocalDate.of(2026, 12, 24), field.date().orElseThrow());
        field.setText(" 2026-12-24 ");
        assertTrue(field.date().isPresent());
        field.setText("24/12/2026");
        assertTrue(field.date().isEmpty());
        field.setText("");
        assertTrue(field.date().isEmpty());
    }

    @Test
    void setDateWritesTheIsoText() {
        field.setDate(LocalDate.of(2027, 1, 5));
        assertEquals("2027-01-05", field.getText());
        field.setDate(null);
        assertEquals("", field.getText());
    }

    @Test
    void theCalendarOpensOnThisMonthWhenThereIsNoDate() {
        field.openCalendar();
        assertEquals("December 2026", monthTitle());
    }

    @Test
    void theCalendarOpensOnTheMonthOfTheTypedDate() {
        field.setText("2027-03-15");
        field.openCalendar();
        assertEquals("March 2027", monthTitle());
        field.setText("not a date");
        field.openCalendar();
        assertEquals("December 2026", monthTitle());
    }

    @Test
    void clickingADayFillsTheField() {
        field.openCalendar();
        button("2026-12-24").doClick(0);
        assertEquals("2026-12-24", field.getText());
    }

    @Test
    void theChosenDayAndTodayLookDifferentFromTheRest() {
        field.setText("2026-12-15");
        field.openCalendar();
        assertEquals("primary day", button("2026-12-15").getClientProperty("FlatLaf.styleClass"));
        assertEquals("today day", button("2026-12-01").getClientProperty("FlatLaf.styleClass"));
        assertEquals("day", button("2026-12-20").getClientProperty("FlatLaf.styleClass"));
    }

    @Test
    void earlierDaysOfTheSameMonthAreDisabled() {
        field.setMinDate(LocalDate.of(2026, 12, 10));
        field.openCalendar();
        assertFalse(button("2026-12-09").isEnabled());
        assertTrue(button("2026-12-10").isEnabled());
    }

    @Test
    void theMonthButtonsPageThroughTheYear() {
        field.openCalendar();
        button("Next month").doClick(0);
        assertEquals("January 2027", monthTitle());
        button("Previous month").doClick(0);
        button("Previous month").doClick(0);
        assertEquals("November 2026", monthTitle());
    }

    @Test
    void thePreviousMonthIsDisabledAtTheMinimum() {
        field.setMinDate(TODAY);
        field.openCalendar();
        assertFalse(button("Previous month").isEnabled());
    }

    @Test
    void theNextMonthIsDisabledAtTheMaximum() {
        field.setMaxDate(LocalDate.of(2026, 12, 31));
        field.openCalendar();
        assertFalse(button("Next month").isEnabled());
    }
}
