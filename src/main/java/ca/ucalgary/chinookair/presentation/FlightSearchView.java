package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.control.FlightOffer;
import ca.ucalgary.chinookair.business.control.SearchCriteria;
import ca.ucalgary.chinookair.business.entity.Airline;
import ca.ucalgary.chinookair.business.entity.Airport;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/**
 * The search screen: origin, destination, date and airline in, a table of flights out. It parses the
 * date and shows what the FlightSearchController answers; the rules about the input are the controller's.
 */
class FlightSearchView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final Color ERROR = new Color(0xB00020);
    private static final String NO_FLIGHTS = "No flights found. Try another date or airline.";

    private final transient Controllers controllers;
    private final transient Navigator navigator;
    private final transient BookingSession booking;

    final JComboBox<Airport> originCombo = new JComboBox<>();
    final JComboBox<Airport> destinationCombo = new JComboBox<>();
    final JTextField dateField = new JTextField(10);
    final JComboBox<Airline> airlineCombo = new JComboBox<>();
    final JButton searchButton = new JButton("Search");
    final JButton selectButton = new JButton("Select flight");
    final JButton backButton = new JButton("Back");
    final FlightTableModel tableModel = new FlightTableModel();
    final JTable table = new JTable(tableModel);
    private final JLabel heading = new JLabel("Find available flights");
    private final JLabel messageLabel = new JLabel(" ");

    FlightSearchView(Controllers controllers, Navigator navigator, BookingSession booking) {
        super(new BorderLayout(0, 10));
        this.controllers = controllers;
        this.navigator = navigator;
        this.booking = booking;
        build();
    }

    private void build() {
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 22f));
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        form.add(label("From", 'F', originCombo));
        form.add(originCombo);
        form.add(label("To", 'T', destinationCombo));
        form.add(destinationCombo);
        form.add(label("Date (yyyy-MM-dd)", 'D', dateField));
        form.add(dateField);
        form.add(label("Airline", 'A', airlineCombo));
        form.add(airlineCombo);
        form.add(searchButton);
        searchButton.setMnemonic('S');
        JPanel top = new JPanel(new BorderLayout());
        top.add(heading, BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        selectButton.setMnemonic('E');
        backButton.setMnemonic('B');
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
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
        dateField.addActionListener(e -> search());
        selectButton.addActionListener(e -> selectFlight());
        backButton.addActionListener(e -> goBack());
    }

    private static JLabel label(String text, char mnemonic, Component field) {
        JLabel label = new JLabel(text);
        label.setDisplayedMnemonic(mnemonic);
        label.setLabelFor(field);
        return label;
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
            tableModel.setOffers(offers);
            if (offers.isEmpty()) {
                showMessage(NO_FLIGHTS);
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
        fill(originCombo, controllers.flightSearchController().airports(), 0);
        fill(destinationCombo, controllers.flightSearchController().airports(), 1);
        DefaultComboBoxModel<Airline> airlines = new DefaultComboBoxModel<>();
        airlines.addElement(null);
        controllers.flightSearchController().airlines().forEach(airlines::addElement);
        airlineCombo.setModel(airlines);
        dateField.setText(controllers.flightSearchController().today().toString());
        tableModel.setOffers(List.of());
        messageLabel.setText(" ");
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
