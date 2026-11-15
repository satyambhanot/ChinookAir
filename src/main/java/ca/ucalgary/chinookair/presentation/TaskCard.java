package ca.ucalgary.chinookair.presentation;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
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

    TaskCard(String label, String description) {
        super(label);
        this.description = description;
        setHorizontalAlignment(SwingConstants.LEFT);
        setVerticalAlignment(SwingConstants.TOP);
        setFont(getFont().deriveFont(Font.BOLD, 17f));
        setMargin(new Insets(PAD, PAD, PAD, PAD));
        setPreferredSize(new Dimension(300, 112));
        putClientProperty("FlatLaf.style",
                "background: #FFFFFF; borderColor: #DDE2E8; hoverBorderColor: #0B63CE; "
                        + "hoverBackground: #F3F8FE; pressedBackground: #E8F1FC; arc: 14; "
                        + "focusedBorderColor: #0B63CE; borderWidth: 1");
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
        g2.setFont(getFont().deriveFont(Font.PLAIN, 13f));
        g2.setColor(Theme.MUTED);
        FontMetrics small = g2.getFontMetrics();
        Insets inset = getInsets();
        int top = inset.top + getFontMetrics(getFont()).getHeight() + small.getAscent() + 6;
        g2.drawString(description, inset.left, top);
        g2.dispose();
    }
}
