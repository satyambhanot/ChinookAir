package ca.ucalgary.chinookair.presentation;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A row of inputs above a table or a card: each input has its label above it, and buttons and
 * search boxes sit on the same line as the inputs, centred on it. Labels start where the drawn edge
 * of their input starts, so a label and its field line up.
 */
final class FilterBar extends JPanel {

    private static final long serialVersionUID = 1L;

    private final List<JComponent> controls = new ArrayList<>();
    private int column;

    FilterBar() {
        super(new GridBagLayout());
        setOpaque(false);
    }

    /** An input with its label above it. The mnemonic is the key that jumps to the input. */
    FilterBar field(String text, JComponent control, char mnemonic) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD, Type.LABEL));
        label.setForeground(Theme.TEXT);
        label.setLabelFor(control);
        if (mnemonic != 0) {
            label.setDisplayedMnemonic(mnemonic);
        }
        control.getAccessibleContext().setAccessibleName(text);
        GridBagConstraints at = constraints(0);
        at.anchor = GridBagConstraints.WEST;
        at.insets = new Insets(0, Space.fieldEdge(), Space.XS, Space.M);
        add(label, at);
        return control(control, false);
    }

    /** A button or link on the line of the inputs, without a label of its own. */
    FilterBar action(JComponent control) {
        return control(control, false);
    }

    /** A box that takes the rest of the width, like a search box beside the inputs. */
    FilterBar fill(JComponent control) {
        return control(control, true);
    }

    private FilterBar control(JComponent control, boolean fill) {
        GridBagConstraints at = constraints(1);
        at.anchor = GridBagConstraints.WEST;
        at.insets = new Insets(0, 0, 0, Space.M);
        if (fill) {
            at.fill = GridBagConstraints.HORIZONTAL;
            at.weightx = 1;
        }
        add(control, at);
        controls.add(control);
        column++;
        return this;
    }

    private GridBagConstraints constraints(int row) {
        GridBagConstraints at = new GridBagConstraints();
        at.gridx = column;
        at.gridy = row;
        return at;
    }

    /** The inputs and buttons, left to right, for tests. */
    List<JComponent> controls() {
        return List.copyOf(controls);
    }
}
