package ca.ucalgary.chinookair.presentation;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * A table in a scroll pane that is only as tall as its rows, with room for a few rows at least, so a
 * table with one row is not a tall white box. When there are more rows than the window has room for,
 * it fills the room and scrolls.
 */
final class TableArea extends JPanel {

    private static final long serialVersionUID = 1L;

    /** The fewest rows it makes room for, so the empty text has somewhere to sit. */
    static final int MIN_ROWS = 5;

    private final HintTable table;
    private final JScrollPane scroll;

    TableArea(HintTable table) {
        super(null);
        this.table = table;
        this.scroll = new JScrollPane(table);
        setOpaque(false);
        setLayout(new Fitting());
        add(scroll);
        // more or fewer rows change how tall it wants to be
        table.getModel().addTableModelListener(e -> revalidate());
    }

    /** The height the table wants, in pixels, for the rows it has now. */
    int wantedHeight() {
        int rows = Math.max(MIN_ROWS, table.getRowCount());
        int body = Math.max(table.getPreferredSize().height, rows * (table.getRowHeight() + table.getRowMargin()));
        Insets in = scroll.getInsets();
        return table.getTableHeader().getPreferredSize().height + body + in.top + in.bottom;
    }

    JScrollPane scrollPane() {
        return scroll;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(super.getPreferredSize().width, wantedHeight());
    }

    private final class Fitting implements LayoutManager {

        @Override
        public void addLayoutComponent(String name, Component comp) {
            // the one child is found through the container
        }

        @Override
        public void removeLayoutComponent(Component comp) {
            // nothing is kept
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return new Dimension(scroll.getPreferredSize().width, wantedHeight());
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return new Dimension(0, 0);
        }

        @Override
        public void layoutContainer(Container parent) {
            scroll.setBounds(0, 0, parent.getWidth(), Math.min(parent.getHeight(), wantedHeight()));
        }
    }
}
