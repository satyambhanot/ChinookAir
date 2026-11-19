package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.ReservationStatus;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * One reservation as a card: where it goes and when, its status, seat, price and confirmation number,
 * and the things that can be done with it. A button that does not apply is not there.
 */
final class ReservationCard extends JPanel {

    private static final long serialVersionUID = 1L;

    final JButton payButton = Theme.primary(new JButton("Pay now"));
    final JButton cancelButton = Theme.danger(new JButton("Cancel reservation"));
    private final transient Reservation reservation;

    ReservationCard(Reservation reservation, boolean canCancel, Runnable pay, Runnable cancel) {
        super(new BorderLayout(Space.XL, 0));
        this.reservation = reservation;
        SurfaceBorder.apply(this, Space.L, Space.XL, Space.L, Space.XL);
        Flight flight = reservation.flight();

        JLabel route = new JLabel(flight.route().origin().code() + " → " + flight.route().destination().code());
        route.setFont(route.getFont().deriveFont(Font.BOLD, Type.SECTION));
        route.setForeground(Palette.NAVY_900);
        ButtonRow head = ButtonRow.left();
        head.add(route);
        head.add(new Badge(Status.of(reservation.status())));
        JLabel when = new JLabel(Format.dateTime(flight.departure()));
        JLabel what = Theme.hint(flight.airline().name() + " " + flight.flightNumber() + "  ·  Seat "
                + (reservation.seatNo() == null ? "not chosen yet" : Format.seat(flight, reservation.seatNo()))
                + "  ·  Confirmation " + reservation.confirmationNumber());

        JPanel words = new JPanel(new GridBagLayout());
        words.setOpaque(false);
        GridBagConstraints at = new GridBagConstraints();
        at.gridx = 0;
        at.weightx = 1;
        at.anchor = GridBagConstraints.WEST;
        at.gridy = 0;
        words.add(head, at);
        at.gridy = 1;
        at.insets = new Insets(Space.XS, 0, 0, 0);
        words.add(when, at);
        at.gridy = 2;
        words.add(what, at);
        add(words, BorderLayout.CENTER);

        JPanel side = new JPanel(new BorderLayout(0, Space.S));
        side.setOpaque(false);
        JLabel price = new JLabel(reservation.price() == null ? "" : Format.money(reservation.price()),
                JLabel.RIGHT);
        price.setFont(price.getFont().deriveFont(Font.BOLD, Type.SECTION));
        side.add(price, BorderLayout.NORTH);
        ButtonRow actions = ButtonRow.right();
        boolean canPay = reservation.status() == ReservationStatus.PENDING_PAYMENT;
        payButton.setVisible(canPay);
        cancelButton.setVisible(canCancel);
        payButton.addActionListener(e -> pay.run());
        cancelButton.addActionListener(e -> cancel.run());
        actions.add(payButton);
        actions.add(cancelButton);
        side.add(actions, BorderLayout.SOUTH);
        add(side, BorderLayout.EAST);
        getAccessibleContext().setAccessibleName("Reservation " + reservation.confirmationNumber() + ", "
                + flight.route().origin().code() + " to " + flight.route().destination().code() + ", "
                + Status.of(reservation.status()).text());
    }

    Reservation reservation() {
        return reservation;
    }
}
