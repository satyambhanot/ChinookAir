package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.Role;
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
import javax.swing.ListSelectionModel;

/**
 * For a flight agent: the reservations of the chosen customer and a Modify button. Modify sends the
 * agent to the search and seat screens to pick the new flight and seat; the seat screen then asks the
 * ReservationController to make the change.
 */
class ModifyReservationView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final Color ERROR = new Color(0xB00020);
    private static final Color NOTICE = new Color(0x1B5E20);

    private final transient Controllers controllers;
    private final transient Navigator navigator;
    private final transient BookingSession booking;

    final ReservationTableModel tableModel = new ReservationTableModel();
    final JTable table = new JTable(tableModel);
    final JButton modifyButton = new JButton("Modify");
    final JButton backButton = new JButton("Back");
    private final JLabel heading = new JLabel("Modify a reservation");
    private final JLabel messageLabel = new JLabel(" ");

    ModifyReservationView(Controllers controllers, Navigator navigator, BookingSession booking) {
        super(new BorderLayout(0, 10));
        this.controllers = controllers;
        this.navigator = navigator;
        this.booking = booking;
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 22f));
        add(heading, BorderLayout.NORTH);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        modifyButton.setMnemonic('M');
        backButton.setMnemonic('B');
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(backButton);
        buttons.add(modifyButton);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(messageLabel, BorderLayout.WEST);
        bottom.add(buttons, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        modifyButton.setEnabled(false);
        table.getSelectionModel().addListSelectionListener(e -> updateButton());
        modifyButton.addActionListener(e -> modifySelected());
        backButton.addActionListener(e -> {
            booking.clear();
            navigator.show(ViewId.CUSTOMER_MANAGEMENT);
        });
    }

    private Reservation selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : tableModel.reservationAt(table.convertRowIndexToModel(row));
    }

    private void updateButton() {
        Reservation chosen = selected();
        modifyButton.setEnabled(chosen != null && controllers.reservationController().canModify(chosen));
    }

    private void modifySelected() {
        Reservation chosen = selected();
        if (chosen == null) {
            return;
        }
        booking.startModifying(chosen);
        navigator.show(ViewId.FLIGHT_SEARCH);
    }

    private void showMessage(String text, Color color) {
        messageLabel.setForeground(color);
        messageLabel.setText(text);
    }

    @Override
    public void onShow() {
        Optional<User> user = controllers.session().currentUser();
        if (user.isEmpty() || user.get().role() != Role.AGENT) {
            // only a flight agent changes reservations
            navigator.show(ViewId.LOGIN);
            return;
        }
        String notice = booking.takeNotice();
        tableModel.setReservations(List.of());
        updateButton();
        Optional<Customer> customer = booking.customer();
        if (customer.isEmpty()) {
            heading.setText("Modify a reservation");
            showMessage("Choose a customer first", ERROR);
            return;
        }
        heading.setText("Modify a reservation of " + customer.get().name());
        try {
            tableModel.setReservations(controllers.reservationController().history(user.get(), customer.get().id()));
            if (notice != null) {
                showMessage(notice, NOTICE);
            } else {
                messageLabel.setText(" ");
            }
        } catch (RuntimeException e) {
            showMessage(ErrorMessages.forException(e), ERROR);
        }
    }

    String message() {
        return messageLabel.getText().trim();
    }
}
