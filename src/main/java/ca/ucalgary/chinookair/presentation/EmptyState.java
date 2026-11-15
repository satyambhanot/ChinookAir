package ca.ucalgary.chinookair.presentation;

import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * What a screen shows when there is nothing to list yet: the faded mountain from the logo, one line
 * that says what is missing, one line that says what to do, and optionally the button that does it.
 */
class EmptyState extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int PICTURE_WIDTH = 180;

    private final JLabel title;
    private final JLabel hint;
    private JButton action;
    private Component actionGap;

    EmptyState(String titleText, String hintText) {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(Theme.PAGE, Theme.PAGE, Theme.PAGE, Theme.PAGE));

        JLabel picture = new JLabel(new FadedIcon(Brand.scaledToWidth(Brand.mark(), PICTURE_WIDTH)));
        title = new JLabel(titleText);
        title.setFont(title.getFont().deriveFont(Font.BOLD, Type.SECTION));
        title.setForeground(Theme.TEXT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        hint = Theme.hint(hintText);
        for (JComponent part : new JComponent[] {picture, title, hint}) {
            part.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        add(picture);
        add(Box.createVerticalStrut(Theme.PAD));
        add(title);
        add(Box.createVerticalStrut(4));
        add(hint);
    }

    /** Adds the button that fixes the emptiness, or removes it when the label is null. */
    void setAction(String label, Runnable run) {
        if (action != null) {
            remove(action);
            remove(actionGap);
            action = null;
            actionGap = null;
        }
        if (label != null) {
            action = Theme.primary(new JButton(label));
            action.setAlignmentX(Component.CENTER_ALIGNMENT);
            action.addActionListener(e -> run.run());
            actionGap = Box.createVerticalStrut(Theme.PAD);
            add(actionGap);
            add(action);
        }
        revalidate();
        repaint();
    }

    String title() {
        return title.getText();
    }

    String hint() {
        return hint.getText();
    }

    JButton action() {
        return action;
    }

    /** The picture drawn half transparent, so it decorates without competing with the words. */
    private static final class FadedIcon implements Icon {

        private final Image image;

        FadedIcon(Image image) {
            this.image = image;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
            g2.drawImage(image, x, y, null);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return image.getWidth(null);
        }

        @Override
        public int getIconHeight() {
            return image.getHeight(null);
        }
    }
}
