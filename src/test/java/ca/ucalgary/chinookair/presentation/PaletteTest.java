package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Keeps the colours readable and in one place. */
class PaletteTest {

    private static final double TEXT = 4.5;
    private static final double LARGE_OR_LINE = 3.0;

    /** The contrast ratio of two colours, as the accessibility guidelines (WCAG 2.2) work it out. */
    static double contrast(Color a, Color b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminance(Color c) {
        return 0.2126 * channel(c.getRed()) + 0.7152 * channel(c.getGreen()) + 0.0722 * channel(c.getBlue());
    }

    private static double channel(int value) {
        double s = value / 255.0;
        return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }

    private static void atLeast(double limit, Color text, Color background, String what) {
        double ratio = contrast(text, background);
        assertTrue(ratio >= limit, what + " has contrast " + String.format("%.2f", ratio) + ", needs " + limit);
    }

    @Test
    void bodyTextIsReadableOnEverySurface() {
        for (Color surface : List.of(Palette.SURFACE, Palette.PAGE, Palette.SKY_100, Palette.TABLE_HEADER)) {
            atLeast(TEXT, Palette.TEXT, surface, "text on " + Integer.toHexString(surface.getRGB()));
        }
        for (Color surface : List.of(Palette.SURFACE, Palette.PAGE, Palette.TABLE_HEADER, Palette.TAKEN)) {
            atLeast(TEXT, Palette.TEXT_MUTED, surface, "muted text on " + Integer.toHexString(surface.getRGB()));
        }
    }

    @Test
    void linksAndButtonsAreReadable() {
        atLeast(TEXT, Palette.BLUE_700, Palette.SURFACE, "link on white");
        atLeast(TEXT, Palette.BLUE_700, Palette.PAGE, "link on the page");
        atLeast(TEXT, Color.WHITE, Palette.BLUE_700, "button text");
        atLeast(TEXT, Color.WHITE, Palette.BLUE_800, "button text when hovered");
        atLeast(TEXT, Color.WHITE, Palette.NAVY_900, "text on navy");
    }

    @Test
    void theNavyBarIsReadable() {
        atLeast(TEXT, Color.WHITE, Palette.NAVY_900, "selected tab and name");
        atLeast(TEXT, Palette.BORDER, Palette.NAVY_900, "tab and role text");
        atLeast(TEXT, Color.WHITE, Palette.BLUE_800, "bar button when hovered");
        atLeast(TEXT, Palette.BORDER, Palette.BLUE_800, "tab when hovered");
        atLeast(TEXT, Color.WHITE, Palette.BLUE_900, "bar button when pressed");
        atLeast(LARGE_OR_LINE, Palette.SKY_500, Palette.NAVY_900, "underline and focus ring on navy");
        atLeast(LARGE_OR_LINE, Palette.BORDER_STRONG, Palette.NAVY_900, "outline of the Log out button");
    }

    @Test
    void goldAndSkyAreOnlyUsedBehindDarkText() {
        atLeast(TEXT, Palette.NAVY_900, Palette.GOLD_500, "navy on gold");
        atLeast(TEXT, Palette.NAVY_900, Palette.GOLD_100, "navy on pale gold");
        atLeast(TEXT, Palette.NAVY_900, Palette.SKY_100, "navy on pale sky");
        // and the reason they are never text: this is what they would look like on white
        assertTrue(contrast(Palette.GOLD_500, Palette.SURFACE) < LARGE_OR_LINE);
        assertTrue(contrast(Palette.SKY_500, Palette.SURFACE) < LARGE_OR_LINE);
    }

    @Test
    void feedbackColoursAreReadableOnTheirTintAndOnWhite() {
        atLeast(TEXT, Palette.SUCCESS_TEXT, Palette.SUCCESS_TINT, "success badge");
        atLeast(TEXT, Palette.WARNING_TEXT, Palette.WARNING_TINT, "warning badge");
        atLeast(TEXT, Palette.DANGER_TEXT, Palette.DANGER_TINT, "error strip");
        atLeast(TEXT, Palette.SUCCESS_TEXT, Palette.SURFACE, "success text");
        atLeast(TEXT, Palette.DANGER_TEXT, Palette.SURFACE, "error text");
        atLeast(TEXT, Palette.DANGER_TEXT, Palette.PAGE, "error text on the page");
    }

    @Test
    void theRedButtonOfAConfirmationIsReadable() {
        atLeast(TEXT, Color.WHITE, Palette.DANGER_TEXT, "red confirm button");
        atLeast(TEXT, Color.WHITE, Palette.DANGER_DARK, "red confirm button when hovered");
    }

    @Test
    void theFocusRingAndDisabledControlsStayVisible() {
        atLeast(LARGE_OR_LINE, Palette.BLUE_600, Palette.PAGE, "focus ring on the page");
        atLeast(LARGE_OR_LINE, Palette.BLUE_600, Palette.SURFACE, "focus ring on white");
        atLeast(LARGE_OR_LINE, Palette.DISABLED_TEXT, Palette.DISABLED_FILL, "disabled button text");
    }

    @Test
    void theThemeFileOnlyUsesPaletteColours() throws IOException {
        Set<String> known = new HashSet<>();
        for (Field field : Palette.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == Color.class) {
                try {
                    field.setAccessible(true);
                    known.add(String.format("#%06X", ((Color) field.get(null)).getRGB() & 0xFFFFFF));
                } catch (IllegalAccessException e) {
                    throw new AssertionError(e);
                }
            }
        }
        known.add("#FFFFFF");
        String file;
        try (InputStream in = Palette.class.getResourceAsStream("FlatLaf.properties")) {
            file = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = Pattern.compile("#[0-9A-Fa-f]{6}\\b").matcher(file);
        List<String> unknown = new ArrayList<>();
        while (m.find()) {
            if (!known.contains(m.group().toUpperCase())) {
                unknown.add(m.group());
            }
        }
        assertEquals(List.of(), unknown, "colours in FlatLaf.properties that are not in Palette");
    }

    @Test
    void noViewMakesUpItsOwnColourNumber() throws IOException {
        Pattern literal = Pattern.compile("0x[0-9A-Fa-f]{6}\\b|\"[^\"\\n]*#[0-9A-Fa-f]{6}");
        List<String> found = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java/ca/ucalgary/chinookair"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (file.getFileName().toString().equals("Palette.java")) {
                    continue;
                }
                Matcher m = literal.matcher(Files.readString(file));
                if (m.find()) {
                    found.add(file.getFileName() + ": " + m.group());
                }
            }
        }
        assertEquals(List.of(), found, "colour numbers outside Palette");
    }
}
