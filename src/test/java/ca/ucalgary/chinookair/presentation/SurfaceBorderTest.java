package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SurfaceBorderTest {

    private static final int W = 120;
    private static final int H = 80;

    @BeforeAll
    static void install() {
        Theme.install();
    }

    private static JPanel card() {
        JPanel panel = new JPanel();
        SurfaceBorder.apply(panel, Space.L, Space.XL, Space.S, Space.XL);
        panel.setSize(W, H);
        return panel;
    }

    private static BufferedImage paintOnPage(JPanel panel) {
        BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Palette.PAGE);
        g.fillRect(0, 0, W, H);
        panel.paint(g);
        g.dispose();
        return image;
    }

    @Test
    void theCardKeepsItsPaddingBorderLineAndShadowRoom() {
        Insets in = card().getBorder().getBorderInsets(card());
        assertEquals(1 + Space.L, in.top);
        assertEquals(1 + Space.XL, in.left);
        assertEquals(1 + Space.S + SurfaceBorder.SHADOW, in.bottom);
        assertEquals(1 + Space.XL, in.right);
    }

    @Test
    void thePanelIsMadeSeeThroughSoTheBorderDrawsTheBody() {
        assertFalse(card().isOpaque());
    }

    @Test
    void theBodyIsWhiteTheCornersShowThePageAndTheShadowDarkensItBelow() {
        BufferedImage image = paintOnPage(card());
        assertEquals(Palette.SURFACE.getRGB(), image.getRGB(W / 2, H / 2));
        assertEquals(Palette.PAGE.getRGB(), image.getRGB(0, 0), "the corner is rounded off");
        int below = image.getRGB(W / 2, H - 1);
        assertNotEquals(Palette.PAGE.getRGB(), below, "there is a soft shadow under the body");
        assertEquals(Palette.BORDER.getRGB(), image.getRGB(0, H / 2), "a thin border on the side");
    }
}
