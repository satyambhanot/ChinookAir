package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Airport;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.Role;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The customer's Home: the next trip, a box to start looking for a flight, and the latest bookings.
 * It shows only what is already stored; it opens other screens and changes nothing itself.
 */
class CustomerHome extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final int RECENT_COUNT = 3;
    private static final int SIDE_WIDTH = 360;

    private final transient Controllers controllers;
    private final transient Navigator navigator;
    private final transient BookingSession booking;

    private final JLabel greeting = Theme.title(" ");
    final JComboBox<Airport> originCombo = new JComboBox<>();
    final JComboBox<Airport> destinationCombo = new JComboBox<>();
    final DatePickerField dateField;
    final JButton searchButton = Theme.primary(new JButton("Search"));
    final JButton allButton = Theme.link(new JButton("See all reservations"));
    private final Alert alert = new Alert(480);
    private final JPanel tripBody = new JPanel();
    private final JPanel recentBody = new JPanel();
    private transient Optional<Reservation> shownTrip = Optional.empty();
    private transient List<Reservation> shownRecent = List.of();

    CustomerHome(Controllers controllers, Navigator navigator, BookingSession booking) {
        super(new BorderLayout(0, Space.XL));
        this.controllers = controllers;
        this.navigator = navigator;
        this.booking = booking;
        this.dateField = new DatePickerField(10, () -> controllers.flightSearchController().today());
        setName("Customer home");
        setBorder(BorderFactory.createEmptyBorder(Space.XL, Space.PAGE, Space.XL, Space.PAGE));

        JPanel top = new JPanel(new BorderLayout(0, Space.XS));
        top.setOpaque(false);
        top.add(greeting, BorderLayout.NORTH);
        top.add(Theme.hint("Here is what is coming up."), BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        JPanel left = new JPanel(new BorderLayout(0, Space.L));
        left.setOpaque(false);
        left.add(tripCard(), BorderLayout.NORTH);
        left.add(searchCard(), BorderLayout.CENTER);

        JPanel columns = new JPanel(new GridBagLayout());
        columns.setOpaque(false);
        GridBagConstraints at = new GridBagConstraints();
        at.anchor = GridBagConstraints.NORTHWEST;
        at.fill = GridBagConstraints.BOTH;
        at.gridy = 0;
        at.weightx = 1;
        at.weighty = 1;
        at.insets = new Insets(0, 0, 0, Space.L);
        columns.add(left, at);
        at.weightx = 0;
        at.insets = new Insets(0, 0, 0, 0);
        JPanel recent = recentCard();
        recent.setPreferredSize(new Dimension(SIDE_WIDTH, recent.getPreferredSize().height));
        columns.add(recent, at);
        add(ScrollableForm.around(columns), BorderLayout.CENTER);

        allButton.addActionListener(e -> navigator.show(ViewId.MY_RESERVATIONS));
        searchButton.addActionListener(e -> search());
        dateField.addActionListener(e -> search());
        searchButton.setMnemonic('S');
    }

    private JPanel tripCard() {
        JPanel card = new JPanel(new BorderLayout());
        SurfaceBorder.apply(card, Space.XL, Space.XL, Space.XL, Space.XL);
        tripBody.setOpaque(false);
        tripBody.setLayout(new BoxLayout(tripBody, BoxLayout.Y_AXIS));
        card.add(tripBody, BorderLayout.CENTER);
        return card;
    }

    private JPanel searchCard() {
        JPanel card = new JPanel(new BorderLayout(0, Space.M));
        SurfaceBorder.apply(card, Space.XL, Space.XL, Space.L, Space.XL);
        card.add(sectionTitle("Find a flight"), BorderLayout.NORTH);
        originCombo.setRenderer(new CodeRenderer());
        destinationCombo.setRenderer(new CodeRenderer());
        originCombo.setPreferredSize(new Dimension(170, originCombo.getPreferredSize().height));
        destinationCombo.setPreferredSize(new Dimension(170, destinationCombo.getPreferredSize().height));
        FilterBar bar = new FilterBar()
                .field("From", originCombo, 'F')
                .field("To", destinationCombo, 'T')
                .field("Date", dateField, 'D')
                .action(searchButton);
        JPanel body = new JPanel(new BorderLayout(0, Space.S));
        body.setOpaque(false);
        body.add(bar, BorderLayout.NORTH);
        body.add(alert, BorderLayout.CENTER);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel recentCard() {
        JPanel card = new JPanel(new BorderLayout(0, Space.M));
        SurfaceBorder.apply(card, Space.XL, Space.XL, Space.L, Space.XL);
        card.add(sectionTitle("Recent bookings"), BorderLayout.NORTH);
        recentBody.setOpaque(false);
        recentBody.setLayout(new BoxLayout(recentBody, BoxLayout.Y_AXIS));
        card.add(recentBody, BorderLayout.CENTER);
        JPanel footer = ButtonRow.left();
        footer.add(allButton);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private static JLabel sectionTitle(String text) {
        JLabel title = new JLabel(text);
        title.setFont(title.getFont().deriveFont(Font.BOLD, Type.SECTION));
        title.setForeground(Theme.TEXT);
        return title;
    }

    private void showTrip(Optional<Reservation> trip) {
        shownTrip = trip;
        tripBody.removeAll();
        JLabel caption = Theme.hint("Your next trip");
        caption.setAlignmentX(LEFT_ALIGNMENT);
        tripBody.add(caption);
        tripBody.add(Box.createVerticalStrut(Space.XS));
        if (trip.isEmpty()) {
            JLabel none = sectionTitle("No trip booked yet");
            none.setAlignmentX(LEFT_ALIGNMENT);
            JLabel hint = Theme.hint("Find a flight below and it will show up here.");
            hint.setAlignmentX(LEFT_ALIGNMENT);
            tripBody.add(none);
            tripBody.add(Box.createVerticalStrut(Space.XS));
            tripBody.add(hint);
        } else {
            Reservation reservation = trip.get();
            Flight flight = reservation.flight();
            JLabel route = new JLabel(flight.route().origin().code() + " → " + flight.route().destination().code());
            route.setFont(route.getFont().deriveFont(Font.BOLD, Type.DISPLAY));
            route.setForeground(Palette.NAVY_900);
            JLabel when = new JLabel(Format.dateTime(flight.departure()));
            when.setFont(when.getFont().deriveFont(Type.SECTION));
            JLabel details = Theme.hint(flight.airline().name() + " " + flight.flightNumber() + "  ·  Seat "
                    + reservation.seatNo() + "  ·  Confirmation " + reservation.confirmationNumber());
            for (JComponent part : new JComponent[] {route, when, details}) {
                part.setAlignmentX(LEFT_ALIGNMENT);
            }
            tripBody.add(route);
            tripBody.add(when);
            tripBody.add(Box.createVerticalStrut(Space.S));
            tripBody.add(details);
        }
        tripBody.revalidate();
        tripBody.repaint();
    }

    private void showRecent(List<Reservation> recent) {
        shownRecent = recent;
        recentBody.removeAll();
        if (recent.isEmpty()) {
            JLabel none = Theme.hint("No bookings yet.");
            none.setAlignmentX(LEFT_ALIGNMENT);
            recentBody.add(none);
        }
        for (Reservation reservation : recent) {
            recentBody.add(recentRow(reservation));
            recentBody.add(Box.createVerticalStrut(Space.M));
        }
        allButton.setEnabled(!recent.isEmpty());
        recentBody.revalidate();
        recentBody.repaint();
    }

    private static JPanel recentRow(Reservation reservation) {
        Flight flight = reservation.flight();
        JPanel row = new JPanel(new BorderLayout(Space.M, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        JLabel route = new JLabel(flight.route().origin().code() + " → " + flight.route().destination().code());
        route.setFont(route.getFont().deriveFont(Font.BOLD, Type.BODY));
        JPanel words = new JPanel(new BorderLayout());
        words.setOpaque(false);
        words.add(route, BorderLayout.NORTH);
        words.add(Theme.hint(Format.dateTime(flight.departure())), BorderLayout.CENTER);
        row.add(words, BorderLayout.CENTER);
        JPanel badge = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));
        badge.setOpaque(false);
        badge.add(new Badge(Status.of(reservation.status())));
        row.add(badge, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private void search() {
        alert.clear();
        Airport origin = (Airport) originCombo.getSelectedItem();
        Airport destination = (Airport) destinationCombo.getSelectedItem();
        LocalDate date;
        try {
            date = InputParser.date(dateField.getText());
        } catch (RuntimeException e) {
            alert.show(Alert.Kind.ERROR, ErrorMessages.forException(e));
            return;
        }
        if (origin == null || destination == null) {
            alert.show(Alert.Kind.ERROR, "Choose where you fly from and to.");
            return;
        }
        booking.rememberSearch(origin.code(), destination.code(), null, date);
        navigator.show(ViewId.FLIGHT_SEARCH);
    }

    @Override
    public void onShow() {
        Optional<User> user = controllers.session().currentUser();
        if (user.isEmpty() || user.get().role() != Role.CUSTOMER) {
            navigator.show(ViewId.LOGIN);
            return;
        }
        Customer customer = (Customer) user.get();
        greeting.setText("Hello, " + HomeTrips.firstName(customer.name(), customer.username()));
        alert.clear();
        List<Reservation> history = controllers.reservationController().history(customer, customer.id());
        LocalDate today = controllers.flightSearchController().today();
        showTrip(HomeTrips.next(history, today));
        showRecent(HomeTrips.recent(history, RECENT_COUNT));
        fillSearch();
    }

    /** The airports to choose from, starting from the last search when there was one. */
    private void fillSearch() {
        List<Airport> airports = controllers.flightSearchController().airports();
        originCombo.setModel(new DefaultComboBoxModel<>(airports.toArray(new Airport[0])));
        destinationCombo.setModel(new DefaultComboBoxModel<>(airports.toArray(new Airport[0])));
        if (airports.size() > 1) {
            originCombo.setSelectedIndex(0);
            destinationCombo.setSelectedIndex(1);
        }
        Optional<BookingSession.SearchMemory> last = booking.lastSearch();
        if (last.isPresent()) {
            select(originCombo, last.get().origin());
            select(destinationCombo, last.get().destination());
            dateField.setText(last.get().date().toString());
        } else {
            LocalDate today = controllers.flightSearchController().today();
            Optional<StartingRoute.Start> start =
                    StartingRoute.find(controllers.flightSearchController(), airports, today);
            if (start.isPresent()) {
                originCombo.setSelectedItem(start.get().from());
                destinationCombo.setSelectedItem(start.get().to());
                dateField.setText(start.get().date().toString());
            } else {
                dateField.setText(today.toString());
            }
        }
    }

    private static void select(JComboBox<Airport> combo, String code) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).code().equals(code)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    @Override
    public JButton defaultButton() {
        return searchButton;
    }

    @Override
    public JComponent initialFocus() {
        return originCombo;
    }

    // package-private only so the tests can look at the screen

    String greetingText() {
        return greeting.getText();
    }

    Optional<Reservation> shownTrip() {
        return shownTrip;
    }

    List<Reservation> shownRecent() {
        return shownRecent;
    }

    String alertText() {
        return alert.text();
    }
}
