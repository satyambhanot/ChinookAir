package ca.ucalgary.chinookair.presentation;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A strip with an icon and one or two sentences, tinted for what it says: a problem, good news or a
 * hint. The icon and the words say it too, so the colour is never the only sign. It is hidden while
 * it has nothing to say.
 */
class Alert extends JPanel {

    private static final long serialVersionUID = 1L;

    /** What the strip is about. */
    enum Kind {
        ERROR(Palette.DANGER_TEXT, Palette.DANGER_TINT, "alert"),
        SUCCESS(Palette.SUCCESS_TEXT, Palette.SUCCESS_TINT, "check"),
        INFO(Palette.NAVY_900, Palette.SKY_100, "alert");

        private final Color text;
        private final Color fill;
        private final String icon;

        Kind(Color text, Color fill, String icon) {
            this.text = text;
            this.fill = fill;
            this.icon = icon;
        }
    }

    private final int textWidth;
    private final JLabel icon = new JLabel();
    private final JLabel words = new JLabel();
    private Kind kind = Kind.ERROR;
    private String text = "";

    /** @param textWidth the width in pixels where long text wraps to the next line */
    Alert(int textWidth) {
        super(new BorderLayout(10, 0));
        this.textWidth = textWidth;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(Space.M, Space.M, Space.M, Space.M));
        add(icon, BorderLayout.WEST);
        add(words, BorderLayout.CENTER);
        icon.setVerticalAlignment(JLabel.TOP);
        words.setFont(words.getFont().deriveFont(Type.LABEL));
        setVisible(false);
    }

    void show(Kind newKind, String message) {
        if (message == null || message.isBlank()) {
            clear();
            return;
        }
        this.kind = newKind;
        this.text = message;
        icon.setIcon(Icons.of(newKind.icon, 18, newKind.text));
        words.setForeground(newKind.text);
        words.setText("<html><div style='width:" + textWidth + "px'>" + escape(message) + "</div></html>");
        getAccessibleContext().setAccessibleDescription(message);
        setVisible(true);
        revalidate();
        repaint();
    }

    void clear() {
        text = "";
        words.setText("");
        setVisible(false);
        revalidate();
    }

    /** The sentence shown, without markup; empty when the strip is hidden. */
    String text() {
        return text;
    }

    Kind kind() {
        return kind;
    }

    private static String escape(String message) {
        return message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(kind.fill);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
        g2.dispose();
        super.paintComponent(g);
    }
}
