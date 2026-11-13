package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.BookingController;
import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.control.FlightOffer;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.Seat;
import ca.ucalgary.chinookair.business.entity.SeatClass;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

/**
 * Step two of booking: the seat map of the chosen flight. Opening it starts a reservation, Continue
 * gives it the chosen seat and Back gives the reservation up again. If someone else took the seat first,
 * the screen says so and shows the map as it is now.
 */
class SeatSelectionView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final Color ERROR = new Color(0xB00020);
    private static final Color BUSINESS = new Color(0xFFE08A);
    private static final Color ECONOMY = new Color(0xD7E8FA);
    private static final Color TAKEN = new Color(0xBDBDBD);
    private static final String PICK = "Pick a seat";

    private final transient Controllers controllers;
    private final transient Navigator navigator;
    private final transient BookingSession booking;

    final JButton continueButton = new JButton("Continue to payment");
    final JButton backButton = new JButton("Back");
    private final JLabel heading = new JLabel(" ");
    private final JLabel chosenLabel = new JLabel(PICK);
    private final JLabel messageLabel = new JLabel(" ");
    private final JPanel mapPanel = new JPanel();
    private final transient Map<String, JToggleButton> seatButtons = new HashMap<>();
    private transient ButtonGroup group = new ButtonGroup();
    private transient FlightOffer offer;
    private transient String chosenSeat;

    SeatSelectionView(Controllers controllers, Navigator navigator, BookingSession booking) {
        super(new BorderLayout(0, 10));
        this.controllers = controllers;
        this.navigator = navigator;
        this.booking = booking;
        build();
    }

    private void build() {
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 22f));
        add(heading, BorderLayout.NORTH);

        add(new JScrollPane(mapPanel), BorderLayout.CENTER);

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        legend.add(swatch("Business", BUSINESS));
        legend.add(swatch("Economy", ECONOMY));
        legend.add(swatch("Taken", TAKEN));
        chosenLabel.setFont(chosenLabel.getFont().deriveFont(Font.BOLD, 14f));
        JPanel info = new JPanel(new BorderLayout());
        info.add(legend, BorderLayout.NORTH);
        info.add(chosenLabel, BorderLayout.CENTER);
        info.add(messageLabel, BorderLayout.SOUTH);

        backButton.setMnemonic('B');
        continueButton.setMnemonic('C');
        continueButton.setEnabled(false);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(backButton);
        buttons.add(continueButton);
        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.add(info, BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        continueButton.addActionListener(e -> continueToPayment());
        backButton.addActionListener(e -> goBack());
    }

    private static JLabel swatch(String text, Color color) {
        JLabel label = new JLabel(" " + text + " ", SwingConstants.CENTER);
        label.setOpaque(true);
        label.setBackground(color);
        return label;
    }

    @Override
    public void onShow() {
        Optional<User> user = controllers.session().currentUser();
        if (user.isEmpty()) {
            navigator.show(ViewId.LOGIN);
            return;
        }
        if (booking.selectedOffer().isEmpty()) {
            navigator.show(ViewId.FLIGHT_SEARCH);
            return;
        }
        giveUpEarlierReservation();
        offer = booking.selectedOffer().get();
        messageLabel.setText(" ");
        BookingController bookings = controllers.bookingController();
        Flight flight = offer.flight();
        heading.setText("Choose a seat on " + flight.flightNumber() + ", "
                + flight.route().origin().code() + " → " + flight.route().destination().code());
        try {
            Reservation reservation = bookings.startReservation(user.get(), customerFor(user.get()), flight.id());
            booking.setReservation(reservation);
            drawMap();
        } catch (RuntimeException e) {
            booking.setReservation(null);
            mapPanel.removeAll();
            mapPanel.revalidate();
            showError(ErrorMessages.forException(e));
        }
    }

    /** The customer the booking is for: the one chosen for it, or else the person logged in. */
    private Customer customerFor(User user) {
        Optional<Customer> chosen = booking.customer();
        if (chosen.isPresent()) {
            return chosen.get();
        }
        if (user instanceof Customer) {
            return (Customer) user;
        }
        throw new IllegalStateException("Choose a customer to book for first");
    }

    /** A reservation left over from the payment screen is given up, so no orphan stays. */
    private void giveUpEarlierReservation() {
        Optional<Reservation> earlier = booking.reservation();
        booking.setReservation(null);
        if (earlier.isPresent() && earlier.get().id() != null) {
            try {
                controllers.bookingController().cancelPending(earlier.get().id());
            } catch (RuntimeException e) {
                // it was paid or already given up: nothing to free
            }
        }
    }

    private void drawMap() {
        BookingController bookings = controllers.bookingController();
        Flight flight = offer.flight();
        SeatGridModel grid = SeatGridModel.of(flight.aircraft(), bookings.availableSeats(flight.id()));
        mapPanel.removeAll();
        seatButtons.clear();
        group = new ButtonGroup();
        chosenSeat = null;
        chosenLabel.setText(PICK);
        continueButton.setEnabled(false);
        mapPanel.setLayout(new GridLayout(grid.rowCount() + 1, grid.columnCount() + 1, 4, 4));
        mapPanel.add(new JLabel(" "));
        for (int c = 0; c < grid.columnCount(); c++) {
            mapPanel.add(new JLabel(String.valueOf(SeatGridModel.letter(c)), SwingConstants.CENTER));
        }
        for (int r = 0; r < grid.rowCount(); r++) {
            mapPanel.add(new JLabel(String.valueOf(r + 1), SwingConstants.CENTER));
            for (int c = 0; c < grid.columnCount(); c++) {
                mapPanel.add(seatButton(grid.seatAt(r, c), grid.isEnabled(r, c)));
            }
        }
        mapPanel.revalidate();
        mapPanel.repaint();
    }

    private JToggleButton seatButton(Seat seat, boolean enabled) {
        JToggleButton button = new JToggleButton(seat.number());
        button.setOpaque(true);
        button.setBackground(enabled ? (seat.seatClass() == SeatClass.BUSINESS ? BUSINESS : ECONOMY) : TAKEN);
        button.setEnabled(enabled);
        button.setMargin(new Insets(2, 2, 2, 2));
        if (enabled) {
            button.addActionListener(e -> choose(seat));
        }
        group.add(button);
        seatButtons.put(seat.number(), button);
        return button;
    }

    private void choose(Seat seat) {
        chosenSeat = seat.number();
        String className = seat.seatClass() == SeatClass.BUSINESS ? "Business" : "Economy";
        chosenLabel.setText("Seat " + seat.number() + " (" + className + ") - $"
                + offer.flight().priceFor(seat.seatClass()).toPlainString());
        continueButton.setEnabled(true);
        messageLabel.setText(" ");
    }

    private void continueToPayment() {
        Optional<Reservation> reservation = booking.reservation();
        if (chosenSeat == null || reservation.isEmpty()) {
            return;
        }
        try {
            booking.setReservation(controllers.bookingController().selectSeat(reservation.get().id(), chosenSeat));
            navigator.show(ViewId.PAYMENT);
        } catch (RuntimeException e) {
            String message = ErrorMessages.forException(e);
            try {
                drawMap();
            } catch (RuntimeException again) {
                // keep the first message
            }
            showError(message);
        }
    }

    private void goBack() {
        giveUpEarlierReservation();
        navigator.show(ViewId.FLIGHT_SEARCH);
    }

    private void showError(String text) {
        messageLabel.setForeground(ERROR);
        messageLabel.setText(text);
    }

    // ---- for the tests

    String message() {
        return messageLabel.getText().trim();
    }

    String chosenText() {
        return chosenLabel.getText();
    }

    JToggleButton seatButtonFor(String number) {
        return seatButtons.get(number);
    }
}
