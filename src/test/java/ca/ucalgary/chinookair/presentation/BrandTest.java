package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.jupiter.api.Test;

class BrandTest {

    @Test
    void theFullLogoIsWiderThanTallAndNotBlank() {
        BufferedImage logo = Brand.fullLogo();
        assertTrue(logo.getWidth() > logo.getHeight());
        assertTrue(hasDarkPixels(logo), "the logo should contain the navy of the name");
    }

    @Test
    void theMarkIsTheLogoWithoutTheName() {
        // the name takes the lower part of the full logo, so the mark is shorter for the same width
        double fullShape = (double) Brand.fullLogo().getHeight() / Brand.fullLogo().getWidth();
        double markShape = (double) Brand.mark().getHeight() / Brand.mark().getWidth();
        assertTrue(markShape < fullShape, markShape + " vs " + fullShape);
        assertTrue(hasDarkPixels(Brand.mark()));
    }

    @Test
    void thereIsASquareIconForEverySize() {
        List<Image> icons = Brand.icons();
        assertEquals(Brand.ICON_SIZES.length, icons.size());
        for (int i = 0; i < icons.size(); i++) {
            assertEquals(Brand.ICON_SIZES[i], icons.get(i).getWidth(null));
            assertEquals(Brand.ICON_SIZES[i], icons.get(i).getHeight(null));
        }
    }

    @Test
    void theCornersOfAnIconAreTransparentSoItLooksRounded() {
        BufferedImage big = (BufferedImage) Brand.icons().get(Brand.icons().size() - 1);
        assertEquals(0, big.getRGB(0, 0) >>> 24);
        assertEquals(255, big.getRGB(big.getWidth() / 2, 4) >>> 24);
    }

    @Test
    void scalingKeepsTheShapeOfThePicture() {
        BufferedImage logo = Brand.fullLogo();
        Image small = Brand.scaledToWidth(logo, 240);
        double expected = (double) logo.getHeight() / logo.getWidth() * 240;
        assertEquals(240, small.getWidth(null));
        assertEquals(expected, small.getHeight(null), 1.0);
    }

    @Test
    void theIconListCannotBeChangedByACaller() {
        List<Image> icons = Brand.icons();
        assertFalse(icons.isEmpty());
        try {
            icons.add(null);
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("the list should be read-only");
    }

    private static boolean hasDarkPixels(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y += 4) {
            for (int x = 0; x < image.getWidth(); x += 4) {
                int rgb = image.getRGB(x, y);
                if ((rgb >> 16 & 255) < 40 && (rgb >> 8 & 255) < 80 && (rgb & 255) < 120) {
                    return true;
                }
            }
        }
        return false;
    }
}
