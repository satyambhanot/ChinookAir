package ca.ucalgary.chinookair.presentation;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A label above an input and a line under it. The line holds a hint, or the error for this field in
 * red; an error also gives the input a red outline, so it is easy to see which field to fix.
 */
class FormField extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel label;
    private final JLabel line = new JLabel(" ");
    private final JComponent field;
    private String helper = "";
    private String error = "";

    FormField(String text, JComponent field) {
        super(new BorderLayout(0, 4));
        this.field = field;
        this.label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD, Type.LABEL));
        label.setForeground(Theme.TEXT);
        label.setLabelFor(field);
        // the drawn edge of the input starts a little in from its bounds; the label starts there too
        label.setBorder(BorderFactory.createEmptyBorder(0, Space.fieldEdge(), 0, 0));
        line.setFont(line.getFont().deriveFont(Type.CAPTION));
        line.setBorder(BorderFactory.createEmptyBorder(0, Space.fieldEdge(), 0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 0, Space.S, 0));
        add(label, BorderLayout.NORTH);
        add(field, BorderLayout.CENTER);
        add(line, BorderLayout.SOUTH);
        field.getAccessibleContext().setAccessibleName(text);
        refresh();
    }

    /** The key that jumps to this field, shown as an underlined letter in the label. */
    FormField mnemonic(char key) {
        label.setDisplayedMnemonic(key);
        return this;
    }

    /** A hint under the input, like "At least 8 characters". It shows while there is no error. */
    FormField helper(String text) {
        this.helper = text == null ? "" : text;
        refresh();
        return this;
    }

    /** Marks this field as wrong and says why. */
    void showError(String message) {
        this.error = message == null ? "" : message;
        refresh();
    }

    void clearError() {
        showError("");
    }

    private void refresh() {
        boolean wrong = !error.isEmpty();
        field.putClientProperty("JComponent.outline", wrong ? "error" : null);
        String shown = wrong ? error : helper;
        line.setText(shown.isEmpty() ? " " : shown);
        line.setForeground(wrong ? Theme.DANGER : Theme.MUTED);
        field.getAccessibleContext().setAccessibleDescription(shown);
    }

    /** A vertical layout must not stretch the field taller than it needs to be. */
    @Override
    public java.awt.Dimension getMaximumSize() {
        return new java.awt.Dimension(super.getMaximumSize().width, getPreferredSize().height);
    }

    JComponent field() {
        return field;
    }

    String labelText() {
        return label.getText();
    }

    /** The text of the line under the input: the error if there is one, else the hint. */
    String lineText() {
        return line.getText().trim();
    }

    boolean hasError() {
        return !error.isEmpty();
    }
}
