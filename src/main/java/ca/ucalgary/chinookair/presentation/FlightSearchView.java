package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.control.FlightOffer;
import ca.ucalgary.chinookair.business.control.SearchCriteria;
import ca.ucalgary.chinookair.business.entity.Airline;
import ca.ucalgary.chinookair.business.entity.Airport;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.time.LocalDate;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;
import javax.swing.JComponent;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

/**
 * The search screen: origin, destination, date and airline in, a table of flights out. It parses the
 * date and shows what the FlightSearchController answers; the rules about the input are the controller's.
 */
class FlightSearchView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final Color ERROR = Theme.DANGER;
    private static final String NO_FLIGHTS = "No flights found. Try another date or airline.";

    private final transient Controllers controllers;
    private final transient Navigator navigator;
    private final transient BookingSession booking;

    final JComboBox<Airport> originCombo = new JComboBox<>();
    final JComboBox<Airport> destinationCombo = new JComboBox<>();
    final DatePickerField dateField;
    final JComboBox<Airline> airlineCombo = new JComboBox<>();
    final JButton searchButton = Theme.primary(new JButton("Search"));
    final JButton selectButton = Theme.primary(new JButton("Select flight"));
    final JButton backButton = Theme.secondary(new JButton("Back"));
    final JButton swapButton = new JButton(Icons.muted("swap", 18));
    final FlightResultList results = new FlightResultList("Choose the airports and a date, then press Search.");
    final DayStrip dayStrip = new DayStrip();
    final JToggleButton[] sortChips = new JToggleButton[FlightResultList.Sort.values().length];
    private final JLabel caption = Theme.hint(" ");
    private final JPanel resultsHead = new JPanel(new BorderLayout(0, Space.M));
    private final JPanel sorting = new JPanel();
    private final JLabel heading = new JLabel("Find flights");
    private final JLabel messageLabel = new JLabel(" ");
    FilterBar searchBar;

    FlightSearchView(Controllers controllers, Navigator navigator, BookingSession booking) {
        super(new BorderLayout(0, 12));
        this.controllers = controllers;
        this.navigator = navigator;
        this.booking = booking;
        this.dateField = new DatePickerField(10, () -> controllers.flightSearchController().today());
        build();
    }

    private void build() {
        setBorder(BorderFactory.createEmptyBorder(Space.XL, Space.PAGE, Space.XL, Space.PAGE));

        Theme.heading(heading);
        originCombo.setPreferredSize(new Dimension(190, originCombo.getPreferredSize().height));
        destinationCombo.setPreferredSize(new Dimension(190, destinationCombo.getPreferredSize().height));
        airlineCombo.setPreferredSize(new Dimension(190, airlineCombo.getPreferredSize().height));
        swapButton.putClientProperty("JButton.buttonType", "toolBarButton");
        swapButton.setToolTipText("Swap the two airports");
        swapButton.getAccessibleContext().setAccessibleName("Swap the two airports");
        swapButton.setMnemonic('W');

        JPanel card = new JPanel(new BorderLayout());
        SurfaceBorder.apply(card, Space.L, Space.XL, Space.L, Space.XL);
        searchButton.setMnemonic('S');
        searchBar = new FilterBar()
                .field("From", originCombo, 'F')
                .action(swapButton)
                .field("To", destinationCombo, 'T')
                .field("Date", dateField, 'D')
                .field("Airline", airlineCombo, 'A')
                .action(searchButton);
        card.add(searchBar, BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(heading, BorderLayout.NORTH);
        top.add(card, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        buildResultsHead();

        selectButton.setMnemonic('E');
        backButton.setMnemonic('B');
        ButtonRow buttons = ButtonRow.right();
        buttons.add(backButton);
        buttons.add(selectButton);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(messageLabel, BorderLayout.WEST);
        bottom.add(buttons, BorderLayout.EAST);
        JPanel body = new JPanel(new BorderLayout(0, Space.M));
        body.setOpaque(false);
        body.add(resultsHead, BorderLayout.NORTH);
        body.add(new ResultsArea(results, bottom), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        originCombo.setRenderer(new CodeRenderer());
        destinationCombo.setRenderer(new CodeRenderer());
        airlineCombo.setRenderer(new CodeRenderer());

        selectButton.setEnabled(false);
        results.addListSelectionListener(e -> selectButton.setEnabled(results.selectedOffer() != null));
        searchButton.addActionListener(e -> search());
        swapButton.addActionListener(e -> swapAirports());
        dateField.addActionListener(e -> search());
        selectButton.addActionListener(e -> selectFlight());
        // a double click on a flight does what Select flight does
        results.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && results.selectedOffer() != null) {
                    selectFlight();
                }
            }
        });
        // Enter on a chosen card chooses that flight, instead of searching again
        results.getInputMap(javax.swing.JComponent.WHEN_FOCUSED).put(javax.swing.KeyStroke.getKeyStroke("ENTER"),
                "chooseFlight");
        results.getActionMap().put("chooseFlight", new javax.swing.AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (results.selectedOffer() != null) {
                    selectFlight();
                }
            }
        });
        dayStrip.onPick(day -> {
            dateField.setDate(day);
            search();
        });
        backButton.addActionListener(e -> goBack());
    }

    private void search() {
        messageLabel.setText(" ");
        try {
            Airport origin = (Airport) originCombo.getSelectedItem();
            Airport destination = (Airport) destinationCombo.getSelectedItem();
            Airline airline = (Airline) airlineCombo.getSelectedItem();
            SearchCriteria criteria = new SearchCriteria(origin == null ? null : origin.code(),
                    destination == null ? null : destination.code(), InputParser.date(dateField.getText()),
                    airline == null ? null : airline.code());
            List<FlightOffer> offers = controllers.flightSearchController().search(criteria);
            booking.rememberSearch(criteria.origin(), criteria.destination(), criteria.airlineCode(), criteria.date());
            results.setOffers(offers);
            results.setEmptyText("No flights match this search.");
            showResultsHead(criteria, offers);
            if (offers.isEmpty()) {
                messageLabel.setForeground(Theme.MUTED);
                messageLabel.setText(NO_FLIGHTS);
            }
        } catch (RuntimeException e) {
            // the table keeps what it showed before
            showMessage(ErrorMessages.forException(e));
        }
    }

    /** The line above the cards: the nearby days with their prices, how many flights, and how to order them. */
    private void buildResultsHead() {
        sorting.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, Space.S, 0));
        sorting.setOpaque(false);
        sorting.add(Theme.hint("Sort by"));
        javax.swing.ButtonGroup group = new javax.swing.ButtonGroup();
        FlightResultList.Sort[] sorts = FlightResultList.Sort.values();
        for (int i = 0; i < sorts.length; i++) {
            FlightResultList.Sort sort = sorts[i];
            sortChips[i] = Theme.chip(new JToggleButton(sort.label()));
            sortChips[i].setSelected(sort == FlightResultList.Sort.EARLIEST);
            sortChips[i].addActionListener(e -> results.sortBy(sort));
            group.add(sortChips[i]);
            sorting.add(sortChips[i]);
        }
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.add(caption, BorderLayout.WEST);
        row.add(sorting, BorderLayout.EAST);
        resultsHead.setOpaque(false);
        resultsHead.add(dayStrip, BorderLayout.NORTH);
        resultsHead.add(row, BorderLayout.SOUTH);
        resultsHead.setVisible(false);
    }

    private void showResultsHead(SearchCriteria criteria, List<FlightOffer> offers) {
        LocalDate today = controllers.flightSearchController().today();
        dayStrip.show(criteria.date(), today, day -> day.equals(criteria.date()) ? lowestPrice(offers)
                : lowestPrice(controllers.flightSearchController().search(new SearchCriteria(criteria.origin(),
                        criteria.destination(), day, criteria.airlineCode()))));
        caption.setText(offers.size() + (offers.size() == 1 ? " flight on " : " flights on ")
                + Format.date(criteria.date()));
        // there is nothing to put in order when there is one flight or none
        sorting.setVisible(offers.size() > 1);
        resultsHead.setVisible(true);
    }

    private static java.util.Optional<java.math.BigDecimal> lowestPrice(List<FlightOffer> offers) {
        return offers.stream().map(FlightOffer::fromPrice).min(java.util.Comparator.naturalOrder());
    }

    private void selectFlight() {
        FlightOffer chosen = results.selectedOffer();
        if (chosen == null) {
            return;
        }
        booking.select(chosen);
        navigator.show(ViewId.SEAT_SELECTION);
    }

    private void goBack() {
        if (booking.modifying().isPresent()) {
            booking.stopModifying();
            navigator.show(ViewId.MODIFY_RESERVATION);
            return;
        }
        Optional<User> user = controllers.session().currentUser();
        navigator.show(user.isPresent() ? ViewId.landingFor(user.get().role()) : ViewId.LOGIN);
    }

    private void showMessage(String text) {
        messageLabel.setForeground(ERROR);
        messageLabel.setText(text);
    }

    String message() {
        return messageLabel.getText().trim();
    }

    java.awt.Color messageColor() {
        return messageLabel.getForeground();
    }

    @Override
    public JButton defaultButton() {
        return searchButton;
    }

    @Override
    public int minimumHeight() {
        return getMinimumSize().height;
    }

    @Override
    public JComponent initialFocus() {
        return originCombo;
    }

    @Override
    public void onShow() {
        if (controllers.session().currentUser().isEmpty()) {
            navigator.show(ViewId.LOGIN);
            return;
        }
        // while an agent moves a reservation the booking is kept, it only picks a flight
        if (booking.modifying().isEmpty()) {
            booking.clear();
        }
        heading.setText(booking.modifying().isPresent() ? "Find the new flight" : "Find flights");
        List<Airport> airports = controllers.flightSearchController().airports();
        fill(originCombo, airports, 0);
        fill(destinationCombo, airports, 1);
        DefaultComboBoxModel<Airline> airlines = new DefaultComboBoxModel<>();
        airlines.addElement(null);
        controllers.flightSearchController().airlines().forEach(airlines::addElement);
        airlineCombo.setModel(airlines);
        LocalDate today = controllers.flightSearchController().today();
        dateField.setMinDate(today);
        dateField.setText(today.toString());
        results.setOffers(List.of());
        resultsHead.setVisible(false);
        messageLabel.setText(" ");
        // the flights of the starting route show at once, so a booking needs one click less
        if (startFromLastSearchOrFirstRoute(airports, today)) {
            search();
        }
    }

    /**
     * Starts the form where the user left it. The first time there is nothing to remember, so it picks the
     * first route that has a flight in the coming week, so the first search is not an empty one. It says
     * whether it found a route to start on.
     */
    private boolean startFromLastSearchOrFirstRoute(List<Airport> airports, LocalDate today) {
        Optional<BookingSession.SearchMemory> last = booking.lastSearch();
        if (last.isPresent()) {
            selectByCode(originCombo, last.get().origin());
            selectByCode(destinationCombo, last.get().destination());
            selectAirline(last.get().airline());
            // an earlier day is no use now, so the form starts today
            LocalDate remembered = last.get().date();
            dateField.setDate(remembered == null || remembered.isBefore(today) ? today : remembered);
            return true;
        }
        Optional<StartingRoute.Start> start = StartingRoute.find(controllers.flightSearchController(), airports, today);
        if (start.isPresent()) {
            originCombo.setSelectedItem(start.get().from());
            destinationCombo.setSelectedItem(start.get().to());
            dateField.setDate(start.get().date());
            return true;
        }
        return false;
    }

    private static void selectByCode(JComboBox<Airport> combo, String code) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).code().equals(code)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void selectAirline(String code) {
        for (int i = 0; i < airlineCombo.getItemCount(); i++) {
            Airline airline = airlineCombo.getItemAt(i);
            if (airline == null ? code == null : airline.code().equals(code)) {
                airlineCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void swapAirports() {
        Object origin = originCombo.getSelectedItem();
        originCombo.setSelectedItem(destinationCombo.getSelectedItem());
        destinationCombo.setSelectedItem(origin);
    }

    private static void fill(JComboBox<Airport> combo, List<Airport> airports, int selected) {
        combo.setModel(new DefaultComboBoxModel<>(airports.toArray(new Airport[0])));
        if (selected < airports.size()) {
            combo.setSelectedIndex(selected);
        }
    }
}
