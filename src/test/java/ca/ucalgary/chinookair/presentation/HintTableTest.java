package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;

class HintTableTest {

    private static DefaultTableModel model() {
        return new DefaultTableModel(new Object[] {"Name", "Seats"}, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public Class<?> getColumnClass(int column) {
                return column == 1 ? Integer.class : String.class;
            }
        };
    }

    @Test
    void anEmptyTableKeepsTheTextItWasGiven() {
        HintTable table = new HintTable(model(), "Nothing here yet.");
        assertEquals("Nothing here yet.", table.emptyText());
        table.setEmptyText("Try again.");
        assertEquals("Try again.", table.emptyText());
    }

    @Test
    void columnsGrowToFitTheirLongestText() {
        DefaultTableModel model = model();
        HintTable table = new HintTable(model, "");
        int before = table.getColumnModel().getColumn(0).getPreferredWidth();
        model.addRow(new Object[] {"A rather long name that would be cut off", 3});
        int after = table.getColumnModel().getColumn(0).getPreferredWidth();
        assertTrue(after > before + 100, before + " -> " + after);
    }

    @Test
    void headersAreOnTheLeftExceptOverNumbers() {
        HintTable table = new HintTable(model(), "");
        // Swing reuses one label for every header cell, so each is checked right after it is asked for
        Component name = table.getTableHeader().getDefaultRenderer()
                .getTableCellRendererComponent(table, "Name", false, false, -1, 0);
        assertEquals(SwingConstants.LEFT, ((JLabel) name).getHorizontalAlignment());
        Component seats = table.getTableHeader().getDefaultRenderer()
                .getTableCellRendererComponent(table, "Seats", false, false, -1, 1);
        assertEquals(SwingConstants.RIGHT, ((JLabel) seats).getHorizontalAlignment());
    }
}
