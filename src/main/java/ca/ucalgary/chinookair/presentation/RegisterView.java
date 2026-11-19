package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Customer;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Creates a customer account. The checks and the duplicate messages come from the CustomerController;
 * a message about one field is shown under that field, any other message in a strip above the button.
 */
class RegisterView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final int FIELD_WIDTH = 320;
    private static final String PASSWORD_HINT = "At least 8 characters and one digit";

    private final transient Controllers controllers;
    private final transient Navigator navigator;
    private final transient BiConsumer<String, String> onRegistered;

    final JTextField usernameField = Theme.placeholder(new JTextField(20), "Choose a username");
    final JPasswordField passwordField = new JPasswordField(20);
    final JTextField nameField = Theme.placeholder(new JTextField(20), "First and last name");
    final JTextField emailField = Theme.placeholder(new JTextField(20), "name@example.com");
    final JTextField phoneField = CustomerForm.phoneBox(Theme.placeholder(new JTextField(20), "403-555-0100"));
    final JButton registerButton = Theme.primary(new JButton("Create account"));
    final JButton backButton = Theme.secondary(new JButton("Back to log in"));
    private final FormField usernameRow = new FormField("Username", usernameField).mnemonic('U');
    private final FormField passwordRow = new FormField("Password", passwordField).mnemonic('P')
            .helper(PASSWORD_HINT);
    private final FormField nameRow = new FormField("Full name", nameField).mnemonic('N');
    private final FormField emailRow = new FormField("Email", emailField).mnemonic('E');
    private final FormField phoneRow = new FormField("Phone (optional)", phoneField).mnemonic('H');
    private final Alert alert = new Alert(FIELD_WIDTH - 40);
    private String shown = "";

    /**
     * @param onRegistered told the new username and a friendly message once the account exists
     */
    RegisterView(Controllers controllers, Navigator navigator, BiConsumer<String, String> onRegistered) {
        super();
        this.controllers = controllers;
        this.navigator = navigator;
        this.onRegistered = onRegistered;
        build();
    }

    private void build() {
        JPanel card = Theme.card();
        card.removeAll();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel logo = new JLabel(new ImageIcon(Brand.scaledToWidth(Brand.mark(), 120)));
        logo.getAccessibleContext().setAccessibleName("ChinookAir");
        JLabel title = new JLabel("Create your account", SwingConstants.CENTER);
        Theme.heading(title);
        JLabel subtitle = Theme.hint("You can book and manage flights once you have logged in");
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);
        passwordField.putClientProperty("JPasswordField.showRevealButton", Boolean.TRUE);

        registerButton.setMnemonic('C');
        backButton.setMnemonic('B');
        registerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        backButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        for (JComponent part : new JComponent[] {logo, title, subtitle}) {
            part.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        card.add(logo);
        card.add(Box.createVerticalStrut(6));
        card.add(title);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(16));
        for (FormField row : new FormField[] {usernameRow, passwordRow, nameRow, emailRow, phoneRow}) {
            row.setPreferredSize(new Dimension(FIELD_WIDTH, row.getPreferredSize().height));
            row.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(row);
        }
        alert.setAlignmentX(Component.CENTER_ALIGNMENT);
        alert.setMaximumSize(new Dimension(Integer.MAX_VALUE, alert.getMaximumSize().height));
        registerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(alert);
        card.add(Box.createVerticalStrut(8));
        card.add(registerButton);
        card.add(Box.createVerticalStrut(8));
        card.add(backButton);
        card.setBorder(BorderFactory.createCompoundBorder(card.getBorder(), BorderFactory.createEmptyBorder()));
        // a short window scrolls the card instead of cutting off the buttons
        JPanel centred = new JPanel(new GridBagLayout());
        centred.setOpaque(false);
        centred.add(card, new GridBagConstraints());
        setLayout(new java.awt.BorderLayout());
        add(ScrollableForm.around(centred), java.awt.BorderLayout.CENTER);

        registerButton.addActionListener(e -> register());
        // Enter in the last field submits, like the default button
        phoneField.addActionListener(e -> register());
        backButton.addActionListener(e -> navigator.show(ViewId.LOGIN));
    }

    private void register() {
        clearErrors();
        char[] typed = passwordField.getPassword();
        try {
            Customer created = controllers.customerController().register(usernameField.getText(), new String(typed),
                    nameField.getText(), emailField.getText(), phoneField.getText());
            clearFields();
            onRegistered.accept(created.username(), "Account created. You can log in now.");
        } catch (RuntimeException e) {
            showError(ErrorMessages.forException(e));
            passwordField.setText("");
        } finally {
            Arrays.fill(typed, ' ');
        }
    }

    /** Puts the problem under the field it is about; anything else goes in the strip. */
    private void showError(String text) {
        shown = text;
        String lower = text.toLowerCase();
        if (lower.contains("username")) {
            usernameRow.showError(text);
        } else if (lower.contains("password")) {
            passwordRow.showError(text);
        } else if (lower.contains("email")) {
            emailRow.showError(text);
        } else if (lower.contains("phone")) {
            phoneRow.showError(text);
        } else if (lower.contains("name")) {
            nameRow.showError(text);
        } else {
            alert.show(Alert.Kind.ERROR, text);
        }
    }

    private void clearErrors() {
        shown = "";
        alert.clear();
        for (FormField row : new FormField[] {usernameRow, passwordRow, nameRow, emailRow, phoneRow}) {
            row.clearError();
        }
    }

    /** The last problem shown, wherever it appears on the screen. */
    String message() {
        return shown;
    }

    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        nameField.setText("");
        emailField.setText("");
        phoneField.setText("");
        clearErrors();
    }

    @Override
    public JButton defaultButton() {
        return registerButton;
    }

    @Override
    public JComponent initialFocus() {
        return usernameField;
    }

    @Override
    public void onShow() {
        clearFields();
        usernameField.requestFocusInWindow();
    }

    // ---- for the tests

    FormField usernameRow() {
        return usernameRow;
    }

    FormField passwordRow() {
        return passwordRow;
    }

    FormField emailRow() {
        return emailRow;
    }

    FormField nameRow() {
        return nameRow;
    }
}
