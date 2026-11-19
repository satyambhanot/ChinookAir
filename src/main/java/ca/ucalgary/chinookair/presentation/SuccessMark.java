package ca.ucalgary.chinookair.presentation;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JLabel;

/** A green circle with a white check, for good news like a paid booking. */
final class SuccessMark extends JLabel {

    private static final long serialVersionUID = 1L;
    static final int SIZE = 40;

    SuccessMark() {
        super(Icons.of("check", 22, Color.WHITE));
        setOpaque(false);
        setHorizontalAlignment(CENTER);
        setPreferredSize(new Dimension(SIZE, SIZE));
        setMinimumSize(new Dimension(SIZE, SIZE));
        getAccessibleContext().setAccessibleName("Success");
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Theme.SUCCESS);
        g2.fillOval(0, 0, getWidth(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }
}
