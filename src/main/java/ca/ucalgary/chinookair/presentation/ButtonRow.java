package ca.ucalgary.chinookair.presentation;

import java.awt.Component;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

/**
 * A row of buttons that starts or ends exactly at the edge of the column above it, with the same gap
 * between buttons everywhere. A flow layout keeps a margin at both ends, so the last button would
 * stop short of the fields above it.
 */
final class ButtonRow extends JPanel {

    private static final long serialVersionUID = 1L;

    private final boolean right;
    private final transient Component glue = Box.createHorizontalGlue();
    private int buttons;

    private ButtonRow(boolean right) {
        super();
        this.right = right;
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false);
        super.add(glue);
    }

    /** Buttons pushed to the right edge, in the order they are added from left to right. */
    static ButtonRow right() {
        return new ButtonRow(true);
    }

    /** Buttons that start at the left edge. */
    static ButtonRow left() {
        return new ButtonRow(false);
    }

    @Override
    public Component add(Component component) {
        if (!right) {
            remove(glue);
        }
        if (buttons > 0) {
            super.add(Box.createHorizontalStrut(Space.S));
        }
        buttons++;
        Component added = super.add(component);
        if (!right) {
            super.add(glue);
        }
        return added;
    }
}
