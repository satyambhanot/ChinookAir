package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.entity.Customer;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Arrays;
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
    private final FormField passwordRow = new FormField("Starting password", passwordField);

    CustomerForm() {
        super(new GridBagLayout());
        setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        int row = 0;
        for (FormField field : new FormField[] {new FormField("Username", usernameField), passwordRow,
                new FormField("Name", nameField).mnemonic('N'), new FormField("Email", emailField).mnemonic('E'),
                new FormField("Phone", phoneField).mnemonic('H')}) {
            c.gridy = row++;
            add(field, c);
        }
        usernameField.setEditable(false);
        showNewCustomerFields(false);
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
        passwordRow.setVisible(isNew);
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
