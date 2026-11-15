package ca.ucalgary.chinookair.presentation;

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

/**
 * A table styled like the rest of the program. Headers sit on the left, except over numbers, which
 * sit on the right like the numbers below them. When there are no rows it says why, so an empty
 * table never looks broken.
 */
class HintTable extends JTable {

    private static final long serialVersionUID = 1L;

    private String emptyText;

    HintTable(TableModel model, String emptyText) {
        super(model);
        this.emptyText = emptyText;
        setFillsViewportHeight(true);
        setAutoCreateRowSorter(false);
        JTableHeader header = getTableHeader();
        header.setReorderingAllowed(false);
        TableCellRenderer base = header.getDefaultRenderer();
        header.setDefaultRenderer((table, value, selected, focus, row, column) -> {
            Component cell = base.getTableCellRendererComponent(table, value, selected, focus, row, column);
            if (cell instanceof JLabel) {
                boolean number = Number.class.isAssignableFrom(table.getColumnClass(column));
                ((JLabel) cell).setHorizontalAlignment(number ? SwingConstants.RIGHT : SwingConstants.LEFT);
            }
            return cell;
        });
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
