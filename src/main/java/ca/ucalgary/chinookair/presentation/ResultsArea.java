package ca.ucalgary.chinookair.presentation;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * A list of cards in a scroll pane that is only as tall as its cards, with room for one card at least
 * so the empty text has somewhere to sit, and a footer, like a row of buttons, right under it. When
 * there are more cards than the window has room for, it fills the room and scrolls.
 */
final class ResultsArea extends JPanel {

    private static final long serialVersionUID = 1L;
    static final int MIN_CELLS = 1;

    private final JList<?> list;
    private final JScrollPane scroll;
    private final JComponent footer;

    ResultsArea(JList<?> list, JComponent footer) {
        super(null);
        this.list = list;
        this.footer = footer;
        this.scroll = new JScrollPane(list);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(FlightResultList.CELL_HEIGHT / 2);
        setOpaque(false);
        setLayout(new Fitting());
        add(scroll);
        add(footer);
        list.getModel().addListDataListener(new javax.swing.event.ListDataListener() {
            @Override
            public void intervalAdded(javax.swing.event.ListDataEvent e) {
                revalidate();
            }

            @Override
            public void intervalRemoved(javax.swing.event.ListDataEvent e) {
                revalidate();
            }

            @Override
            public void contentsChanged(javax.swing.event.ListDataEvent e) {
                revalidate();
            }
        });
    }

    JScrollPane scrollPane() {
        return scroll;
    }

    /** The height the cards want, in pixels, for the rows there are now. */
    int cardsHeight() {
        int cells = Math.max(MIN_CELLS, list.getModel().getSize());
        return cells * list.getFixedCellHeight();
    }

    private int footerHeight() {
        return Space.M + footer.getPreferredSize().height;
    }

    private final class Fitting implements LayoutManager {

        @Override
        public void addLayoutComponent(String name, Component comp) {
            // the children are found through the container
        }

        @Override
        public void removeLayoutComponent(Component comp) {
            // nothing is kept
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return new Dimension(0, cardsHeight() + footerHeight());
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return new Dimension(0, 0);
        }

        @Override
        public void layoutContainer(Container parent) {
            int height = Math.min(parent.getHeight() - footerHeight(), cardsHeight());
            scroll.setBounds(0, 0, parent.getWidth(), height);
            footer.setBounds(0, height + Space.M, parent.getWidth(), footer.getPreferredSize().height);
        }
    }
}
