package ca.ucalgary.chinookair.presentation;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;

/**
 * Holds one screen and keeps it a readable width: the screen fills a small window, but in a wide
 * window it stays centred at {@link #MAX_WIDTH} instead of stretching its tables across the monitor.
 */
final class ContentPane extends JPanel implements Scrollable {

    private static final long serialVersionUID = 1L;

    /** The widest a screen gets: 1200 pixels of content and the page padding on both sides. */
    static final int MAX_WIDTH = 1200 + 2 * Space.PAGE;

    ContentPane(JComponent screen) {
        super(new Centred());
        setOpaque(false);
        add(screen);
    }

    /** The least height the screen is useful in, or 0 when it shrinks to any height. */
    private int minimumHeight() {
        if (getComponentCount() > 0 && getComponent(0) instanceof Screen) {
            return ((Screen) getComponent(0)).minimumHeight();
        }
        return 0;
    }

    /** As tall as the screen needs at the least, so a low window scrolls this pane and nothing is squeezed. */
    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(size.width, minimumHeight());
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return visible.height;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    /** The screen fills the window while the window is tall enough for it; below that the page scrolls. */
    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getParent() instanceof JViewport && getParent().getHeight() >= minimumHeight();
    }

    /** A scroll pane round a screen that shows no frame and no bar until the window is too low. */
    static javax.swing.JScrollPane scrolling(ContentPane pane) {
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(pane);
        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
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
