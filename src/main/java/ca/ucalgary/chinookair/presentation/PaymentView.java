package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.control.PaymentResult;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.PaymentDetails;
import ca.ucalgary.chinookair.business.entity.PaymentMethodType;
import ca.ucalgary.chinookair.business.entity.PaymentStatus;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.ReservationStatus;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.Optional;
import javax.swing.JComponent;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * Step three of booking: the card details on the left and what is being paid for on the right. It sends
 * the details to the PaymentController. An approved payment opens the confirmation; a declined one
 * shows the reason and lets the customer try again. A problem with one field is shown under that field.
 * The card number is never kept on the screen after an attempt.
 */
class PaymentView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final Color ERROR = Theme.DANGER;
    private static final Color NOTICE = Theme.SUCCESS;
    private static final int FORM_WIDTH = 440;
    private static final int SUMMARY_WIDTH = 320;

    private final transient Controllers controllers;
    private final transient Navigator navigator;
    private final transient BookingSession booking;

    final JComboBox<PaymentMethodType> typeCombo = new JComboBox<>(PaymentMethodType.values());
    final JTextField nameField = Theme.placeholder(new JTextField(20), "As printed on the card");
    final JPasswordField cardField = Theme.placeholder(new JPasswordField(20), "1234 5678 9012 3456");
    final JTextField expiryField = Theme.placeholder(new JTextField(6), "MM/yy");
    final JButton payButton = Theme.primary(new JButton("Pay"));
    final JButton backButton = Theme.secondary(new JButton("Back"));
    private final FormField typeRow = new FormField("Card type", typeCombo).mnemonic('T');
    private final FormField nameRow = new FormField("Name on card", nameField).mnemonic('N');
    private final FormField cardRow = new FormField("Card number", cardField).mnemonic('C');
    private final FormField expiryRow = new FormField("Expiry date", expiryField).mnemonic('E');
    private final JLabel messageLabel = new JLabel(" ");
    private final JPanel summaryHolder = new JPanel(new BorderLayout());
    private SummaryCard summary = new SummaryCard("Order summary");
    private String shown = "";

    PaymentView(Controllers controllers, Navigator navigator, BookingSession booking) {
        super(new BorderLayout(0, 16));
        this.controllers = controllers;
        this.navigator = navigator;
        this.booking = booking;
        build();
    }

    private void build() {
        InputFilters.only(cardField, "0-9 -", 23);
        InputFilters.only(expiryField, "0-9/", 5);
        // the number is hidden as it is typed; the eye shows it again to check it
        cardField.putClientProperty("JPasswordField.showRevealButton", Boolean.TRUE);
        setBorder(BorderFactory.createEmptyBorder(Space.XL, Space.PAGE, Space.PAGE, Space.PAGE));
        JLabel heading = new JLabel("Payment");
        Theme.heading(heading);
        add(heading, BorderLayout.NORTH);

        typeCombo.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focus) {
                String text = value == PaymentMethodType.DEBIT ? "Debit card" : "Credit card";
                return super.getListCellRendererComponent(list, text, index, selected, focus);
            }
        });
        payButton.setMnemonic('P');
        backButton.setMnemonic('B');
        messageLabel.setFont(messageLabel.getFont().deriveFont(Type.LABEL));

        JPanel form = new JPanel();
        form.setBackground(Theme.SURFACE);
        form.setLayout(new javax.swing.BoxLayout(form, javax.swing.BoxLayout.Y_AXIS));
        SurfaceBorder.apply(form, Space.XL, Space.XL, Space.XL, Space.XL);
        JLabel section = new JLabel("Card details");
        section.setFont(section.getFont().deriveFont(java.awt.Font.BOLD, Type.SECTION));
        section.setAlignmentX(LEFT_ALIGNMENT);
        form.add(section);
        form.add(javax.swing.Box.createVerticalStrut(12));
        // name and number first, then the type and the expiry date on one line; Tab follows this order
        for (FormField row : new FormField[] {nameRow, cardRow}) {
            row.setAlignmentX(LEFT_ALIGNMENT);
            form.add(row);
        }
        JPanel typeAndExpiry = new JPanel(new java.awt.GridLayout(1, 2, 12, 0));
        typeAndExpiry.setOpaque(false);
        typeAndExpiry.setAlignmentX(LEFT_ALIGNMENT);
        typeAndExpiry.add(typeRow);
        typeAndExpiry.add(expiryRow);
        form.add(typeAndExpiry);
        messageLabel.setAlignmentX(LEFT_ALIGNMENT);
        form.add(messageLabel);
        JLabel keptNote = Theme.hint("Only the last four digits of the card are kept.");
        keptNote.setAlignmentX(LEFT_ALIGNMENT);
        keptNote.setBorder(BorderFactory.createEmptyBorder(0, Space.fieldEdge(), 0, 0));
        form.add(keptNote);
        form.add(javax.swing.Box.createVerticalStrut(12));
        ButtonRow buttons = ButtonRow.right();
        buttons.setOpaque(false);
        buttons.add(backButton);
        buttons.add(payButton);
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        form.add(buttons);

        summaryHolder.setOpaque(false);
        summaryHolder.add(summary, BorderLayout.NORTH);

        JPanel columns = new JPanel(new GridBagLayout());
        columns.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.insets = new Insets(0, 0, 0, Space.XL);
        form.setPreferredSize(new java.awt.Dimension(FORM_WIDTH, form.getPreferredSize().height));
        columns.add(form, c);
        c.gridx = 1;
        c.insets = new Insets(0, 0, 0, 0);
        columns.add(summaryHolder, c);
        c.gridx = 2;
        c.weightx = 1;
        columns.add(javax.swing.Box.createHorizontalGlue(), c);
        add(ScrollableForm.around(columns), BorderLayout.CENTER);

        // Enter in any field presses Pay
        nameField.addActionListener(e -> pay());
        cardField.addActionListener(e -> pay());
        expiryField.addActionListener(e -> pay());
        payButton.addActionListener(e -> pay());
        // back to the seat map when booking, back to the list when paying a reservation from it
        backButton.addActionListener(e -> navigator.show(
                booking.selectedOffer().isPresent() ? ViewId.SEAT_SELECTION : ViewId.MY_RESERVATIONS));
    }

    private void pay() {
        clearErrors();
        Optional<User> user = controllers.session().currentUser();
        Optional<Reservation> reservation = booking.reservation();
        if (user.isEmpty() || reservation.isEmpty()) {
            return;
        }
        char[] typed = cardField.getPassword();
        try {
            PaymentDetails details = new PaymentDetails(nameField.getText(), new String(typed),
                    expiryField.getText());
            PaymentResult result = controllers.paymentController().pay(user.get(), reservation.get().id(),
                    (PaymentMethodType) typeCombo.getSelectedItem(), details);
            if (result.payment().status() == PaymentStatus.APPROVED) {
                booking.setReservation(result.reservation());
                booking.setPayment(result);
                clearFields();
                navigator.show(ViewId.CONFIRMATION);
            } else {
                showError(result.message());
            }
        } catch (RuntimeException e) {
            showError(ErrorMessages.forException(e));
        } finally {
            // a card number is never kept on the screen after an attempt
            Arrays.fill(typed, ' ');
            cardField.setText("");
        }
    }

    /** Puts the problem under the field it is about, or in the general line when it is about the payment. */
    private void showError(String text) {
        shown = text;
        String lower = text.toLowerCase();
        if (lower.contains("card number")) {
            cardRow.showError(text);
        } else if (lower.contains("expir")) {
            expiryRow.showError(text);
        } else if (lower.contains("name")) {
            nameRow.showError(text);
        } else {
            messageLabel.setForeground(ERROR);
            messageLabel.setText(text);
        }
    }

    private void clearErrors() {
        shown = "";
        messageLabel.setText(" ");
        for (FormField row : new FormField[] {typeRow, nameRow, cardRow, expiryRow}) {
            row.clearError();
        }
    }

    private void clearFields() {
        nameField.setText("");
        cardField.setText("");
        expiryField.setText("");
    }

    /** The last message shown, wherever it appears on the screen. */
    String message() {
        return shown.isEmpty() ? messageLabel.getText().trim() : shown;
    }

    @Override
    public JButton defaultButton() {
        return payButton;
    }

    @Override
    public JComponent initialFocus() {
        return nameField;
    }

    @Override
    public void onShow() {
        if (controllers.session().currentUser().isEmpty()) {
            navigator.show(ViewId.LOGIN);
            return;
        }
        Optional<Reservation> reservation = booking.reservation();
        if (reservation.isEmpty() || reservation.get().status() != ReservationStatus.PENDING_PAYMENT) {
            // nothing waits for payment: a paid reservation can not be paid twice
            navigator.show(ViewId.FLIGHT_SEARCH);
            return;
        }
        clearFields();
        clearErrors();
        String notice = booking.takeNotice();
        messageLabel.setForeground(NOTICE);
        messageLabel.setText(notice == null ? " " : notice);
        typeCombo.setSelectedItem(PaymentMethodType.CREDIT);
        showSummary(reservation.get());
    }

    private void showSummary(Reservation reservation) {
        Flight flight = reservation.flight();
        String price = Format.money(reservation.price());
        summaryHolder.removeAll();
        summary = new SummaryCard("Order summary")
                .addRow("Passenger", reservation.customer().name())
                .addRow("Flight", flight.flightNumber() + " (" + flight.airline().name() + ")")
                .addRow("Route", flight.route().origin().code() + " \u2192 " + flight.route().destination().code())
                .addRow("Departs", Format.dateTime(flight.departure()))
                .addRow("Seat", Format.seat(flight, reservation.seatNo()))
                .addTotal("Total", price);
        summaryHolder.add(summary, BorderLayout.NORTH);
        summaryHolder.setPreferredSize(new java.awt.Dimension(SUMMARY_WIDTH, summary.getPreferredSize().height));
        summaryHolder.revalidate();
        summaryHolder.repaint();
        payButton.setText("Pay " + price);
    }

    // ---- for the tests

    /** The total of the order summary, like "$184.00". */
    String summaryTotal() {
        java.util.List<SummaryCard.Row> rows = summary.rows();
        return rows.isEmpty() ? "" : rows.get(rows.size() - 1).value();
    }

    java.util.List<SummaryCard.Row> summaryRows() {
        return summary.rows();
    }

    FormField cardRow() {
        return cardRow;
    }

    FormField expiryRow() {
        return expiryRow;
    }

    FormField nameRow() {
        return nameRow;
    }
}
