package ca.ucalgary.chinookair.presentation;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Holds one screen and keeps it a readable width: the screen fills a small window, but in a wide
 * window it stays centred at {@link #MAX_WIDTH} instead of stretching its tables across the monitor.
 */
final class ContentPane extends JPanel {

    private static final long serialVersionUID = 1L;

    /** The widest a screen gets: 1200 pixels of content and the page padding on both sides. */
    static final int MAX_WIDTH = 1200 + 2 * Space.PAGE;

    ContentPane(JComponent screen) {
        super(new Centred());
        setOpaque(false);
        add(screen);
    }

    /** Gives the one child the full height and a centred, capped width. */
    private static final class Centred implements LayoutManager {

        @Override
        public void addLayoutComponent(String name, Component comp) {
            // the child is found through the container
        }

        @Override
        public void removeLayoutComponent(Component comp) {
            // nothing is kept
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return parent.getComponentCount() == 0 ? new Dimension() : parent.getComponent(0).getPreferredSize();
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return parent.getComponentCount() == 0 ? new Dimension() : parent.getComponent(0).getMinimumSize();
        }

        @Override
        public void layoutContainer(Container parent) {
            if (parent.getComponentCount() == 0) {
                return;
            }
            Insets in = parent.getInsets();
            int available = parent.getWidth() - in.left - in.right;
            int width = Math.min(available, MAX_WIDTH);
            parent.getComponent(0).setBounds(in.left + (available - width) / 2, in.top, width,
                    parent.getHeight() - in.top - in.bottom);
        }
    }
}
