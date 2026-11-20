package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.BookingController;
import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.control.FlightOffer;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.ReservationStatus;
import ca.ucalgary.chinookair.business.entity.Seat;
import ca.ucalgary.chinookair.business.entity.SeatClass;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
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
    private static final Color ERROR = Theme.DANGER;
    private static final Color BUSINESS = Palette.GOLD_100;
    private static final Color ECONOMY = Theme.ACCENT_SOFT;
    private static final Color TAKEN = Palette.TAKEN;
    private static final Color SELECTED = Theme.ACCENT;
    private static final String PICK = "Pick a seat";
    private static final int SIDE_WIDTH = 320;
    private static final int SEAT_WIDTH = 56;
    private static final int SEAT_HEIGHT = 30;
    private static final int AISLE_WIDTH = 28;

    private final transient Controllers controllers;
    private final transient Navigator navigator;
    private final transient BookingSession booking;

    final JButton continueButton = Theme.primary(new JButton("Continue to payment"));
    final JButton backButton = Theme.secondary(new JButton("Back"));
    private final JLabel heading = new JLabel(" ");
    private final JLabel messageLabel = new JLabel(" ");
    private final JPanel mapPanel = new JPanel(new GridBagLayout());
    private final JPanel summaryHolder = new JPanel(new BorderLayout());
    private String chosenText = PICK;
    private SummaryCard summary = new SummaryCard("Your seat");
    private transient SeatGridModel grid;
    private transient JToggleButton[][] buttons = new JToggleButton[0][0];
    private final transient Map<String, JToggleButton> seatButtons = new HashMap<>();
    private transient ButtonGroup group = new ButtonGroup();
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

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        legend.setOpaque(false);
        legend.add(legendItem("Business", BUSINESS, false));
        legend.add(legendItem("Economy", ECONOMY, false));
        legend.add(legendItem("Taken", TAKEN, true));
        legend.add(legendItem("Your seat", SELECTED, false));
        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(heading, BorderLayout.NORTH);
        top.add(legend, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // the map keeps its natural size and sits in the middle of the free space
        JPanel mapWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        mapWrapper.setBackground(Theme.SURFACE);
        mapPanel.setOpaque(false);
        mapWrapper.add(mapPanel);
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

    /** One entry of the legend: a small square in the colour of the seats, and what it means. */
    private static JLabel legendItem(String text, Color color, boolean crossed) {
        JLabel label = new JLabel(text, new Swatch(color, crossed), SwingConstants.LEFT);
        label.setIconTextGap(Space.S);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, Space.L));
        label.setForeground(Theme.TEXT);
        label.setFont(label.getFont().deriveFont(Type.LABEL));
        return label;
    }

    /** A small rounded square in a seat colour; a taken seat has the cross it has on the map. */
    private static final class Swatch implements javax.swing.Icon {

        private static final int SIZE = 16;
        private final Color color;
        private final boolean crossed;

        Swatch(Color color, boolean crossed) {
            this.color = color;
            this.crossed = crossed;
        }

        @Override
        public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(x, y, SIZE, SIZE, 6, 6);
            g2.setColor(Palette.BORDER_STRONG);
            g2.drawRoundRect(x, y, SIZE - 1, SIZE - 1, 6, 6);
            if (crossed) {
                g2.setColor(Theme.MUTED);
                g2.drawLine(x + 4, y + 4, x + SIZE - 5, y + SIZE - 5);
                g2.drawLine(x + SIZE - 5, y + 4, x + 4, y + SIZE - 5);
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }
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
        grid = SeatGridModel.of(flight.aircraft(), bookings.availableSeats(flight.id()));
        mapPanel.removeAll();
        seatButtons.clear();
        group = new ButtonGroup();
        chosenSeat = null;
        chosenText = PICK;
        showSummary(null);
        continueButton.setEnabled(false);
        buttons = new JToggleButton[grid.rowCount()][grid.columnCount()];

        int aisle = grid.aisleAfter();
        int last = grid.columnCount() + (aisle >= 0 ? 1 : 0) + 1;
        GridBagConstraints at = new GridBagConstraints();
        at.insets = new Insets(2, 2, 2, 2);
        at.fill = GridBagConstraints.BOTH;
        for (int c = 0; c < grid.columnCount(); c++) {
            at.gridx = 1 + column(c, aisle);
            at.gridy = 0;
            mapPanel.add(centred(String.valueOf(SeatGridModel.letter(c)), Theme.MUTED), at);
        }
        int y = 1;
        for (int r = 0; r < grid.rowCount(); r++) {
            if (r == 0 || grid.seatAt(r, 0).seatClass() != grid.seatAt(r - 1, 0).seatClass()) {
                at.gridy = y++;
                at.gridx = 0;
                at.gridwidth = last + 1;
                at.insets = new Insets(r == 0 ? Space.S : Space.L, 2, Space.XS, 2);
                mapPanel.add(cabinLabel(r), at);
                at.gridwidth = 1;
                at.insets = new Insets(2, 2, 2, 2);
            }
            at.gridy = y++;
            at.gridx = 0;
            mapPanel.add(centred(String.valueOf(r + 1), Theme.MUTED), at);
            at.gridx = last;
            mapPanel.add(centred(String.valueOf(r + 1), Theme.MUTED), at);
            for (int c = 0; c < grid.columnCount(); c++) {
                at.gridx = 1 + column(c, aisle);
                mapPanel.add(seatButton(grid.seatAt(r, c), grid.isEnabled(r, c), r, c), at);
            }
        }
        if (aisle >= 0) {
            at.gridx = 1 + aisle;
            at.gridy = 0;
            mapPanel.add(javax.swing.Box.createHorizontalStrut(AISLE_WIDTH), at);
        }
        mapPanel.revalidate();
        mapPanel.repaint();
    }

    /** "BUSINESS, rows 1 to 2": a heading over the first row of a cabin. */
    private JLabel cabinLabel(int firstRow) {
        SeatClass cabin = grid.seatAt(firstRow, 0).seatClass();
        int lastRow = firstRow;
        while (lastRow + 1 < grid.rowCount() && grid.seatAt(lastRow + 1, 0).seatClass() == cabin) {
            lastRow++;
        }
        String rows = firstRow == lastRow ? "row " + (firstRow + 1) : "rows " + (firstRow + 1) + " to " + (lastRow + 1);
        JLabel label = new JLabel((cabin == SeatClass.BUSINESS ? "Business" : "Economy") + " \u00b7 " + rows);
        label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD, Type.CAPTION));
        label.setForeground(Theme.MUTED);
        return label;
    }

    /** The grid column of a seat column: seats after the aisle move one column to the right. */
    private static int column(int seatColumn, int aisleAfter) {
        return aisleAfter >= 0 && seatColumn >= aisleAfter ? seatColumn + 1 : seatColumn;
    }

    private static JLabel centred(String text, Color color) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setForeground(color);
        label.setFont(label.getFont().deriveFont(Type.LABEL));
        label.setPreferredSize(new java.awt.Dimension(28, SEAT_HEIGHT));
        return label;
    }

    private JToggleButton seatButton(Seat seat, boolean enabled, int row, int column) {
        // a taken seat shows a cross, so it differs from a free seat by more than its colour
        JToggleButton button = new JToggleButton(enabled ? seat.number() : "\u00D7");
        button.setOpaque(true);
        button.setBackground(enabled ? (seat.seatClass() == SeatClass.BUSINESS ? BUSINESS : ECONOMY) : TAKEN);
        button.setEnabled(enabled);
        button.setMargin(new Insets(2, 2, 2, 2));
        button.setPreferredSize(new java.awt.Dimension(SEAT_WIDTH, SEAT_HEIGHT));
        button.setFont(button.getFont().deriveFont(Type.LABEL));
        String price = enabled ? Format.money(offer.flight().priceFor(seat.seatClass())) : "";
        button.getAccessibleContext().setAccessibleName("Seat " + seat.number()
                + (enabled ? ", " + className(seat) + ", " + price : ", taken"));
        button.setToolTipText(enabled ? seat.number() + " \u00b7 " + className(seat) + " \u00b7 " + price
                : "Seat " + seat.number() + " is taken");
        // the chosen seat turns blue, so it is easy to find in the grid
        button.putClientProperty("FlatLaf.styleClass", "seat");
        if (enabled) {
            button.addActionListener(e -> choose(seat));
            bindArrows(button, row, column);
        }
        group.add(button);
        seatButtons.put(seat.number(), button);
        buttons[row][column] = button;
        return button;
    }

    /** The arrow keys move from seat to seat, skipping the seats that are taken. */
    private void bindArrows(JToggleButton button, int row, int column) {
        int[][] steps = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        String[] keys = {"UP", "DOWN", "LEFT", "RIGHT"};
        for (int i = 0; i < keys.length; i++) {
            int[] step = steps[i];
            String name = "seat-" + keys[i];
            button.getInputMap(javax.swing.JComponent.WHEN_FOCUSED).put(javax.swing.KeyStroke.getKeyStroke(keys[i]),
                    name);
            button.getActionMap().put(name, new javax.swing.AbstractAction() {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    int[] next = grid.nextEnabled(row, column, step[0], step[1]);
                    if (next != null) {
                        buttons[next[0]][next[1]].requestFocusInWindow();
                    }
                }
            });
        }
    }

    private static String className(Seat seat) {
        return seat.seatClass() == SeatClass.BUSINESS ? "Business" : "Economy";
    }

    private void choose(Seat seat) {
        chosenSeat = seat.number();
        chosenText = "Seat " + seat.number() + " (" + className(seat) + ") - $"
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
                    .addRow("Seat", seat.number() + " (" + className(seat) + ")")
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
            booking.setReservation(controllers.bookingController().selectSeat(reservation.get().id(), chosenSeat));
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

    // ---- for the tests

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
        return grid;
    }

    JToggleButton seatButtonFor(String number) {
        return seatButtons.get(number);
    }
}
