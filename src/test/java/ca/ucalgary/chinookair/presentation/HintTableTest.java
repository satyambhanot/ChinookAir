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
    void aStatusColumnIsDrawnAsABadge() {
        HintTable table = new HintTable(model(), "");
        assertTrue(table.getDefaultRenderer(Status.class) instanceof Badge.CellRenderer);
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

    private static HintTable tableWith(String... names) {
        DefaultTableModel model = model();
        for (String name : names) {
            model.addRow(new Object[] {name, 1});
        }
        return new HintTable(model, "");
    }

    @Test
    void filteringKeepsOnlyTheRowsThatContainTheWords() {
        HintTable table = tableWith("AC101 Calgary", "WS302 Toronto", "ac555 Edmonton");
        table.filterRows("ac", 0);
        assertEquals(2, table.getRowCount(), "capitals do not matter");
        table.filterRows("toronto", 0);
        assertEquals(1, table.getRowCount());
        table.filterRows("   ", 0);
        assertEquals(3, table.getRowCount(), "blank text shows everything again");
        table.filterRows(null, 0);
        assertEquals(3, table.getRowCount());
    }

    @Test
    void filteringTreatsSpecialCharactersAsPlainText() {
        HintTable table = tableWith("YYC (main)", "YVR");
        table.filterRows("(main", 0);
        assertEquals(1, table.getRowCount());
        table.filterRows(".*", 0);
        assertEquals(0, table.getRowCount());
    }

    @Test
    void filteringLooksOnlyInTheGivenColumns() {
        DefaultTableModel model = model();
        model.addRow(new Object[] {"alpha", 7});
        HintTable table = new HintTable(model, "");
        table.filterRows("7", 0);
        assertEquals(0, table.getRowCount());
        table.filterRows("7", 1);
        assertEquals(1, table.getRowCount());
    }

    @Test
    void theRowsThatShowCanBeMappedBackToTheModel() {
        HintTable table = tableWith("one", "two", "three");
        table.filterRows("t", 0);
        assertEquals(2, table.getRowCount());
        assertEquals("three", table.getModel().getValueAt(table.convertRowIndexToModel(1), 0));
    }

    @Test
    void aTableCanBeNamedForAScreenReader() {
        HintTable table = tableWith("one");
        table.describe("Flights");
        assertEquals("Flights", table.getAccessibleContext().getAccessibleName());
    }

    @Test
    void columnTitlesAreSmallAndSemibold() {
        HintTable table = new HintTable(new javax.swing.table.DefaultTableModel(new Object[] {"Departs"}, 0), "none");
        java.awt.Font font = table.getTableHeader().getDefaultRenderer()
                .getTableCellRendererComponent(table, "Departs", false, false, -1, 0).getFont();
        assertEquals(Type.CAPTION, font.getSize2D());
        assertTrue(font.isBold() || font.getName().toLowerCase().contains("semibold"), font.getName());
    }
}
