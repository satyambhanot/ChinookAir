package ca.ucalgary.chinookair.presentation;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

/** A white card with a title and rows of "label, value", and a total in bold at the bottom. */
class SummaryCard extends JPanel {

    private static final long serialVersionUID = 1L;

    /** One line of the card, kept so tests and screens can read it back. */
    record Row(String label, String value, boolean total) {
    }

    private final List<Row> rows = new ArrayList<>();
    private final GridBagConstraints at = new GridBagConstraints();
    private int next = 1;

    SummaryCard(String title) {
        super(new GridBagLayout());
        setBackground(Theme.SURFACE);
        setBorder(BorderFactory.createCompoundBorder(new LineBorder(Theme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)));
        JLabel heading = new JLabel(title);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, Type.SECTION));
        heading.setForeground(Theme.TEXT);
        at.gridx = 0;
        at.gridy = 0;
        at.gridwidth = 2;
        at.anchor = GridBagConstraints.WEST;
        at.insets = new Insets(0, 0, 12, 0);
        add(heading, at);
    }

    /** Adds a line like "Seat" and "5C". */
    SummaryCard addRow(String label, String value) {
        return add(new Row(label, value, false));
    }

    /** Adds the last line, in bold, like "Total" and "$184.00". */
    SummaryCard addTotal(String label, String value) {
        return add(new Row(label, value, true));
    }

    private SummaryCard add(Row row) {
        rows.add(row);
        JLabel name = new JLabel(row.label());
        JLabel value = new JLabel(row.value(), SwingConstants.RIGHT);
        // a few pixels of room, so the last letter is never cut off
        value.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
        name.setForeground(row.total() ? Theme.TEXT : Theme.MUTED);
        value.setForeground(Theme.TEXT);
        if (row.total()) {
            name.setFont(name.getFont().deriveFont(Font.BOLD));
            value.setFont(value.getFont().deriveFont(Font.BOLD, Type.SECTION));
        }
        at.gridy = next++;
        at.gridwidth = 1;
        at.insets = new Insets(row.total() ? 12 : 3, 0, 3, 16);
        at.gridx = 0;
        at.weightx = 0;
        add(name, at);
        at.gridx = 1;
        at.weightx = 1;
        at.anchor = GridBagConstraints.EAST;
        at.insets = new Insets(row.total() ? 12 : 3, 0, 3, 0);
        add(value, at);
        at.anchor = GridBagConstraints.WEST;
        return this;
    }

    List<Row> rows() {
        return List.copyOf(rows);
    }
}
