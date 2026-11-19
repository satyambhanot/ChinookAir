package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SuccessMarkTest {

    @BeforeAll
    static void install() {
        Theme.install();
    }

    private static BufferedImage paint() {
        SuccessMark mark = new SuccessMark();
        mark.setSize(SuccessMark.SIZE, SuccessMark.SIZE);
        BufferedImage image = new BufferedImage(SuccessMark.SIZE, SuccessMark.SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        mark.paint(g);
        g.dispose();
        return image;
    }

    @Test
    void theMarkIsACircleNotASquare() {
        BufferedImage image = paint();
        assertEquals(0, image.getRGB(1, 1) >>> 24, "the corner is left clear");
        assertEquals(Theme.SUCCESS.getRGB(), image.getRGB(4, SuccessMark.SIZE / 2), "the edge of the circle is green");
    }

    @Test
    void itHasAnAccessibleName() {
        assertEquals("Success", new SuccessMark().getAccessibleContext().getAccessibleName());
    }
}
