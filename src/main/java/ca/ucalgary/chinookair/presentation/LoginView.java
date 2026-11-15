package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Arrays;
import javax.swing.JComponent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
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
    private static final int FIELD_WIDTH = 300;
    private static final int LOGO_WIDTH = 240;

    private final transient Controllers controllers;
    private final transient Navigator navigator;
    final JTextField usernameField = Theme.placeholder(new JTextField(20), "Your username");
    final JPasswordField passwordField = new JPasswordField(20);
    final JButton loginButton = Theme.primary(new JButton("Log in"));
    final JButton registerButton = Theme.link(new JButton("Create an account"));
    private final Alert alert = new Alert(FIELD_WIDTH - 40);

    LoginView(Controllers controllers, Navigator navigator) {
        super(new GridBagLayout());
        this.controllers = controllers;
        this.navigator = navigator;
        build();
    }

    private void build() {
        JPanel card = Theme.card();
        card.removeAll();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel logo = new JLabel(new ImageIcon(Brand.scaledToWidth(Brand.fullLogo(), LOGO_WIDTH)));
        logo.getAccessibleContext().setAccessibleName("ChinookAir");
        JLabel subtitle = Theme.hint("Log in to book and manage your flights");
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);

        passwordField.putClientProperty("JPasswordField.showRevealButton", Boolean.TRUE);
        Theme.placeholder(passwordField, "Your password");
        FormField userRow = new FormField("Username", usernameField).mnemonic('U');
        FormField passwordRow = new FormField("Password", passwordField).mnemonic('P');
        loginButton.setMnemonic('L');
        registerButton.setMnemonic('R');
        loginButton.setPreferredSize(new Dimension(FIELD_WIDTH, 40));
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        for (JPanel part : new JPanel[] {userRow, passwordRow, alert}) {
            part.setMaximumSize(new Dimension(Integer.MAX_VALUE, part.getMaximumSize().height));
        }
        for (Component part : new Component[] {logo, subtitle, userRow, passwordRow, alert, loginButton,
                registerButton}) {
            ((javax.swing.JComponent) part).setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        userRow.setPreferredSize(new Dimension(FIELD_WIDTH, userRow.getPreferredSize().height));
        passwordRow.setPreferredSize(new Dimension(FIELD_WIDTH, passwordRow.getPreferredSize().height));
        card.add(logo);
        card.add(Box.createVerticalStrut(4));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(20));
        card.add(userRow);
        card.add(Box.createVerticalStrut(4));
        card.add(passwordRow);
        card.add(Box.createVerticalStrut(4));
        card.add(alert);
        card.add(Box.createVerticalStrut(12));
        card.add(loginButton);
        card.add(Box.createVerticalStrut(8));
        card.add(registerButton);
        card.setBorder(BorderFactory.createCompoundBorder(card.getBorder(), BorderFactory.createEmptyBorder()));
        add(card, new GridBagConstraints());

        // Enter in either field presses Log in
        usernameField.addActionListener(e -> logIn());
        passwordField.addActionListener(e -> logIn());
        loginButton.addActionListener(e -> logIn());
        registerButton.addActionListener(e -> navigator.show(ViewId.REGISTER));
    }

    private void logIn() {
        alert.clear();
        char[] typed = passwordField.getPassword();
        try {
            User user = controllers.loginController().authenticate(usernameField.getText(), new String(typed));
            controllers.session().login(user);
            clearFields();
            navigator.show(ViewId.dashboardFor(user.role()));
        } catch (RuntimeException e) {
            alert.show(Alert.Kind.ERROR, ErrorMessages.forException(e));
            // a wrong password is never kept on the screen, and the cursor is where the retry starts
            passwordField.setText("");
            passwordField.requestFocusInWindow();
        } finally {
            Arrays.fill(typed, ' ');
        }
    }

    /** Shows good news above the Log in button, for example after registering. */
    void showNotice(String text) {
        alert.show(Alert.Kind.SUCCESS, text);
    }

    /** Puts a username in the field and the cursor in the password field. */
    void prefill(String username) {
        usernameField.setText(username);
        passwordField.requestFocusInWindow();
    }

    String message() {
        return alert.text();
    }

    Alert.Kind messageKind() {
        return alert.kind();
    }

    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        alert.clear();
    }

    @Override
    public JButton defaultButton() {
        return loginButton;
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
}
