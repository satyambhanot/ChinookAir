package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TableAreaTest {

    @BeforeAll
    static void install() {
        Theme.install();
    }

    private static DefaultTableModel rows(int count) {
        DefaultTableModel model = new DefaultTableModel(new Object[] {"Name"}, 0);
        for (int i = 0; i < count; i++) {
            model.addRow(new Object[] {"Row " + i});
        }
        return model;
    }

    private static TableArea area(DefaultTableModel model, int width, int height) {
        TableArea area = new TableArea(new HintTable(model, "Nothing here"));
        area.setSize(width, height);
        area.doLayout();
        return area;
    }

    @Test
    void aShortTableIsAsTallAsFiveRowsNotAsTheWindow() {
        TableArea area = area(rows(1), 800, 600);
        int fiveRows = area.scrollPane().getHeight();
        assertTrue(fiveRows < 600, "not the whole window: " + fiveRows);
        assertTrue(fiveRows >= TableArea.MIN_ROWS * 40, "room for five rows: " + fiveRows);
        assertEquals(fiveRows, area(rows(0), 800, 600).scrollPane().getHeight(), "an empty table is the same height");
        assertEquals(fiveRows, area(rows(5), 800, 600).scrollPane().getHeight());
    }

    @Test
    void aLongerTableGrowsWithItsRowsUntilTheWindowIsFull() {
        int five = area(rows(5), 800, 900).scrollPane().getHeight();
        int ten = area(rows(10), 800, 900).scrollPane().getHeight();
        assertTrue(ten > five + 4 * 40, five + " then " + ten);
        assertEquals(300, area(rows(60), 800, 300).scrollPane().getHeight(), "it never outgrows the room");
    }

    @Test
    void addingRowsMakesItWantToBeTaller() {
        DefaultTableModel model = rows(2);
        TableArea area = area(model, 800, 900);
        int before = area.wantedHeight();
        for (int i = 0; i < 8; i++) {
            model.addRow(new Object[] {"More"});
        }
        assertTrue(area.wantedHeight() > before);
    }
}
