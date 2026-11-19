package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Payment;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
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
    private static final int LIST_WIDTH = 420;

    private final transient Controllers controllers;
    private final transient Navigator navigator;

    final SearchField searchField = new SearchField("Search by name or email", 24);
    final CustomerTableModel customerModel = new CustomerTableModel();
    final HintTable customerTable = new HintTable(customerModel, NO_CUSTOMERS);
    final PaymentTableModel paymentModel = new PaymentTableModel();
    final HintTable paymentTable = new HintTable(paymentModel, CHOOSE);
    private final JLabel paymentsHeading = new JLabel("Payments");
    private final JLabel paymentsTotals = new JLabel(" ");
    private final Alert alert = new Alert(700);

    PaymentRecordsView(Controllers controllers, Navigator navigator) {
        super(new BorderLayout(0, 12));
        this.controllers = controllers;
        this.navigator = navigator;
        setBorder(BorderFactory.createEmptyBorder(Space.XL, Space.PAGE, Space.XL, Space.PAGE));
        JLabel heading = new JLabel("Payment records");
        Theme.heading(heading);
        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(heading, BorderLayout.NORTH);
        top.add(alert, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        customerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customerTable.describe("Customers");
        paymentTable.setFillsViewportHeight(true);
        paymentTable.describe("Payments");
        paymentTable.getColumnModel().getColumn(0).setCellRenderer(HintTable.timeWithYearRenderer());
        paymentsHeading.setFont(paymentsHeading.getFont().deriveFont(java.awt.Font.BOLD, Type.SECTION));

        // the customers on the left, and on the right the payments of the one that is chosen
        customerTable.removeColumn(customerTable.getColumn("Phone"));
        customerTable.removeColumn(customerTable.getColumn("Username"));
        JPanel left = new JPanel(new BorderLayout(0, Space.M));
        left.setOpaque(false);
        left.setPreferredSize(new java.awt.Dimension(LIST_WIDTH, 10));
        JPanel searchRow = new JPanel(new BorderLayout());
        searchRow.setOpaque(false);
        searchRow.add(searchField, BorderLayout.SOUTH);
        left.add(searchRow, BorderLayout.NORTH);
        left.add(new JScrollPane(customerTable), BorderLayout.CENTER);
        paymentsTotals.setForeground(Theme.MUTED);
        JPanel headings = new JPanel(new BorderLayout(0, Space.XS));
        headings.setOpaque(false);
        headings.add(paymentsHeading, BorderLayout.NORTH);
        headings.add(paymentsTotals, BorderLayout.CENTER);
        // both lists start at the same height: the search box sits at the bottom of a row as tall as the headings
        searchRow.setPreferredSize(new java.awt.Dimension(10, headings.getPreferredSize().height));
        JPanel right = new JPanel(new BorderLayout(0, Space.M));
        right.setOpaque(false);
        right.add(headings, BorderLayout.NORTH);
        right.add(new JScrollPane(paymentTable), BorderLayout.CENTER);
        JPanel columns = new JPanel(new BorderLayout(Space.L, 0));
        columns.setOpaque(false);
        columns.add(left, BorderLayout.WEST);
        columns.add(right, BorderLayout.CENTER);
        add(columns, BorderLayout.CENTER);

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
        paymentsTotals.setText(" ");
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
            paymentsTotals.setText(totalsText(payments));
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

    /**
     * What the payments add up to, like "Paid $184.00 · Refunded $0.00 · 2 payments". Only payments
     * that went through count as paid, and only refunded ones as refunded.
     */
    static String totalsText(List<Payment> payments) {
        java.math.BigDecimal paid = java.math.BigDecimal.ZERO;
        java.math.BigDecimal refunded = java.math.BigDecimal.ZERO;
        for (Payment payment : payments) {
            if (payment.status() == ca.ucalgary.chinookair.business.entity.PaymentStatus.APPROVED) {
                paid = paid.add(payment.amount());
            } else if (payment.status() == ca.ucalgary.chinookair.business.entity.PaymentStatus.REFUNDED) {
                refunded = refunded.add(payment.amount());
            }
        }
        return "Paid " + Format.money(paid) + "  \u00b7  Refunded " + Format.money(refunded) + "  \u00b7  "
                + payments.size() + (payments.size() == 1 ? " payment" : " payments");
    }

    String totalsLine() {
        return paymentsTotals.getText().trim();
    }

    String paymentsTitle() {
        return paymentsHeading.getText();
    }
}
