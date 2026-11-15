package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.Role;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Optional;
import javax.swing.JComponent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

/**
 * For a flight agent: the reservations of the chosen customer and a Modify button. Modify sends the
 * agent to the search and seat screens to pick the new flight and seat; the seat screen then asks the
 * ReservationController to make the change.
 */
class ModifyReservationView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;

    private final transient Controllers controllers;
    private final transient Navigator navigator;
    private final transient BookingSession booking;

    final ReservationTableModel tableModel = new ReservationTableModel();
    final HintTable table = new HintTable(tableModel, "This customer has no reservations to change.");
    final JButton modifyButton = Theme.primary(new JButton("Modify"));
    final JButton backButton = Theme.secondary(new JButton("Back to customers"));
    private final JLabel heading = new JLabel("Modify a reservation");
    private final Alert alert = new Alert(600);

    ModifyReservationView(Controllers controllers, Navigator navigator, BookingSession booking) {
        super(new BorderLayout(0, 10));
        this.controllers = controllers;
        this.navigator = navigator;
        this.booking = booking;
        setBorder(BorderFactory.createEmptyBorder(Theme.PAGE - 8, Theme.PAGE, Theme.PAGE - 8, Theme.PAGE));
        Theme.heading(heading);
        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.setOpaque(false);
        top.add(heading, BorderLayout.NORTH);
        top.add(Theme.hint("Pick a reservation, then choose its new flight and seat."), BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        modifyButton.setMnemonic('M');
        backButton.setMnemonic('B');
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(backButton);
        buttons.add(modifyButton);
        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);
        bottom.add(alert, BorderLayout.NORTH);
        bottom.add(buttons, BorderLayout.SOUTH);
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
        boolean can = chosen != null && controllers.reservationController().canModify(chosen);
        modifyButton.setEnabled(can);
        // a button that cannot be pressed says why
        modifyButton.setToolTipText(can ? null : chosen == null ? "Select a reservation first"
                : "This reservation can no longer be changed");
    }

    private void modifySelected() {
        Reservation chosen = selected();
        if (chosen == null) {
            return;
        }
        booking.startModifying(chosen);
        navigator.show(ViewId.FLIGHT_SEARCH);
    }

    @Override
    public JButton defaultButton() {
        return modifyButton;
    }

    @Override
    public JComponent initialFocus() {
        return table;
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
        alert.clear();
        tableModel.setReservations(List.of());
        updateButton();
        Optional<Customer> customer = booking.customer();
        if (customer.isEmpty()) {
            heading.setText("Modify a reservation");
            alert.show(Alert.Kind.ERROR, "Choose a customer first");
            return;
        }
        heading.setText("Modify a reservation of " + customer.get().name());
        try {
            tableModel.setReservations(controllers.reservationController().history(user.get(), customer.get().id()));
            alert.show(Alert.Kind.SUCCESS, notice);
        } catch (RuntimeException e) {
            alert.show(Alert.Kind.ERROR, ErrorMessages.forException(e));
        }
    }

    String message() {
        return alert.text();
    }

    Alert.Kind messageKind() {
        return alert.kind();
    }
}
