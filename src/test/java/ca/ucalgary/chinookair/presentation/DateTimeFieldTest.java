package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DateTimeFieldTest {

    @BeforeAll
    static void install() {
        Theme.install();
    }

    private static DateTimeField field() {
        return new DateTimeField(() -> LocalDate.of(2026, 12, 1));
    }

    @Test
    void itReadsAndWritesTheSameTextAsOneBoxWould() {
        DateTimeField field = field();
        field.setText("2026-12-02 14:30");
        assertEquals("2026-12-02", field.date.getText());
        assertEquals("14:30", field.time.getText());
        assertEquals("2026-12-02 14:30", field.getText());
        assertEquals(InputParser.dateTime("2026-12-02 14:30"), InputParser.dateTime(field.getText()));
    }

    @Test
    void textWithoutATimeFillsOnlyTheDayAndComesBackWithoutOne() {
        DateTimeField field = field();
        field.setText("tomorrow");
        assertEquals("tomorrow", field.date.getText());
        assertEquals("", field.time.getText());
        assertEquals("tomorrow", field.getText());
    }

    @Test
    void emptyTextEmptiesBothBoxes() {
        DateTimeField field = field();
        field.setText("2026-12-02 14:30");
        field.setText("");
        assertEquals("", field.getText());
        field.setText(null);
        assertEquals("", field.getText());
    }

    @Test
    void enterInEitherBoxIsHeard() {
        DateTimeField field = field();
        AtomicInteger heard = new AtomicInteger();
        field.addActionListener(e -> heard.incrementAndGet());
        field.date.postActionEvent();
        field.time.postActionEvent();
        assertEquals(2, heard.get());
    }

    @Test
    void disablingItDisablesBothBoxes() {
        DateTimeField field = field();
        field.setEnabled(false);
        assertFalse(field.date.isEnabled());
        assertFalse(field.time.isEnabled());
    }
}
