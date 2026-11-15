package ca.ucalgary.chinookair.presentation;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Font;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.UIManager;

/**
 * The look of the whole program in one place: colours, sizes and a few helpers that build the same
 * heading, button or hint on every screen. Screens use these instead of picking their own numbers.
 */
public final class Theme {

    // colours
    static final Color ACCENT = new Color(0x0B63CE);
    static final Color ACCENT_DARK = new Color(0x084FA6);
    static final Color ACCENT_SOFT = new Color(0xE8F1FC);
    static final Color BACKGROUND = new Color(0xF4F6F9);
    static final Color SURFACE = Color.WHITE;
    static final Color BORDER = new Color(0xDDE2E8);
    static final Color TEXT = new Color(0x1B1F24);
    static final Color MUTED = new Color(0x5B6675);
    static final Color SUCCESS = new Color(0x1A7F37);
    static final Color DANGER = new Color(0xC62828);
    static final Color WARNING = new Color(0x9A6700);

    // spacing, in pixels
    static final int GAP = 8;
    static final int PAD = 16;
    static final int PAGE = 28;
    static final int ARC = 10;

    private Theme() {
    }

    /** Switches the program to the flat look. Call it before the first window is made. */
    public static void install() {
        FlatLaf.setGlobalExtraDefaults(Map.of("@accentColor", "#0B63CE"));
        FlatLightLaf.setup();
        UIManager.put("defaultFont", UIManager.getFont("defaultFont").deriveFont(14f));
        UIManager.put("Component.arc", ARC);
        UIManager.put("Button.arc", ARC);
        UIManager.put("TextComponent.arc", ARC - 2);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Table.rowHeight", 32);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.intercellSpacing", new java.awt.Dimension(0, 1));
        UIManager.put("Table.gridColor", new Color(0xEEF1F4));
        UIManager.put("Table.alternateRowColor", new Color(0xF8FAFC));
        UIManager.put("Table.selectionBackground", ACCENT_SOFT);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("Table.cellMargins", new java.awt.Insets(4, 12, 4, 12));
        UIManager.put("TableHeader.cellMargins", new java.awt.Insets(4, 12, 4, 12));
        UIManager.put("TableHeader.height", 34);
        UIManager.put("TableHeader.background", new Color(0xEEF1F5));
        UIManager.put("TableHeader.bottomSeparatorColor", BORDER);
        UIManager.put("ScrollPane.border", javax.swing.BorderFactory.createLineBorder(BORDER));
    }

    /** A page title, the biggest text on a screen. */
    static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 24f));
        label.setForeground(TEXT);
        return label;
    }

    /** A line of smaller grey text under a title or beside a field. */
    static JLabel hint(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(13f));
        label.setForeground(MUTED);
        return label;
    }

    /** A page title for a screen's own heading label, which is already made. */
    static void heading(JLabel label) {
        label.setFont(label.getFont().deriveFont(Font.BOLD, 24f));
        label.setForeground(TEXT);
    }

    /** The one main action of a screen: filled with the accent colour. */
    static JButton primary(JButton button) {
        button.putClientProperty("FlatLaf.style",
                "background: #0B63CE; foreground: #FFFFFF; hoverBackground: #084FA6; "
                        + "pressedBackground: #073F85; borderWidth: 0; focusedBackground: #0B63CE; "
                        + "disabledBackground: #C9D3E0; disabledText: #FFFFFF; margin: 6,16,6,16");
        return button;
    }

    /** An action that removes something: red text, so it does not look like the safe choice. */
    static JButton danger(JButton button) {
        button.putClientProperty("FlatLaf.style",
                "foreground: #C62828; borderColor: #E3B5B5; hoverBorderColor: #C62828; margin: 6,16,6,16");
        return button;
    }

    /** An ordinary button with the same roomy padding as the others. */
    static JButton secondary(JButton button) {
        button.putClientProperty("FlatLaf.style", "margin: 6,16,6,16");
        return button;
    }

    /** A button that looks like a link, for small actions like "Register a new account". */
    static JButton link(JButton button) {
        button.putClientProperty("JButton.buttonType", "borderless");
        button.putClientProperty("FlatLaf.style", "foreground: #0B63CE");
        return button;
    }
}
