package ca.ucalgary.chinookair.presentation;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

/** The line between two airports on a flight card: dots at both ends and the flight time above it. */
final class FlightLine extends JComponent {

    private static final long serialVersionUID = 1L;
    private static final int WIDTH = 150;
    private static final int HEIGHT = 34;
    private static final int DOT = 8;

    private final boolean onDark;
    private String text = "";

    FlightLine() {
        this(false);
    }

    /** {@code onDark} draws it in light colours, for the navy band of an itinerary. */
    FlightLine(boolean onDark) {
        this.onDark = onDark;
        setOpaque(false);
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setMinimumSize(new Dimension(WIDTH, HEIGHT));
    }

    void setText(String text) {
        this.text = text;
        repaint();
    }

    String text() {
        return text;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int line = getHeight() - DOT;
        g2.setColor(onDark ? Palette.SKY_500 : Palette.BORDER_STRONG);
        g2.drawLine(DOT, line, getWidth() - DOT, line);
        g2.setColor(onDark ? Palette.GOLD_500 : Palette.BLUE_700);
        g2.fillOval(0, line - DOT / 2, DOT, DOT);
        g2.fillOval(getWidth() - DOT, line - DOT / 2, DOT, DOT);
        g2.setFont(getFont().deriveFont(Type.CAPTION));
        g2.setColor(onDark ? Palette.SKY_100 : Theme.MUTED);
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2, line - DOT);
        g2.dispose();
    }
}
