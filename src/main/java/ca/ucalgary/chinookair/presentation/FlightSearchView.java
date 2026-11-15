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
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;
import javax.swing.JComponent;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

/**
 * The search screen: origin, destination, date and airline in, a table of flights out. It parses the
 * date and shows what the FlightSearchController answers; the rules about the input are the controller's.
 */
class FlightSearchView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final Color ERROR = Theme.DANGER;
    private static final int ROUTE_LOOKAHEAD_DAYS = 7;
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
    final FlightTableModel tableModel = new FlightTableModel();
    final HintTable table = new HintTable(tableModel, "Choose the airports and a date, then press Search.");
    private final JLabel heading = new JLabel("Find available flights");
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
        card.add(searchBar, BorderLayout.WEST);

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(heading, BorderLayout.NORTH);
        top.add(card, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.describe("Flights found");
        add(new TableArea(table), BorderLayout.CENTER);

        selectButton.setMnemonic('E');
        backButton.setMnemonic('B');
        ButtonRow buttons = ButtonRow.right();
        buttons.add(backButton);
        buttons.add(selectButton);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(messageLabel, BorderLayout.WEST);
        bottom.add(buttons, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        originCombo.setRenderer(new CodeRenderer());
        destinationCombo.setRenderer(new CodeRenderer());
        airlineCombo.setRenderer(new CodeRenderer());

        selectButton.setEnabled(false);
        table.getSelectionModel().addListSelectionListener(e -> selectButton.setEnabled(table.getSelectedRow() >= 0));
        searchButton.addActionListener(e -> search());
        swapButton.addActionListener(e -> swapAirports());
        dateField.addActionListener(e -> search());
        selectButton.addActionListener(e -> selectFlight());
        // a double click on a flight does what Select flight does
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    selectFlight();
                }
            }
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
            tableModel.setOffers(offers);
            table.setEmptyText("No flights match this search.");
            if (offers.isEmpty()) {
                messageLabel.setForeground(Theme.MUTED);
                messageLabel.setText(NO_FLIGHTS);
            }
        } catch (RuntimeException e) {
            // the table keeps what it showed before
            showMessage(ErrorMessages.forException(e));
        }
    }

    private void selectFlight() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        booking.select(tableModel.offerAt(table.convertRowIndexToModel(row)));
        navigator.show(ViewId.SEAT_SELECTION);
    }

    private void goBack() {
        if (booking.modifying().isPresent()) {
            booking.stopModifying();
            navigator.show(ViewId.MODIFY_RESERVATION);
            return;
        }
        Optional<User> user = controllers.session().currentUser();
        navigator.show(user.isPresent() ? ViewId.dashboardFor(user.get().role()) : ViewId.LOGIN);
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
        heading.setText(booking.modifying().isPresent() ? "Find the new flight" : "Find available flights");
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
        tableModel.setOffers(List.of());
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
        for (int day = 0; day < ROUTE_LOOKAHEAD_DAYS; day++) {
            for (Airport from : airports) {
                for (Airport to : airports) {
                    if (!from.equals(to) && !controllers.flightSearchController()
                            .search(new SearchCriteria(from.code(), to.code(), today.plusDays(day), null)).isEmpty()) {
                        originCombo.setSelectedItem(from);
                        destinationCombo.setSelectedItem(to);
                        dateField.setDate(today.plusDays(day));
                        return true;
                    }
                }
            }
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

    /** Shows "YYC - Calgary" for an airport, "AC - Air Canada" for an airline, "Any" for none. */
    private static final class CodeRenderer extends DefaultListCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                                                      boolean focus) {
            String text;
            if (value instanceof Airport) {
                Airport airport = (Airport) value;
                text = airport.code() + " - " + airport.city();
            } else if (value instanceof Airline) {
                Airline airline = (Airline) value;
                text = airline.code() + " - " + airline.name();
            } else {
                text = "Any";
            }
            return super.getListCellRendererComponent(list, text, index, selected, focus);
        }
    }
}
