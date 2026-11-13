package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/**
 * The agent's list of customers: search by name or email, add a customer, change a customer's data and
 * open a customer's reservations. The rules are the CustomerController's; the screen only shows its answers.
 */
class CustomerManagementView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final Color ERROR = new Color(0xB00020);
    private static final Color NOTICE = new Color(0x1B5E20);

    private final transient Controllers controllers;
    private final transient Navigator navigator;
    private final transient BookingSession booking;

    final JTextField searchField = new JTextField(20);
    final JButton searchButton = new JButton("Search");
    final CustomerTableModel tableModel = new CustomerTableModel();
    final JTable table = new JTable(tableModel);
    final CustomerForm form = new CustomerForm();
    final JButton newButton = new JButton("New customer");
    final JButton saveButton = new JButton("Save");
    final JButton reservationsButton = new JButton("Reservations");
    final JButton backButton = new JButton("Back");
    private final JLabel messageLabel = new JLabel(" ");

    CustomerManagementView(Controllers controllers, Navigator navigator, BookingSession booking) {
        super(new BorderLayout(0, 10));
        this.controllers = controllers;
        this.navigator = navigator;
        this.booking = booking;
        build();
    }

    private void build() {
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        JLabel heading = new JLabel("Customers");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 22f));
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JLabel searchLabel = new JLabel("Name or email");
        searchLabel.setDisplayedMnemonic('M');
        searchLabel.setLabelFor(searchField);
        searchRow.add(searchLabel);
        searchRow.add(searchField);
        searchRow.add(searchButton);
        searchButton.setMnemonic('S');
        JPanel top = new JPanel(new BorderLayout());
        top.add(heading, BorderLayout.NORTH);
        top.add(searchRow, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        newButton.setMnemonic('W');
        saveButton.setMnemonic('V');
        reservationsButton.setMnemonic('R');
        backButton.setMnemonic('B');
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(backButton);
        buttons.add(reservationsButton);
        buttons.add(newButton);
        buttons.add(saveButton);
        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.add(form, BorderLayout.NORTH);
        bottom.add(messageLabel, BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        updateButtons();
        searchButton.addActionListener(e -> search());
        searchField.addActionListener(e -> search());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                rowChosen();
            }
        });
        newButton.addActionListener(e -> startNew());
        saveButton.addActionListener(e -> save());
        reservationsButton.addActionListener(e -> openReservations());
        backButton.addActionListener(e -> goBack());
    }

    private void updateButtons() {
        reservationsButton.setEnabled(table.getSelectedRow() >= 0);
        saveButton.setEnabled(table.getSelectedRow() >= 0 || form.isNewCustomer());
    }

    private void rowChosen() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            form.show(tableModel.customerAt(table.convertRowIndexToModel(row)));
        }
        updateButtons();
    }

    private Customer selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : tableModel.customerAt(table.convertRowIndexToModel(row));
    }

    private void search() {
        User agent = controllers.session().currentUser().orElse(null);
        if (agent == null) {
            return;
        }
        try {
            List<Customer> found = controllers.customerController().search(agent, searchField.getText());
            tableModel.setCustomers(found);
            form.clear();
            updateButtons();
            if (found.isEmpty()) {
                showMessage("No customers found.", NOTICE);
            } else {
                messageLabel.setText(" ");
            }
        } catch (RuntimeException e) {
            showMessage(ErrorMessages.forException(e), ERROR);
        }
    }

    private void startNew() {
        table.clearSelection();
        form.startNew();
        messageLabel.setText(" ");
        updateButtons();
    }

    private void save() {
        User agent = controllers.session().currentUser().orElse(null);
        if (agent == null) {
            return;
        }
        try {
            Customer saved;
            if (form.isNewCustomer()) {
                saved = controllers.customerController().register(form.usernameField.getText(),
                        new String(form.password()), form.nameField.getText(), form.emailField.getText(),
                        form.phoneField.getText());
            } else {
                Customer chosen = selected();
                if (chosen == null) {
                    return;
                }
                saved = controllers.customerController().update(agent, chosen.id(), form.nameField.getText(),
                        form.emailField.getText(), form.phoneField.getText());
            }
            // look for the saved customer again, so the table shows what is stored
            searchField.setText(saved.email());
            search();
            showMessage("Saved " + saved.name(), NOTICE);
            if (tableModel.getRowCount() > 0) {
                table.setRowSelectionInterval(0, 0);
            }
        } catch (RuntimeException e) {
            showMessage(ErrorMessages.forException(e), ERROR);
        } finally {
            form.wipePassword();
        }
    }

    private void openReservations() {
        Customer chosen = selected();
        if (chosen == null) {
            return;
        }
        booking.clear();
        booking.bookFor(chosen);
        navigator.show(ViewId.MY_RESERVATIONS);
    }

    private void goBack() {
        Optional<User> user = controllers.session().currentUser();
        navigator.show(user.isPresent() ? ViewId.dashboardFor(user.get().role()) : ViewId.LOGIN);
    }

    private void showMessage(String text, Color color) {
        messageLabel.setForeground(color);
        messageLabel.setText(text);
    }

    @Override
    public void onShow() {
        if (controllers.session().currentUser().isEmpty()) {
            navigator.show(ViewId.LOGIN);
            return;
        }
        searchField.setText("");
        search();
    }

    String message() {
        return messageLabel.getText().trim();
    }
}
