package ca.ucalgary.chinookair.presentation;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;
import java.awt.Color;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.UIManager;

/**
 * The look of the whole program in one place: colours, sizes and a few helpers that build the same
 * heading, button or hint on every screen. Screens use these instead of picking their own numbers.
 */
public final class Theme {

    // colours, named for what they do; the numbers are in Palette
    static final Color ACCENT = Palette.BLUE_700;
    static final Color ACCENT_DARK = Palette.BLUE_800;
    static final Color ACCENT_SOFT = Palette.SKY_100;
    static final Color BACKGROUND = Palette.PAGE;
    static final Color SURFACE = Palette.SURFACE;
    static final Color BORDER = Palette.BORDER;
    static final Color TEXT = Palette.TEXT;
    static final Color MUTED = Palette.TEXT_MUTED;
    static final Color SUCCESS = Palette.SUCCESS_TEXT;
    static final Color DANGER = Palette.DANGER_TEXT;
    static final Color WARNING = Palette.WARNING_TEXT;

    // spacing, in pixels
    static final int GAP = 8;
    static final int PAD = 16;
    static final int PAGE = 32;
    static final int ARC = 12;

    private Theme() {
    }

    /**
     * Switches the program to the flat look with the Inter font. Call it before the first window is
     * made. The colours and shapes are in FlatLaf.properties next to this class.
     */
    public static void install() {
        FlatInterFont.install();
        FlatLaf.setPreferredFontFamily(FlatInterFont.FAMILY);
        FlatLaf.setPreferredLightFontFamily(FlatInterFont.FAMILY_LIGHT);
        FlatLaf.setPreferredSemiboldFontFamily(FlatInterFont.FAMILY_SEMIBOLD);
        FlatLaf.registerCustomDefaultsSource("ca.ucalgary.chinookair.presentation");
        FlatLightLaf.setup();
        UIManager.put("defaultFont", UIManager.getFont("defaultFont").deriveFont(Type.BODY));
    }

    /** A page title, the biggest text on a screen. */
    static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, Type.PAGE_HEADING));
        label.setForeground(TEXT);
        return label;
    }

    /** A line of smaller grey text under a title or beside a field. */
    static JLabel hint(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Type.LABEL));
        label.setForeground(MUTED);
        return label;
    }

    /** A white panel with a thin rounded border, for the login and registration forms. */
    static javax.swing.JPanel card() {
        javax.swing.JPanel card = new javax.swing.JPanel(new java.awt.GridBagLayout());
        card.setBackground(SURFACE);
        SurfaceBorder.apply(card, Space.XXL, Space.XXXL, Space.XXL, Space.XXXL);
        return card;
    }

    /** Grey example text inside an empty field, like the shape of a date. */
    static <T extends javax.swing.text.JTextComponent> T placeholder(T field, String text) {
        field.putClientProperty("JTextField.placeholderText", text);
        return field;
    }

    /** A page title for a screen's own heading label, which is already made. */
    static void heading(JLabel label) {
        label.setFont(label.getFont().deriveFont(Font.BOLD, Type.PAGE_HEADING));
        label.setForeground(TEXT);
    }

    /** The one main action of a screen: filled with the accent colour. */
    static JButton primary(JButton button) {
        button.putClientProperty("FlatLaf.styleClass", "primary");
        return button;
    }

    /** An action that removes something: red text, so it does not look like the safe choice. */
    static JButton danger(JButton button) {
        button.putClientProperty("FlatLaf.styleClass", "danger");
        return button;
    }

    /** The button that really does the destructive thing, in the confirmation dialog: filled red. */
    static JButton dangerFilled(JButton button) {
        button.putClientProperty("FlatLaf.styleClass", "dangerFilled");
        return button;
    }

    /** An ordinary button with the same roomy padding as the others. */
    static JButton secondary(JButton button) {
        button.putClientProperty("FlatLaf.styleClass", "secondary");
        return button;
    }

    /** A button that looks like a link, for small actions like "Register a new account". */
    static JButton link(JButton button) {
        button.putClientProperty("JButton.buttonType", "borderless");
        button.putClientProperty("FlatLaf.styleClass", "link");
        return button;
    }
}
