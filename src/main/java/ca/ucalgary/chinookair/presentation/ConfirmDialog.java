package ca.ucalgary.chinookair.presentation;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * A question with two buttons, for things that are hard to undo. A dangerous action gets a red button,
 * and Cancel is the button that Enter presses, so a quick keystroke never destroys anything. Escape
 * always means no.
 */
final class ConfirmDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final int TEXT_WIDTH = 340;

    private final JButton cancelButton = Theme.secondary(new JButton("Cancel"));
    private final JButton confirmButton;
    private boolean confirmed;

    ConfirmDialog(Component parent, String title, String message, String confirmLabel, boolean danger) {
        super(SwingUtilities.getWindowAncestor(parent), "ChinookAir", ModalityType.APPLICATION_MODAL);
        JButton confirm = new JButton(confirmLabel);
        confirmButton = danger ? Theme.dangerFilled(confirm) : Theme.primary(confirm);

        JLabel heading = new JLabel(title);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, ca.ucalgary.chinookair.presentation.Type.SECTION));
        heading.setForeground(Theme.TEXT);
        JLabel words = new JLabel("<html><div style='width:" + TEXT_WIDTH + "px'>" + escape(message)
                + "</div></html>");
        words.setForeground(Theme.MUTED);
        JPanel text = new JPanel(new BorderLayout(0, 8));
        text.setOpaque(false);
        text.add(heading, BorderLayout.NORTH);
        text.add(words, BorderLayout.CENTER);

        ButtonRow buttons = ButtonRow.right();
        buttons.setOpaque(false);
        buttons.add(cancelButton);
        buttons.add(confirmButton);
        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setBackground(Theme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(Space.XL, Space.XL, Space.XL, Space.XL));
        content.add(text, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);

        cancelButton.addActionListener(e -> close(false));
        confirmButton.addActionListener(e -> close(true));
        // Escape means no; Enter presses the safe button when the action is dangerous
        getRootPane().registerKeyboardAction(e -> close(false), KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().setDefaultButton(danger ? cancelButton : confirmButton);
        setResizable(false);
        pack();
        setLocationRelativeTo(parent);
    }

    private void close(boolean answer) {
        confirmed = answer;
        setVisible(false);
        dispose();
    }

    private static String escape(String message) {
        return message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Shows the question and waits for the answer. */
    static boolean ask(Component parent, String title, String message, String confirmLabel, boolean danger) {
        ConfirmDialog dialog = new ConfirmDialog(parent, title, message, confirmLabel, danger);
        dialog.setVisible(true);
        return dialog.confirmed;
    }

    // ---- for the tests

    JButton cancelButton() {
        return cancelButton;
    }

    JButton confirmButton() {
        return confirmButton;
    }

    boolean confirmed() {
        return confirmed;
    }
}
