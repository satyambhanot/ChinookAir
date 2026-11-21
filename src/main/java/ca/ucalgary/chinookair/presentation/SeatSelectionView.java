package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.BookingController;
import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.control.FlightOffer;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.ReservationStatus;
import ca.ucalgary.chinookair.business.entity.Seat;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;

/**
 * Step two of booking: the seat map of the chosen flight. Opening it starts a reservation, Continue
 * gives it the chosen seat and Back gives the reservation up again. If someone else took the seat first,
 * the screen says so and shows the map as it is now.
 */
class SeatSelectionView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final Color ERROR = Theme.DANGER;
    private static final String PICK = "Pick a seat";
    private static final int SIDE_WIDTH = 320;

    private final transient Controllers controllers;
    private final transient Navigator navigator;
    private final transient BookingSession booking;

    final JButton continueButton = Theme.primary(new JButton("Continue to payment"));
    final JButton backButton = Theme.secondary(new JButton("Back"));
    private final JLabel heading = new JLabel(" ");
    private final JLabel messageLabel = new JLabel(" ");
    private final SeatMapPanel map = new SeatMapPanel();
    private final JPanel summaryHolder = new JPanel(new BorderLayout());
    private String chosenText = PICK;
    private SummaryCard summary = new SummaryCard("Your seat");
    private transient FlightOffer offer;
    private transient String chosenSeat;
    private String passenger = "";

    SeatSelectionView(Controllers controllers, Navigator navigator, BookingSession booking) {
        super(new BorderLayout(0, 10));
        this.controllers = controllers;
        this.navigator = navigator;
        this.booking = booking;
        build();
    }

    private void build() {
        setBorder(BorderFactory.createEmptyBorder(Space.XL, Space.PAGE, Space.XL, Space.PAGE));
        Theme.heading(heading);

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(heading, BorderLayout.NORTH);
        top.add(new SeatLegend(), BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // the map keeps its natural size and sits in the middle of the free space
        JPanel mapWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        mapWrapper.setBackground(Theme.SURFACE);
        mapWrapper.add(map);
        JScrollPane scroll = new JScrollPane(mapWrapper);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        add(scroll, BorderLayout.CENTER);

        // what has been chosen, and the way on
        summaryHolder.setOpaque(false);
        backButton.setMnemonic('B');
        continueButton.setMnemonic('C');
        continueButton.setEnabled(false);
        ButtonRow buttons = ButtonRow.right();
        buttons.setOpaque(false);
        buttons.add(backButton);
        buttons.add(continueButton);
        messageLabel.setFont(messageLabel.getFont().deriveFont(Type.LABEL));
        JPanel side = new JPanel(new BorderLayout(0, 12));
        side.setOpaque(false);
        side.setPreferredSize(new java.awt.Dimension(SIDE_WIDTH, 10));
        side.setBorder(BorderFactory.createEmptyBorder(0, Space.L, 0, 0));
        // the way on sits right under the card it belongs to, not at the bottom of the window
        JPanel actions = new JPanel(new BorderLayout(0, Space.S));
        actions.setOpaque(false);
        actions.add(messageLabel, BorderLayout.NORTH);
        actions.add(buttons, BorderLayout.SOUTH);
        JPanel column = new JPanel(new BorderLayout(0, Space.M));
        column.setOpaque(false);
        column.add(summaryHolder, BorderLayout.NORTH);
        column.add(actions, BorderLayout.CENTER);
        side.add(column, BorderLayout.NORTH);
        add(side, BorderLayout.EAST);
        showSummary(null);

        continueButton.addActionListener(e -> continueToPayment());
        backButton.addActionListener(e -> goBack());
    }

    @Override
    public JButton defaultButton() {
        return continueButton;
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
        // an agent moving a reservation only picks a seat: nothing is booked or given up here
        boolean moving = booking.modifying().isPresent();
        if (!moving) {
            giveUpEarlierReservation();
        }
        offer = booking.selectedOffer().get();
        messageLabel.setText(" ");
        BookingController bookings = controllers.bookingController();
        Flight flight = offer.flight();
        heading.setText((moving ? "Choose the new seat on " : "Choose a seat on ") + flight.flightNumber() + ", "
                + flight.route().origin().code() + " → " + flight.route().destination().code());
        continueButton.setText(moving ? "Confirm change" : "Continue to payment");
        try {
            passenger = customerFor(user.get()).name();
            if (!moving) {
                Reservation reservation = bookings.startReservation(user.get(), customerFor(user.get()),
                        flight.id());
                booking.setReservation(reservation);
            }
            drawMap();
        } catch (RuntimeException e) {
            if (!moving) {
                booking.setReservation(null);
            }
            map.clear();
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
                controllers.session().currentUser().ifPresent(user ->
                        controllers.bookingController().cancelPending(user, earlier.get().id()));
            } catch (RuntimeException e) {
                // it was paid or already given up: nothing to free
            }
        }
    }

    private void drawMap() {
        Flight flight = offer.flight();
        SeatGridModel seats = SeatGridModel.of(flight.aircraft(),
                controllers.bookingController().availableSeats(flight.id()));
        chosenSeat = null;
        chosenText = PICK;
        showSummary(null);
        continueButton.setEnabled(false);
        map.show(flight, seats, this::choose);
    }

    private void choose(Seat seat) {
        chosenSeat = seat.number();
        chosenText = "Seat " + seat.number() + " (" + SeatMapPanel.className(seat) + ") - $"
                + offer.flight().priceFor(seat.seatClass()).toPlainString();
        showSummary(seat);
        continueButton.setEnabled(true);
        messageLabel.setText(" ");
    }

    /** The card on the right: what is chosen so far, or a hint to choose. */
    private void showSummary(Seat seat) {
        summaryHolder.removeAll();
        SummaryCard card = new SummaryCard("Your seat");
        if (seat == null || offer == null) {
            card.addRow("Seat", "None yet");
        } else {
            Flight flight = offer.flight();
            card.addRow("Passenger", passenger)
                    .addRow("Flight", flight.flightNumber())
                    .addRow("Route", flight.route().origin().code() + " \u2192 " + flight.route().destination().code())
                    .addRow("Departs", Format.dateTime(flight.departure()))
                    .addRow("Seat", seat.number() + " (" + SeatMapPanel.className(seat) + ")")
                    .addTotal("Price", Format.money(flight.priceFor(seat.seatClass())));
        }
        summary = card;
        summaryHolder.add(card, BorderLayout.NORTH);
        summaryHolder.revalidate();
        summaryHolder.repaint();
    }

    private void continueToPayment() {
        if (booking.modifying().isPresent()) {
            confirmChange();
            return;
        }
        Optional<Reservation> reservation = booking.reservation();
        if (chosenSeat == null || reservation.isEmpty()) {
            return;
        }
        try {
            User user = controllers.session().currentUser().orElseThrow();
            booking.setReservation(controllers.bookingController().selectSeat(user, reservation.get().id(),
                    chosenSeat));
            navigator.show(ViewId.PAYMENT);
        } catch (RuntimeException e) {
            redrawAndShowError(e);
        }
    }

    /** An agent moves a reservation to the chosen flight and seat. */
    private void confirmChange() {
        Optional<User> agent = controllers.session().currentUser();
        Reservation target = booking.modifying().orElse(null);
        if (agent.isEmpty() || target == null || chosenSeat == null) {
            return;
        }
        try {
            Reservation moved = controllers.reservationController().modify(agent.get(), target.id(),
                    offer.flight().id(), chosenSeat);
            booking.stopModifying();
            if (moved.status() == ReservationStatus.PENDING_PAYMENT) {
                booking.setReservation(moved);
                booking.setNotice("The price changed, so the old payment was refunded. "
                        + "Please pay the new amount.");
                navigator.show(ViewId.PAYMENT);
            } else {
                booking.setNotice("Reservation " + moved.confirmationNumber() + " now has seat "
                        + moved.seatNo() + " on flight " + moved.flight().flightNumber() + ".");
                navigator.show(ViewId.MODIFY_RESERVATION);
            }
        } catch (RuntimeException e) {
            redrawAndShowError(e);
        }
    }

    /** Shows what went wrong and draws the map again, because the seat may have been taken meanwhile. */
    private void redrawAndShowError(RuntimeException problem) {
        String message = ErrorMessages.forException(problem);
        try {
            drawMap();
        } catch (RuntimeException again) {
            // keep the first message
        }
        showError(message);
    }

    private void goBack() {
        if (booking.modifying().isPresent()) {
            navigator.show(ViewId.FLIGHT_SEARCH);
            return;
        }
        giveUpEarlierReservation();
        navigator.show(ViewId.FLIGHT_SEARCH);
    }

    private void showError(String text) {
        messageLabel.setForeground(ERROR);
        messageLabel.setText(text);
    }

    // package-private only so the tests can look at the screen

    String message() {
        return messageLabel.getText().trim();
    }

    /** The panel that holds the summary card, for tests. */
    JPanel summaryHolder() {
        return summaryHolder;
    }

    String chosenText() {
        return chosenText;
    }

    java.util.List<SummaryCard.Row> summaryRows() {
        return summary.rows();
    }

    SeatGridModel grid() {
        return map.grid();
    }

    JToggleButton seatButtonFor(String number) {
        return map.seatButtonFor(number);
    }
}
