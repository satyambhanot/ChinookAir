package ca.ucalgary.chinookair.presentation;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;

/**
 * A panel for a form that sits in a scroll pane. It is as wide as the pane, so only the height ever
 * scrolls. A form on a small window then scrolls instead of squeezing its fields to nothing.
 */
final class ScrollableForm extends JPanel implements Scrollable {

    private static final long serialVersionUID = 1L;
    private static final int STEP = 16;

    ScrollableForm() {
        super(new BorderLayout());
        setOpaque(false);
    }

    /** A scroll pane around this content, with no border and no background of its own. */
    static JScrollPane around(JComponent content) {
        ScrollableForm form = new ScrollableForm();
        form.add(content, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(STEP);
        return scroll;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
        return STEP;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return visible.height;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
