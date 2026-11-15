package ca.ucalgary.chinookair.presentation;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Optional;
import javax.swing.JPanel;

/**
 * A thin strip under the top bar that shows where the user is in a task of several screens, like
 * "Find a flight, Choose a seat, Pay, Done". Finished steps show a check, the current one is bold and
 * blue, the next ones are grey. It is hidden when the screen is not part of such a task.
 */
class StepStrip extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int HEIGHT = 44;
    private static final int DOT = 22;
    private static final int LINK = 36;

    private transient StepperModel.Flow flow;

    StepStrip() {
        setBackground(Theme.SURFACE);
        setPreferredSize(new Dimension(10, HEIGHT));
        setVisible(false);
    }

    void show(Optional<StepperModel.Flow> next) {
        this.flow = next.orElse(null);
        setVisible(flow != null);
        getAccessibleContext().setAccessibleDescription(description());
        repaint();
    }

    StepperModel.Flow flow() {
        return flow;
    }

    /** Like "Step 2 of 4: Choose a seat", for a screen reader and for tests. */
    String description() {
        return flow == null ? "" : "Step " + (flow.current() + 1) + " of " + flow.steps().size() + ": "
                + flow.steps().get(flow.current());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (flow == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Theme.BORDER);
        g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);

        Font plain = getFont().deriveFont(Font.PLAIN, Type.LABEL + 1);
        Font bold = plain.deriveFont(Font.BOLD);
        int x = Theme.PAGE;
        int middle = (getHeight() - 1) / 2;
        for (int i = 0; i < flow.steps().size(); i++) {
            boolean done = i < flow.current();
            boolean current = i == flow.current();
            g2.setColor(done || current ? Theme.ACCENT : Palette.BORDER_STRONG);
            if (done || current) {
                g2.fillOval(x, middle - DOT / 2, DOT, DOT);
            } else {
                g2.setStroke(new java.awt.BasicStroke(1.5f));
                g2.drawOval(x, middle - DOT / 2, DOT - 1, DOT - 1);
            }
            if (done) {
                Icons.of("check", 14, java.awt.Color.WHITE).paintIcon(this, g2, x + (DOT - 14) / 2,
                        middle - 7);
            } else {
                g2.setFont(bold.deriveFont(Type.CAPTION));
                g2.setColor(current ? java.awt.Color.WHITE : Theme.MUTED);
                String number = String.valueOf(i + 1);
                FontMetrics numberMetrics = g2.getFontMetrics();
                g2.drawString(number, x + (DOT - numberMetrics.stringWidth(number)) / 2,
                        middle + (numberMetrics.getAscent() - numberMetrics.getDescent()) / 2);
            }
            g2.setFont(current ? bold : plain);
            g2.setColor(current || done ? Theme.TEXT : Theme.MUTED);
            FontMetrics metrics = g2.getFontMetrics();
            x += DOT + 8;
            g2.drawString(flow.steps().get(i), x, middle + (metrics.getAscent() - metrics.getDescent()) / 2);
            x += metrics.stringWidth(flow.steps().get(i)) + 12;
            if (i < flow.steps().size() - 1) {
                g2.setColor(done ? Theme.ACCENT : Palette.BORDER_STRONG);
                g2.fillRect(x, middle, LINK, 2);
                x += LINK + 12;
            }
        }
        g2.dispose();
    }
}
