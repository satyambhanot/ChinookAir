package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * The first screen. It reads a username and a password, asks the LoginController, remembers the
 * user in the session and sends them to the dashboard of their role. It holds no rules of its own.
 */
class LoginView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final Color ERROR = new Color(0xB00020);
    private static final Color NOTICE = new Color(0x1B5E20);

    private final transient Controllers controllers;
    private final transient Navigator navigator;

    final JTextField usernameField = new JTextField(20);
    final JPasswordField passwordField = new JPasswordField(20);
    final JButton loginButton = new JButton("Log in");
    final JButton registerButton = new JButton("Register a new account");
    private final JLabel messageLabel = new JLabel(" ", SwingConstants.CENTER);

    LoginView(Controllers controllers, Navigator navigator) {
        super(new GridBagLayout());
        this.controllers = controllers;
        this.navigator = navigator;
        build();
    }

    private void build() {
        JLabel title = new JLabel("ChinookAir", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        JLabel subtitle = new JLabel("Flight reservation system", SwingConstants.CENTER);

        JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setDisplayedMnemonic('U');
        usernameLabel.setLabelFor(usernameField);
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setDisplayedMnemonic('P');
        passwordLabel.setLabelFor(passwordField);
        loginButton.setMnemonic('L');
        registerButton.setMnemonic('R');
        registerButton.setBorderPainted(false);
        registerButton.setContentAreaFilled(false);
        registerButton.setForeground(new Color(0x0B57D0));
        messageLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(title, c);
        c.gridy = 1;
        c.insets = new Insets(0, 6, 20, 6);
        add(subtitle, c);
        c.insets = new Insets(4, 6, 4, 6);
        c.gridwidth = 1;
        c.gridy = 2;
        add(usernameLabel, c);
        c.gridx = 1;
        add(usernameField, c);
        c.gridx = 0;
        c.gridy = 3;
        add(passwordLabel, c);
        c.gridx = 1;
        add(passwordField, c);
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        add(messageLabel, c);
        c.gridy = 5;
        add(loginButton, c);
        c.gridy = 6;
        add(registerButton, c);

        // Enter in either field presses Log in
        usernameField.addActionListener(e -> logIn());
        passwordField.addActionListener(e -> logIn());
        loginButton.addActionListener(e -> logIn());
        registerButton.addActionListener(e -> navigator.show(ViewId.REGISTER));
    }

    private void logIn() {
        messageLabel.setText(" ");
        char[] typed = passwordField.getPassword();
        try {
            User user = controllers.loginController().authenticate(usernameField.getText(), new String(typed));
            controllers.session().login(user);
            clearFields();
            navigator.show(ViewId.dashboardFor(user.role()));
        } catch (RuntimeException e) {
            showMessage(ErrorMessages.forException(e), ERROR);
            // a wrong password is never kept on the screen
            passwordField.setText("");
        } finally {
            Arrays.fill(typed, ' ');
        }
    }

    /** Shows a friendly message above the Log in button, for example after registering. */
    void showNotice(String text) {
        showMessage(text, NOTICE);
    }

    /** Puts a username in the field and the cursor in the password field. */
    void prefill(String username) {
        usernameField.setText(username);
        passwordField.requestFocusInWindow();
    }

    String message() {
        return messageLabel.getText().trim();
    }

    private void showMessage(String text, Color color) {
        messageLabel.setForeground(color);
        messageLabel.setText(text);
    }

    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        messageLabel.setText(" ");
    }

    @Override
    public void onShow() {
        clearFields();
        usernameField.requestFocusInWindow();
    }
}
