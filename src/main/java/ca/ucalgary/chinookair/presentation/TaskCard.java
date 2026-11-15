package ca.ucalgary.chinookair.presentation;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.SwingConstants;

/**
 * A big button for one task on a dashboard: the name in bold and a line under it that says what the
 * task is for. It is still a plain button, so it can be clicked, tabbed to and pressed with its key.
 */
class TaskCard extends JButton {

    private static final long serialVersionUID = 1L;
    private static final int PAD = 20;

    private final String description;

    TaskCard(String label, String description, String icon) {
        super(label);
        this.description = description;
        setIcon(new Tile(icon));
        setIconTextGap(16);
        setHorizontalAlignment(SwingConstants.LEFT);
        setVerticalAlignment(SwingConstants.TOP);
        setHorizontalTextPosition(SwingConstants.RIGHT);
        setVerticalTextPosition(SwingConstants.TOP);
        setFont(getFont().deriveFont(Font.BOLD, Type.SECTION));
        setMargin(new Insets(PAD - 2, PAD, PAD - 2, PAD));
        setPreferredSize(new Dimension(300, 96));
        putClientProperty("FlatLaf.styleClass", "card");
    }

    /** The line under the name. */
    String description() {
        return description;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(getFont().deriveFont(Font.PLAIN, Type.LABEL));
        g2.setColor(Theme.MUTED);
        FontMetrics small = g2.getFontMetrics();
        Insets inset = getInsets();
        int top = inset.top + getFontMetrics(getFont()).getHeight() + small.getAscent() + 6;
        g2.drawString(description, inset.left + getIcon().getIconWidth() + getIconTextGap(), top);
        g2.dispose();
    }

    /** A pale blue rounded square with the icon of the task in the middle. */
    private static final class Tile implements Icon {

        private static final int SIZE = 44;

        private final String name;

        Tile(String name) {
            this.name = name;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Palette.SKY_100);
            g2.fillRoundRect(x, y, SIZE, SIZE, 12, 12);
            Icons.of(name, 22, Theme.ACCENT).paintIcon(c, g2, x + (SIZE - 22) / 2, y + (SIZE - 22) / 2);
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
