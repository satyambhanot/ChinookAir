package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SummaryCardTest {

    @Test
    void rowsKeepTheirOrderAndTheTotalIsMarked() {
        SummaryCard card = new SummaryCard("Your trip")
                .addRow("Flight", "AC101")
                .addRow("Seat", "5C")
                .addTotal("Total", "$184.00");
        List<SummaryCard.Row> rows = card.rows();
        assertEquals(3, rows.size());
        assertEquals(new SummaryCard.Row("Flight", "AC101", false), rows.get(0));
        assertEquals("Seat", rows.get(1).label());
        assertTrue(rows.get(2).total());
        assertEquals("$184.00", rows.get(2).value());
        assertFalse(rows.get(0).total());
    }

    @Test
    void everyRowIsShownAsTwoLabels() {
        SummaryCard card = new SummaryCard("Trip").addRow("A", "1").addTotal("B", "2");
        // the title, then a name and a value for each row
        assertEquals(1 + 2 * 2, card.getComponentCount());
    }

    @Test
    void theListOfRowsCannotBeChangedFromOutside() {
        SummaryCard card = new SummaryCard("Trip").addRow("A", "1");
        assertThrows(UnsupportedOperationException.class,
                () -> card.rows().add(new SummaryCard.Row("B", "2", false)));
    }
}
