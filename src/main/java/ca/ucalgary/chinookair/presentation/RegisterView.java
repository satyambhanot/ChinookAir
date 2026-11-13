package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Customer;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/** Creates a customer account. The checks and the duplicate messages come from the CustomerController. */
class RegisterView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;

    private final transient Controllers controllers;
    private final transient Navigator navigator;
    private final transient BiConsumer<String, String> onRegistered;

    final JTextField usernameField = new JTextField(20);
    final JPasswordField passwordField = new JPasswordField(20);
    final JTextField nameField = new JTextField(20);
    final JTextField emailField = new JTextField(20);
    final JTextField phoneField = new JTextField(20);
    final JButton registerButton = new JButton("Create account");
    final JButton backButton = new JButton("Back to log in");
    private final JLabel messageLabel = new JLabel(" ", SwingConstants.CENTER);

    /**
     * @param onRegistered told the new username and a friendly message once the account exists
     */
    RegisterView(Controllers controllers, Navigator navigator, BiConsumer<String, String> onRegistered) {
        super(new GridBagLayout());
        this.controllers = controllers;
        this.navigator = navigator;
        this.onRegistered = onRegistered;
        build();
    }

    private void build() {
        JLabel title = new JLabel("Create an account", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        messageLabel.setForeground(new Color(0xB00020));
        messageLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        add(title, c);
        c.gridwidth = 1;
        addRow(c, 1, "Username", 'U', usernameField);
        addRow(c, 2, "Password", 'P', passwordField);
        addRow(c, 3, "Full name", 'N', nameField);
        addRow(c, 4, "Email", 'E', emailField);
        addRow(c, 5, "Phone (optional)", 'H', phoneField);
        c.gridx = 0;
        c.gridy = 6;
        c.gridwidth = 2;
        add(new JLabel("The password needs at least 8 characters and one digit.", SwingConstants.CENTER), c);
        c.gridy = 7;
        add(messageLabel, c);
        c.gridy = 8;
        registerButton.setMnemonic('C');
        add(registerButton, c);
        c.gridy = 9;
        backButton.setMnemonic('B');
        add(backButton, c);

        registerButton.addActionListener(e -> register());
        // Enter in the last field submits, like the default button
        phoneField.addActionListener(e -> register());
        backButton.addActionListener(e -> navigator.show(ViewId.LOGIN));
    }

    private void addRow(GridBagConstraints c, int row, String text, char mnemonic, JComponent field) {
        JLabel label = new JLabel(text);
        label.setDisplayedMnemonic(mnemonic);
        label.setLabelFor(field);
        c.gridx = 0;
        c.gridy = row;
        add(label, c);
        c.gridx = 1;
        add(field, c);
    }

    private void register() {
        messageLabel.setText(" ");
        char[] typed = passwordField.getPassword();
        try {
            Customer created = controllers.customerController().register(usernameField.getText(), new String(typed),
                    nameField.getText(), emailField.getText(), phoneField.getText());
            clearFields();
            onRegistered.accept(created.username(), "Account created. You can log in now.");
        } catch (RuntimeException e) {
            messageLabel.setText(ErrorMessages.forException(e));
            passwordField.setText("");
        } finally {
            Arrays.fill(typed, ' ');
        }
    }

    String message() {
        return messageLabel.getText().trim();
    }

    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        nameField.setText("");
        emailField.setText("");
        phoneField.setText("");
        messageLabel.setText(" ");
    }

    @Override
    public void onShow() {
        clearFields();
        usernameField.requestFocusInWindow();
    }
}
