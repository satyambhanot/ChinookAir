package ca.ucalgary.chinookair.presentation;

import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Taskbar;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * The ChinookAir logo files, read from the program's resources once. The files are made from the
 * original picture by scripts/BrandAssets.java.
 */
public final class Brand {

    /** The sizes of the window icon files. */
    static final int[] ICON_SIZES = {16, 32, 64, 128, 256, 512};

    private static final String FOLDER = "/branding/";

    private static BufferedImage fullLogo;
    private static BufferedImage mark;
    private static List<Image> icons;

    private Brand() {
    }

    /** The mountain, the plane and the name together. */
    static synchronized BufferedImage fullLogo() {
        if (fullLogo == null) {
            fullLogo = read("logo-full.png");
        }
        return fullLogo;
    }

    /** The mountain and the plane without the name. */
    static synchronized BufferedImage mark() {
        if (mark == null) {
            mark = read("mark.png");
        }
        return mark;
    }

    /** The window and Dock icon in every size, so the system can pick the sharpest one. */
    public static synchronized List<Image> icons() {
        if (icons == null) {
            List<Image> list = new ArrayList<>();
            for (int size : ICON_SIZES) {
                list.add(read("icon-" + size + ".png"));
            }
            icons = List.copyOf(list);
        }
        return icons;
    }

    /** Puts the big icon in the Dock or task bar, where the system allows it. */
    public static void installTaskbarIcon() {
        if (GraphicsEnvironment.isHeadless() || !Taskbar.isTaskbarSupported()) {
            return;
        }
        Taskbar taskbar = Taskbar.getTaskbar();
        if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
            try {
                taskbar.setIconImage(icons().get(icons().size() - 1));
            } catch (UnsupportedOperationException | SecurityException e) {
                // the window icon is still set, so the program works without this one
            }
        }
    }

    /** A copy of the picture at this width, with the height following the picture's shape. */
    static Image scaledToWidth(BufferedImage image, int width) {
        int height = Math.round(image.getHeight() * (float) width / image.getWidth());
        return image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
    }

    private static BufferedImage read(String name) {
        try (InputStream in = Brand.class.getResourceAsStream(FOLDER + name)) {
            if (in == null) {
                throw new IllegalStateException("Missing logo file " + name);
            }
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                throw new IllegalStateException("Cannot read logo file " + name);
            }
            return image;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read logo file " + name, e);
        }
    }
}
