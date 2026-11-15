package ca.ucalgary.chinookair.presentation;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import javax.swing.border.AbstractBorder;

/**
 * The look of a card: a white rounded body with a thin border and a soft shadow under it, painted
 * as a border so any panel can become a card. The panel must not paint its own background, so
 * {@link #apply} also makes it see-through. The shadow lives in the bottom few pixels of the
 * panel, so a card takes no room outside its own bounds.
 */
final class SurfaceBorder extends AbstractBorder {

    private static final long serialVersionUID = 1L;

    /** The height, in pixels, kept free under the body for the shadow. */
    static final int SHADOW = 3;

    private final int top;
    private final int left;
    private final int bottom;
    private final int right;

    private SurfaceBorder(int top, int left, int bottom, int right) {
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
    }

    /** Makes the panel a card with this padding between the edge of the body and its content. */
    static void apply(JComponent panel, int top, int left, int bottom, int right) {
        panel.setOpaque(false);
        panel.setBorder(new SurfaceBorder(top, left, bottom, right));
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(1 + top, 1 + left, 1 + bottom + SHADOW, 1 + right);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        Insets own = getBorderInsets(c);
        insets.set(own.top, own.left, own.bottom, own.right);
        return insets;
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int body = height - SHADOW;
        int arc = Theme.ARC;
        g2.setColor(navy(10));
        g2.fillRoundRect(x + 1, y + 2, width - 2, body, arc, arc);
        g2.setColor(navy(7));
        g2.fillRoundRect(x + 2, y + 3, width - 4, body, arc, arc);
        g2.setColor(Theme.SURFACE);
        g2.fillRoundRect(x, y, width, body, arc, arc);
        g2.setColor(Theme.BORDER);
        g2.drawRoundRect(x, y, width - 1, body - 1, arc, arc);
        g2.dispose();
    }

    private static Color navy(int alpha) {
        return new Color(Palette.NAVY_900.getRed(), Palette.NAVY_900.getGreen(), Palette.NAVY_900.getBlue(), alpha);
    }
}
