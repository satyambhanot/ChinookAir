package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Payment;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

/**
 * For an agent: find a customer, choose one, and read their payments, newest first. The search follows
 * the typing, so there is no button to press.
 */
class PaymentRecordsView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final String NO_CUSTOMERS = "No customers match. Try another name or email.";
    private static final String CHOOSE = "Select a customer to see their payments.";

    private final transient Controllers controllers;
    private final transient Navigator navigator;

    final SearchField searchField = new SearchField("Search by name or email", 24);
    final CustomerTableModel customerModel = new CustomerTableModel();
    final HintTable customerTable = new HintTable(customerModel, NO_CUSTOMERS);
    final PaymentTableModel paymentModel = new PaymentTableModel();
    final HintTable paymentTable = new HintTable(paymentModel, CHOOSE);
    private final JLabel paymentsHeading = new JLabel("Payments");
    private final Alert alert = new Alert(700);

    PaymentRecordsView(Controllers controllers, Navigator navigator) {
        super(new BorderLayout(0, 12));
        this.controllers = controllers;
        this.navigator = navigator;
        setBorder(BorderFactory.createEmptyBorder(Theme.PAGE - 8, Theme.PAGE, Theme.PAGE - 8, Theme.PAGE));
        JLabel heading = new JLabel("Payment records");
        Theme.heading(heading);
        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(heading, BorderLayout.NORTH);
        top.add(searchField, BorderLayout.CENTER);
        top.add(alert, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        customerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        paymentTable.setFillsViewportHeight(true);
        paymentsHeading.setFont(paymentsHeading.getFont().deriveFont(java.awt.Font.BOLD, Type.SECTION));

        // the customers take a quarter of the height, the payments the rest
        JPanel tables = new JPanel(new GridBagLayout());
        tables.setOpaque(false);
        GridBagConstraints at = new GridBagConstraints();
        at.gridx = 0;
        at.fill = GridBagConstraints.BOTH;
        at.weightx = 1;
        at.gridy = 0;
        at.weighty = 1;
        tables.add(new JScrollPane(customerTable), at);
        at.gridy = 1;
        at.weighty = 0;
        at.insets = new Insets(14, 0, 8, 0);
        tables.add(paymentsHeading, at);
        at.gridy = 2;
        at.weighty = 3;
        at.insets = new Insets(0, 0, 0, 0);
        tables.add(new JScrollPane(paymentTable), at);
        add(tables, BorderLayout.CENTER);

        searchField.onChange(text -> search());
        customerTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showPayments();
            }
        });
    }

    /** Looks up the customers that match the search box; it runs after every change of the text. */
    void search() {
        User agent = controllers.session().currentUser().orElse(null);
        if (agent == null) {
            return;
        }
        paymentModel.setPayments(List.of());
        paymentsHeading.setText("Payments");
        paymentTable.setEmptyText(CHOOSE);
        try {
            customerModel.setCustomers(controllers.customerController().search(agent, searchField.getText()));
            alert.clear();
        } catch (RuntimeException e) {
            alert.show(Alert.Kind.ERROR, ErrorMessages.forException(e));
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
            paymentsHeading.setText("Payments of " + customer.name());
            paymentTable.setEmptyText(customer.name() + " has no payments.");
            alert.clear();
        } catch (RuntimeException e) {
            alert.show(Alert.Kind.ERROR, ErrorMessages.forException(e));
        }
    }

    @Override
    public JComponent initialFocus() {
        return searchField;
    }

    @Override
    public void onShow() {
        if (controllers.session().currentUser().isEmpty()) {
            navigator.show(ViewId.LOGIN);
            return;
        }
        alert.clear();
        searchField.setText("");
        search();
    }

    String message() {
        return alert.text();
    }

    String paymentsTitle() {
        return paymentsHeading.getText();
    }
}
