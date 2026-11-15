package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Payment;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/** For an agent: search a customer, choose one, and read their payments, newest first. */
class PaymentRecordsView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final Color ERROR = Theme.DANGER;

    private final transient Controllers controllers;
    private final transient Navigator navigator;

    final JTextField searchField = new JTextField(20);
    final JButton searchButton = Theme.primary(new JButton("Search"));
    final JButton backButton = Theme.secondary(new JButton("Back"));
    final CustomerTableModel customerModel = new CustomerTableModel();
    final HintTable customerTable = new HintTable(customerModel, "Search for a customer above.");
    final PaymentTableModel paymentModel = new PaymentTableModel();
    final HintTable paymentTable = new HintTable(paymentModel, "Select a customer to see their payments.");
    private final JLabel messageLabel = new JLabel(" ");

    PaymentRecordsView(Controllers controllers, Navigator navigator) {
        super(new BorderLayout(0, 10));
        this.controllers = controllers;
        this.navigator = navigator;
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        JLabel heading = new JLabel("Payment records");
        Theme.heading(heading);
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

        customerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        paymentTable.setFillsViewportHeight(true);
        JPanel tables = new JPanel(new GridLayout(2, 1, 0, 10));
        tables.add(new JScrollPane(customerTable));
        tables.add(new JScrollPane(paymentTable));
        add(tables, BorderLayout.CENTER);

        backButton.setMnemonic('B');
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(backButton);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(messageLabel, BorderLayout.WEST);
        bottom.add(buttons, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        searchButton.addActionListener(e -> search());
        searchField.addActionListener(e -> search());
        customerTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showPayments();
            }
        });
        backButton.addActionListener(e -> goBack());
    }

    private void search() {
        User agent = controllers.session().currentUser().orElse(null);
        if (agent == null) {
            return;
        }
        paymentModel.setPayments(List.of());
        try {
            List<Customer> found = controllers.customerController().search(agent, searchField.getText());
            customerModel.setCustomers(found);
            showMessage(found.isEmpty() ? "No customers found." : " ", Theme.MUTED);
        } catch (RuntimeException e) {
            showMessage(ErrorMessages.forException(e), ERROR);
        }
    }

    private void showPayments() {
        int row = customerTable.getSelectedRow();
        User agent = controllers.session().currentUser().orElse(null);
        if (row < 0 || agent == null) {
            return;
        }
        Customer customer = customerModel.customerAt(customerTable.convertRowIndexToModel(row));
        try {
            List<Payment> payments = controllers.paymentController().recordsForCustomer(agent, customer.id());
            paymentModel.setPayments(payments);
            showMessage(payments.isEmpty() ? customer.name() + " has no payments." : " ", Theme.MUTED);
        } catch (RuntimeException e) {
            showMessage(ErrorMessages.forException(e), ERROR);
        }
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
