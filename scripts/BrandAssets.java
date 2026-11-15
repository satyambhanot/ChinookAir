import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Makes the logo files the program uses from the original picture.
 *
 * Run it from the project folder:  java scripts/BrandAssets.java
 *
 * It reads docs/brand/chinookair-logo-original.png and writes into src/main/resources/branding:
 *   logo-full.png   the whole logo (mark and name), cropped to its content
 *   mark.png        the mountain and plane without the name
 *   icon-N.png      square window icons with a white rounded tile, for N = 16 .. 512
 */
public class BrandAssets {

    private static final int WHITE_LIMIT = 235;
    private static final int[] ICON_SIZES = {16, 32, 64, 128, 256, 512};

    public static void main(String[] args) throws IOException {
        BufferedImage original = ImageIO.read(new File("docs/brand/chinookair-logo-original.png"));
        File out = new File("src/main/resources/branding");
        out.mkdirs();

        // the name starts below the trails; everything above that line is the mark
        int nameTop = findNameTop(original);
        int[] all = box(original, 0, original.getHeight());
        int[] mark = box(original, 0, nameTop);

        BufferedImage full = crop(original, all, 24);
        write(scaleToWidth(full, 720), new File(out, "logo-full.png"));

        BufferedImage markImage = crop(original, mark, 24);
        write(scaleToWidth(markImage, 640), new File(out, "mark.png"));

        for (int size : ICON_SIZES) {
            write(icon(markImage, size), new File(out, "icon-" + size + ".png"));
        }
        System.out.println("name starts at y=" + nameTop + ", mark box " + mark[0] + "," + mark[1] + " to "
                + mark[2] + "," + mark[3]);
    }

    private static boolean ink(int rgb) {
        return (rgb >> 16 & 255) < WHITE_LIMIT || (rgb >> 8 & 255) < WHITE_LIMIT || (rgb & 255) < WHITE_LIMIT;
    }

    /** The first row of the name: the tallest empty band between the trails and the letters. */
    private static int findNameTop(BufferedImage image) {
        int bestStart = -1;
        int bestLength = 0;
        int start = -1;
        // search the lower half, where the gap between the mark and the name is
        for (int y = image.getHeight() / 2; y < image.getHeight(); y++) {
            boolean empty = true;
            for (int x = 0; x < image.getWidth() && empty; x++) {
                empty = !ink(image.getRGB(x, y));
            }
            if (empty && start < 0) {
                start = y;
            }
            if (!empty && start >= 0) {
                if (y - start > bestLength) {
                    bestLength = y - start;
                    bestStart = start;
                }
                start = -1;
            }
        }
        if (bestStart < 0) {
            throw new IllegalStateException("Could not find the gap above the name");
        }
        return bestStart + bestLength / 2;
    }

    /** Left, top, right, bottom of everything that is not white between two rows. */
    private static int[] box(BufferedImage image, int fromY, int toY) {
        int minX = image.getWidth();
        int minY = toY;
        int maxX = 0;
        int maxY = fromY;
        for (int y = fromY; y < toY; y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (ink(image.getRGB(x, y))) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        return new int[] {minX, minY, maxX, maxY};
    }

    private static BufferedImage crop(BufferedImage image, int[] box, int margin) {
        int x = Math.max(0, box[0] - margin);
        int y = Math.max(0, box[1] - margin);
        int w = Math.min(image.getWidth() - x, box[2] - box[0] + 1 + 2 * margin);
        int h = Math.min(image.getHeight() - y, box[3] - box[1] + 1 + 2 * margin);
        BufferedImage copy = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(image.getSubimage(x, y, w, h), 0, 0, null);
        g.dispose();
        return copy;
    }

    private static BufferedImage scaleToWidth(BufferedImage image, int width) {
        int height = Math.round(image.getHeight() * (float) width / image.getWidth());
        return scale(image, width, height, BufferedImage.TYPE_INT_RGB, Color.WHITE);
    }

    /** A square white rounded tile with the mark centred on it. */
    private static BufferedImage icon(BufferedImage mark, int size) {
        int big = size * 4;
        BufferedImage tile = new BufferedImage(big, big, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = tile.createGraphics();
        hint(g);
        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Float(0, 0, big, big, big * 0.22f, big * 0.22f));
        int inner = Math.round(big * 0.84f);
        int h = Math.round(mark.getHeight() * (float) inner / mark.getWidth());
        g.drawImage(scale(mark, inner, h, BufferedImage.TYPE_INT_RGB, Color.WHITE), (big - inner) / 2,
                (big - h) / 2, null);
        g.dispose();
        return scale(tile, size, size, BufferedImage.TYPE_INT_ARGB, null);
    }

    /** Halves the picture step by step so small sizes stay sharp. */
    private static BufferedImage scale(BufferedImage source, int width, int height, int type, Color background) {
        BufferedImage current = source;
        while (current.getWidth() / 2 >= width && current.getHeight() / 2 >= height) {
            current = step(current, current.getWidth() / 2, current.getHeight() / 2, type, background);
        }
        return step(current, width, height, type, background);
    }

    private static BufferedImage step(BufferedImage source, int width, int height, int type, Color background) {
        BufferedImage target = new BufferedImage(width, height, type);
        Graphics2D g = target.createGraphics();
        hint(g);
        if (background != null) {
            g.setColor(background);
            g.fillRect(0, 0, width, height);
        }
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return target;
    }

    private static void hint(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private static void write(BufferedImage image, File file) throws IOException {
        ImageIO.write(image, "png", file);
    }
}
