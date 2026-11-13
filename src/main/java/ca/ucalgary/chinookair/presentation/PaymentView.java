package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.control.PaymentResult;
import ca.ucalgary.chinookair.business.entity.PaymentDetails;
import ca.ucalgary.chinookair.business.entity.PaymentMethodType;
import ca.ucalgary.chinookair.business.entity.PaymentStatus;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.ReservationStatus;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.Optional;
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
 * Step three of booking: the card details for the reservation that waits for payment. It sends them to
 * the PaymentController. An approved payment opens the confirmation; a declined one shows the reason and
 * lets the customer try again. The card number is never kept on the screen after an attempt.
 */
class PaymentView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final Color ERROR = new Color(0xB00020);
    private static final Color NOTICE = new Color(0x1B5E20);

    private final transient Controllers controllers;
    private final transient Navigator navigator;
    private final transient BookingSession booking;

    final JComboBox<PaymentMethodType> typeCombo = new JComboBox<>(PaymentMethodType.values());
    final JTextField nameField = new JTextField(20);
    final JPasswordField cardField = new JPasswordField(20);
    final JTextField expiryField = new JTextField(6);
    final JTextField amountField = new JTextField(10);
    final JButton payButton = new JButton("Pay");
    final JButton backButton = new JButton("Back");
    private final JLabel messageLabel = new JLabel(" ");

    PaymentView(Controllers controllers, Navigator navigator, BookingSession booking) {
        super(new GridBagLayout());
        this.controllers = controllers;
        this.navigator = navigator;
        this.booking = booking;
        build();
    }

    private void build() {
        setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        JLabel heading = new JLabel("Payment");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 22f));
        amountField.setEditable(false);
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

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        add(heading, c);
        c.gridwidth = 1;
        addRow(c, 1, "Card type", 'T', typeCombo);
        addRow(c, 2, "Name on card", 'N', nameField);
        addRow(c, 3, "Card number", 'C', cardField);
        addRow(c, 4, "Expiry (MM/yy)", 'E', expiryField);
        addRow(c, 5, "Amount", 'A', amountField);
        c.gridx = 0;
        c.gridy = 6;
        c.gridwidth = 2;
        add(messageLabel, c);
        JPanel buttons = new JPanel();
        buttons.add(backButton);
        buttons.add(payButton);
        c.gridy = 7;
        add(buttons, c);

        // Enter in any field presses Pay
        nameField.addActionListener(e -> pay());
        cardField.addActionListener(e -> pay());
        expiryField.addActionListener(e -> pay());
        payButton.addActionListener(e -> pay());
        // back to the seat map when booking, back to the list when paying a reservation from it
        backButton.addActionListener(e -> navigator.show(
                booking.selectedOffer().isPresent() ? ViewId.SEAT_SELECTION : ViewId.MY_RESERVATIONS));
    }

    private void addRow(GridBagConstraints c, int row, String text, char mnemonic, Component field) {
        JLabel label = new JLabel(text);
        label.setDisplayedMnemonic(mnemonic);
        label.setLabelFor(field);
        c.gridx = 0;
        c.gridy = row;
        add(label, c);
        c.gridx = 1;
        add(field, c);
    }

    private void pay() {
        messageLabel.setText(" ");
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

    private void showError(String text) {
        messageLabel.setForeground(ERROR);
        messageLabel.setText(text);
    }

    private void clearFields() {
        nameField.setText("");
        cardField.setText("");
        expiryField.setText("");
    }

    String message() {
        return messageLabel.getText().trim();
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
        String notice = booking.takeNotice();
        messageLabel.setForeground(NOTICE);
        messageLabel.setText(notice == null ? " " : notice);
        typeCombo.setSelectedItem(PaymentMethodType.CREDIT);
        amountField.setText("$" + reservation.get().price().toPlainString());
    }
}
