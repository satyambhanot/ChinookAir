package ca.ucalgary.chinookair.presentation;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** The key above the seat map: a small square in each seat colour, and what the colour means. */
class SeatLegend extends JPanel {

    private static final long serialVersionUID = 1L;

    SeatLegend() {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setOpaque(false);
        add(item("Business", SeatMapPanel.BUSINESS, false));
        add(item("Economy", SeatMapPanel.ECONOMY, false));
        add(item("Taken", SeatMapPanel.TAKEN, true));
        add(item("Your seat", Theme.ACCENT, false));
    }

    private static JLabel item(String text, Color color, boolean crossed) {
        JLabel label = new JLabel(text, new Swatch(color, crossed), SwingConstants.LEFT);
        label.setIconTextGap(Space.S);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, Space.L));
        label.setForeground(Theme.TEXT);
        label.setFont(label.getFont().deriveFont(Type.LABEL));
        return label;
    }

    /** A small rounded square in a seat colour; a taken seat has the cross it has on the map. */
    private static final class Swatch implements Icon {

        private static final int SIZE = 16;
        private final Color color;
        private final boolean crossed;

        Swatch(Color color, boolean crossed) {
            this.color = color;
            this.crossed = crossed;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(x, y, SIZE, SIZE, 6, 6);
            g2.setColor(Palette.BORDER_STRONG);
            g2.drawRoundRect(x, y, SIZE - 1, SIZE - 1, 6, 6);
            if (crossed) {
                g2.setColor(Theme.MUTED);
                g2.drawLine(x + 4, y + 4, x + SIZE - 5, y + SIZE - 5);
                g2.drawLine(x + SIZE - 5, y + 4, x + 4, y + SIZE - 5);
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }
    }
}
