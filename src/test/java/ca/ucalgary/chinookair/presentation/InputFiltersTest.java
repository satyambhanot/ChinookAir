package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.JTextField;
import org.junit.jupiter.api.Test;

class InputFiltersTest {

    private static JTextField box(java.util.function.Consumer<JTextField> filter) {
        JTextField field = new JTextField();
        filter.accept(field);
        return field;
    }

    @Test
    void aDigitsBoxDropsEverythingElseAndStopsAtItsLength() {
        JTextField field = box(f -> InputFilters.digits(f, 3));
        field.setText("a1b2c3d4");
        assertEquals("123", field.getText());
        field.setText("");
        field.setText("12-5");
        assertEquals("125", field.getText());
    }

    @Test
    void typingAtTheEndOfAFullBoxChangesNothing() throws Exception {
        JTextField field = box(f -> InputFilters.digits(f, 2));
        field.setText("12");
        field.getDocument().insertString(2, "3", null);
        assertEquals("12", field.getText());
    }

    @Test
    void aChosenCharacterClassKeepsOnlyThoseCharacters() {
        JTextField field = box(f -> InputFilters.only(f, "0-9/", 5));
        field.setText("12/30abc");
        assertEquals("12/30", field.getText());
        field.setText("1x2");
        assertEquals("12", field.getText());
    }

    @Test
    void aDecimalBoxTakesDigitsAndOneDotAndNoSign() {
        JTextField field = box(f -> InputFilters.decimal(f, 8));
        field.setText("199.99");
        assertEquals("199.99", field.getText());
        field.setText("1.2.3");
        assertEquals("199.99", field.getText(), "a second dot is refused and the text stays");
        field.setText("-5");
        assertEquals("199.99", field.getText(), "no minus sign");
        field.setText("abc");
        assertEquals("199.99", field.getText());
    }

    @Test
    void aDecimalBoxHasALengthLimitToo() {
        JTextField field = box(f -> InputFilters.decimal(f, 5));
        field.setText("12345");
        field.setText("123456");
        assertEquals("12345", field.getText());
    }

    @Test
    void deletingAlwaysWorks() throws Exception {
        JTextField field = box(f -> InputFilters.digits(f, 5));
        field.setText("12345");
        field.getDocument().remove(1, 2);
        assertEquals("145", field.getText());
        field.setText("");
        assertEquals("", field.getText());
    }

    @Test
    void theBoxStillAcceptsATextThatIsOnlyPartlyRight() {
        JTextField field = box(f -> InputFilters.only(f, "0-9 +()-", 20));
        field.setText("+1 (403) 555-0100 ext");
        assertEquals("+1 (403) 555-0100 ", field.getText().substring(0, 18));
    }
}
