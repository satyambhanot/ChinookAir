package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;

class IconsTest {

    @Test
    void everyNamedIconHasAnSvgFileThatLoads() {
        for (String name : Icons.NAMES) {
            FlatSVGIcon icon = Icons.of(name, 20, Color.RED);
            assertTrue(icon.hasFound(), name + " is missing");
            assertEquals(20, icon.getIconWidth());
            assertEquals(20, icon.getIconHeight());
        }
    }

    @Test
    void everySvgFileIsInTheNameList() throws Exception {
        try (Stream<Path> files = Files.list(Path.of("src/main/resources/icons"))) {
            List<String> onDisk = files.map(p -> p.getFileName().toString().replace(".svg", "")).sorted().toList();
            assertEquals(Icons.NAMES.stream().sorted().toList(), onDisk);
        }
    }

    @Test
    void theSvgFilesAreWellFormed() throws Exception {
        for (String name : Icons.NAMES) {
            DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(Path.of("src/main/resources/icons/" + name + ".svg").toFile());
        }
    }

    @Test
    void theIconIsPaintedInTheColourAskedFor() {
        BufferedImage image = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = image.createGraphics();
        Icons.of("check", 24, new Color(0x00AA00)).paintIcon(null, g, 0, 0);
        g.dispose();
        boolean green = false;
        boolean black = false;
        for (int y = 0; y < 24; y++) {
            for (int x = 0; x < 24; x++) {
                int argb = image.getRGB(x, y);
                if ((argb >>> 24) > 200) {
                    green |= (argb >> 8 & 255) > 120 && (argb >> 16 & 255) < 40;
                    black |= (argb & 0xFFFFFF) == 0;
                }
            }
        }
        assertTrue(green, "the strokes should be green");
        assertTrue(!black, "no stroke should stay black");
    }

    @Test
    void anUnknownIconNameIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Icons.of("rocket", 16, Color.BLACK));
    }
}
