package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.control.PaymentResult;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/** The last step of booking: the confirmation number, the trip, and where the confirmation was saved. */
class ConfirmationView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final transient Controllers controllers;
    private final transient Navigator navigator;
    private final transient BookingSession booking;

    final JButton dashboardButton = new JButton("Back to dashboard");
    private final JLabel heading = new JLabel("Booking confirmed");
    private final JTextArea details = new JTextArea(10, 40);

    ConfirmationView(Controllers controllers, Navigator navigator, BookingSession booking) {
        super(new BorderLayout(0, 15));
        this.controllers = controllers;
        this.navigator = navigator;
        this.booking = booking;
        setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 22f));
        details.setEditable(false);
        details.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        dashboardButton.setMnemonic('D');
        JPanel buttons = new JPanel();
        buttons.add(dashboardButton);
        add(heading, BorderLayout.NORTH);
        add(details, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        dashboardButton.addActionListener(e -> backToDashboard());
    }

    private void backToDashboard() {
        booking.clear();
        Optional<User> user = controllers.session().currentUser();
        navigator.show(user.isPresent() ? ViewId.dashboardFor(user.get().role()) : ViewId.LOGIN);
    }

    @Override
    public void onShow() {
        if (controllers.session().currentUser().isEmpty()) {
            navigator.show(ViewId.LOGIN);
            return;
        }
        Optional<PaymentResult> result = booking.payment();
        if (result.isEmpty()) {
            navigator.show(ViewId.FLIGHT_SEARCH);
            return;
        }
        details.setText(textOf(result.get()));
    }

    private static String textOf(PaymentResult result) {
        Reservation reservation = result.reservation();
        Flight flight = reservation.flight();
        String saved = result.confirmationFile() != null ? "Saved to: " + result.confirmationFile()
                : result.message();
        return "Confirmation number: " + reservation.confirmationNumber() + "\n"
                + "Passenger: " + reservation.customer().name() + "\n"
                + "Flight: " + flight.flightNumber() + " (" + flight.airline().name() + ")\n"
                + "Route: " + flight.route().origin().code() + " → " + flight.route().destination().code() + "\n"
                + "Departure: " + flight.departure().format(WHEN) + "\n"
                + "Seat: " + reservation.seatNo() + "\n"
                + "Amount paid: $" + result.payment().amount().toPlainString() + "\n\n"
                + saved;
    }

    // ---- for the tests

    String detailsText() {
        return details.getText();
    }
}
