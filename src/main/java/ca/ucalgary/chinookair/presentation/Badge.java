package ca.ucalgary.chinookair.presentation;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/** A small pill with a dot and the status in words, tinted by the kind of status. */
class Badge extends JLabel {

    private static final long serialVersionUID = 1L;
    private static final int DOT = 8;

    private final Status.Kind kind;

    Badge(Status status) {
        super(status.text());
        this.kind = status.kind();
        setOpaque(false);
        setFont(getFont().deriveFont(Font.BOLD, Type.CAPTION));
        setForeground(kind.text());
        setIcon(new DotIcon());
        setIconTextGap(6);
        setBorder(BorderFactory.createEmptyBorder(3, Space.M, 3, Space.M));
    }

    Status.Kind kind() {
        return kind;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(kind.fill());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }

    /** The coloured dot in front of the words. */
    private final class DotIcon implements Icon {

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(kind.text());
            g2.fillOval(x, y, DOT, DOT);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return DOT;
        }

        @Override
        public int getIconHeight() {
            return DOT;
        }
    }

    /** Shows a {@link Status} cell as a badge, on the row's own background so selection still shows. */
    static final class CellRenderer implements TableCellRenderer {

        private final JPanel holder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                boolean focus, int row, int column) {
            holder.removeAll();
            holder.setOpaque(true);
            holder.setBackground(selected ? table.getSelectionBackground() : table.getBackground());
            holder.setBorder(BorderFactory.createEmptyBorder(Space.S, Space.M, Space.S, Space.M));
            if (value instanceof Status) {
                holder.add(new Badge((Status) value));
            } else if (value != null) {
                holder.add(new JLabel(value.toString()));
            }
            return holder;
        }
    }
}
