package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.ReservationStatus;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

/**
 * The reservations of a customer, newest first: the logged-in customer, or the customer an agent chose.
 * A reservation can be cancelled while its flight has not left, and one that waits for payment can be paid.
 */
class MyReservationsView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final Color ERROR = new Color(0xB00020);

    private final transient Controllers controllers;
    private final transient Navigator navigator;
    private final transient BookingSession booking;
    private transient Predicate<String> confirmer = this::askYesNo;

    final ReservationTableModel tableModel = new ReservationTableModel();
    final JTable table = new JTable(tableModel);
    final JButton cancelButton = new JButton("Cancel reservation");
    final JButton payButton = new JButton("Pay now");
    final JButton backButton = new JButton("Back");
    private final JLabel messageLabel = new JLabel(" ");
    private transient Customer owner;

    MyReservationsView(Controllers controllers, Navigator navigator, BookingSession booking) {
        super(new BorderLayout(0, 10));
        this.controllers = controllers;
        this.navigator = navigator;
        this.booking = booking;
        build();
    }

    private void build() {
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        JLabel heading = new JLabel("Reservations");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 22f));
        add(heading, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        cancelButton.setMnemonic('C');
        payButton.setMnemonic('P');
        backButton.setMnemonic('B');
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(backButton);
        buttons.add(payButton);
        buttons.add(cancelButton);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(messageLabel, BorderLayout.WEST);
        bottom.add(buttons, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        updateButtons();
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        cancelButton.addActionListener(e -> cancelSelected());
        payButton.addActionListener(e -> paySelected());
        backButton.addActionListener(e -> goBack());
    }

    private void updateButtons() {
        Reservation selected = selected();
        payButton.setEnabled(selected != null && selected.status() == ReservationStatus.PENDING_PAYMENT);
        cancelButton.setEnabled(selected != null && controllers.reservationController().canCancel(selected));
    }

    private Reservation selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : tableModel.reservationAt(table.convertRowIndexToModel(row));
    }

    private void cancelSelected() {
        Reservation reservation = selected();
        User user = controllers.session().currentUser().orElse(null);
        if (reservation == null || user == null) {
            return;
        }
        String question = "Cancel reservation " + reservation.confirmationNumber() + "?";
        if (reservation.status() == ReservationStatus.CONFIRMED) {
            question += " You will be refunded $" + reservation.price().toPlainString() + ".";
        }
        if (!confirmer.test(question)) {
            return;
        }
        try {
            controllers.reservationController().cancel(user, reservation.id());
            load();
        } catch (RuntimeException e) {
            showError(ErrorMessages.forException(e));
        }
    }

    private void paySelected() {
        Reservation reservation = selected();
        if (reservation == null) {
            return;
        }
        // a reservation paid from this list has no flight search behind it
        Optional<Customer> chosen = booking.customer();
        booking.clear();
        chosen.ifPresent(booking::bookFor);
        booking.setReservation(reservation);
        navigator.show(ViewId.PAYMENT);
    }

    private void goBack() {
        Optional<User> user = controllers.session().currentUser();
        navigator.show(user.isPresent() ? ViewId.dashboardFor(user.get().role()) : ViewId.LOGIN);
    }

    private boolean askYesNo(String question) {
        return JOptionPane.showConfirmDialog(this, question, "ChinookAir", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }

    /** The customer whose reservations are shown: the one an agent chose, or else the person logged in. */
    private Customer ownerOf(User user) {
        Optional<Customer> chosen = booking.customer();
        if (chosen.isPresent()) {
            return chosen.get();
        }
        if (user instanceof Customer) {
            return (Customer) user;
        }
        throw new IllegalStateException("Choose a customer first");
    }

    private void load() {
        User user = controllers.session().currentUser().orElseThrow();
        List<Reservation> history = controllers.reservationController().history(user, owner.id());
        tableModel.setReservations(history);
        updateButtons();
        if (history.isEmpty()) {
            messageLabel.setForeground(Color.DARK_GRAY);
            messageLabel.setText("No reservations yet.");
        } else {
            messageLabel.setText(" ");
        }
    }

    private void showError(String text) {
        messageLabel.setForeground(ERROR);
        messageLabel.setText(text);
    }

    @Override
    public void onShow() {
        Optional<User> user = controllers.session().currentUser();
        if (user.isEmpty()) {
            navigator.show(ViewId.LOGIN);
            return;
        }
        tableModel.setReservations(List.of());
        updateButtons();
        try {
            owner = ownerOf(user.get());
            load();
        } catch (RuntimeException e) {
            showError(ErrorMessages.forException(e));
        }
    }

    // ---- for the tests

    String message() {
        return messageLabel.getText().trim();
    }

    /** Replaces the Yes/No question, so a test can answer it without a dialog. */
    void setConfirmer(Predicate<String> answer) {
        confirmer = answer;
    }
}
