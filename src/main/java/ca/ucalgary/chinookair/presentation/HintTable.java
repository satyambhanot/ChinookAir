package ca.ucalgary.chinookair.presentation;

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.LocalDateTime;
import java.util.regex.Pattern;
import javax.swing.JLabel;
import javax.swing.RowFilter;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/**
 * A table styled like the rest of the program. Headers sit on the left, except over numbers, which
 * sit on the right like the numbers below them. When there are no rows it says why, so an empty
 * table never looks broken.
 */
class HintTable extends JTable {

    private static final long serialVersionUID = 1L;

    private static final int SAMPLE = 200;

    private String emptyText;

    HintTable(TableModel model, String emptyText) {
        super(model);
        this.emptyText = emptyText;
        setFillsViewportHeight(true);
        setAutoCreateRowSorter(false);
        // a state like "Awaiting payment" is drawn as a badge
        setDefaultRenderer(Status.class, new Badge.CellRenderer());
        // a time is stored as a time, so it sorts right, and written for people here
        setDefaultRenderer(LocalDateTime.class, new TimeRenderer(false));
        JTableHeader header = getTableHeader();
        header.setReorderingAllowed(false);
        TableCellRenderer base = header.getDefaultRenderer();
        header.setDefaultRenderer((table, value, selected, focus, row, column) -> {
            Component cell = base.getTableCellRendererComponent(table, value, selected, focus, row, column);
            if (cell instanceof JLabel) {
                cell.setFont(headerFont(cell.getFont()));
                boolean number = Number.class.isAssignableFrom(table.getColumnClass(column));
                ((JLabel) cell).setHorizontalAlignment(number ? SwingConstants.RIGHT : SwingConstants.LEFT);
            }
            return cell;
        });
    }

    /** Column titles are a little smaller than the cells and semibold, so the rows are what the eye reads. */
    static Font headerFont(Font current) {
        Font semibold = UIManager.getFont("semibold.font");
        Font base = semibold != null ? semibold : current.deriveFont(Font.BOLD);
        return base.deriveFont(Type.CAPTION);
    }

    /** The renderer for a time column that also shows the year, for records that are kept for long. */
    static TableCellRenderer timeWithYearRenderer() {
        return new TimeRenderer(true);
    }

    /** Writes a {@link LocalDateTime} with {@link Format}. */
    private static final class TimeRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;
        private final boolean withYear;

        TimeRenderer(boolean withYear) {
            this.withYear = withYear;
        }

        @Override
        protected void setValue(Object value) {
            if (value instanceof LocalDateTime) {
                LocalDateTime when = (LocalDateTime) value;
                setText(withYear ? Format.dateTimeWithYear(when) : Format.dateTime(when));
            } else {
                super.setValue(value);
            }
        }
    }

    /** Every time the rows change, the columns get as wide as their longest text so nothing is cut off. */
    @Override
    public void tableChanged(TableModelEvent e) {
        super.tableChanged(e);
        fitColumns();
    }

    private void fitColumns() {
        JTableHeader header = getTableHeader();
        if (header == null || getColumnModel() == null) {
            return;
        }
        for (int c = 0; c < getColumnCount(); c++) {
            TableColumn column = getColumnModel().getColumn(c);
            Component head = header.getDefaultRenderer()
                    .getTableCellRendererComponent(this, column.getHeaderValue(), false, false, -1, c);
            int width = head.getPreferredSize().width;
            for (int r = 0; r < Math.min(getRowCount(), SAMPLE); r++) {
                width = Math.max(width, prepareRenderer(getCellRenderer(r, c), r, c).getPreferredSize().width);
            }
            column.setPreferredWidth(width + 8);
        }
    }

    /**
     * Shows only the rows whose text, in the given columns, contains the words typed. Capitals do not
     * matter and characters like "(" are taken literally. Blank text shows every row again.
     */
    @SuppressWarnings("unchecked")
    void filterRows(String text, int... columns) {
        if (getRowSorter() == null) {
            setAutoCreateRowSorter(true);
        }
        TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) getRowSorter();
        if (text == null || text.isBlank()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text.trim()), columns));
        }
    }

    /** The name a screen reader says for this table, like "Flights". */
    void describe(String name) {
        getAccessibleContext().setAccessibleName(name);
    }

    /** What to say when the table has no rows. */
    void setEmptyText(String text) {
        this.emptyText = text;
        repaint();
    }

    String emptyText() {
        return emptyText;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getRowCount() > 0 || emptyText == null || emptyText.isEmpty()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Theme.MUTED);
        FontMetrics metrics = g2.getFontMetrics();
        int x = Math.max(8, (getWidth() - metrics.stringWidth(emptyText)) / 2);
        int y = Math.min(getHeight() / 2, 120) + metrics.getAscent() / 2;
        g2.drawString(emptyText, x, y);
        g2.dispose();
    }
}
