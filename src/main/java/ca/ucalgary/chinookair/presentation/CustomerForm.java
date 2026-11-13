package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.entity.Customer;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * The fields of a customer: username, name, email and phone, and a starting password when a new
 * customer is entered. The agent's list and the customer's own profile both use it.
 */
class CustomerForm extends JPanel {

    private static final long serialVersionUID = 1L;

    final JTextField usernameField = new JTextField(20);
    final JPasswordField passwordField = new JPasswordField(20);
    final JTextField nameField = new JTextField(20);
    final JTextField emailField = new JTextField(20);
    final JTextField phoneField = new JTextField(20);
    private final JLabel passwordLabel = new JLabel("Starting password");

    CustomerForm() {
        super(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 6, 3, 6);
        c.anchor = GridBagConstraints.WEST;
        addRow(c, 0, new JLabel("Username"), usernameField);
        addRow(c, 1, passwordLabel, passwordField);
        addRow(c, 2, label("Name", 'N', nameField), nameField);
        addRow(c, 3, label("Email", 'E', emailField), emailField);
        addRow(c, 4, label("Phone", 'H', phoneField), phoneField);
        usernameField.setEditable(false);
        showNewCustomerFields(false);
    }

    private static JLabel label(String text, char mnemonic, Component field) {
        JLabel label = new JLabel(text);
        label.setDisplayedMnemonic(mnemonic);
        label.setLabelFor(field);
        return label;
    }

    private void addRow(GridBagConstraints c, int row, JLabel label, Component field) {
        c.gridx = 0;
        c.gridy = row;
        add(label, c);
        c.gridx = 1;
        add(field, c);
    }

    /** Shows a customer. The username can not be changed. */
    void show(Customer customer) {
        showNewCustomerFields(false);
        usernameField.setText(customer.username());
        passwordField.setText("");
        nameField.setText(customer.name());
        emailField.setText(customer.email());
        phoneField.setText(customer.phone() == null ? "" : customer.phone());
    }

    /** Empty fields for a new customer: the username and the starting password can be typed. */
    void startNew() {
        showNewCustomerFields(true);
        usernameField.setText("");
        passwordField.setText("");
        nameField.setText("");
        emailField.setText("");
        phoneField.setText("");
        usernameField.requestFocusInWindow();
    }

    /** Empty fields and nothing to edit, for when no customer is chosen. */
    void clear() {
        showNewCustomerFields(false);
        usernameField.setText("");
        passwordField.setText("");
        nameField.setText("");
        emailField.setText("");
        phoneField.setText("");
    }

    private void showNewCustomerFields(boolean isNew) {
        usernameField.setEditable(isNew);
        passwordLabel.setVisible(isNew);
        passwordField.setVisible(isNew);
    }

    boolean isNewCustomer() {
        return passwordField.isVisible();
    }

    /** The typed password; the caller wipes it after use. */
    char[] password() {
        return passwordField.getPassword();
    }

    void wipePassword() {
        char[] typed = passwordField.getPassword();
        Arrays.fill(typed, ' ');
        passwordField.setText("");
    }
}
