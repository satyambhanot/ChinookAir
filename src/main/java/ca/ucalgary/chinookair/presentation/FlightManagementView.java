package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.control.FlightDetails;
import ca.ucalgary.chinookair.business.entity.Aircraft;
import ca.ucalgary.chinookair.business.entity.Airline;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Role;
import ca.ucalgary.chinookair.business.entity.Route;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/**
 * For the administrator: the flights of a period, and a form to add, change or remove a flight. The rules
 * (free aircraft, future departure, no active reservations) are the FlightManagementController's; the
 * screen shows its messages as they are.
 */
class FlightManagementView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final Color ERROR = new Color(0xB00020);
    private static final Color NOTICE = new Color(0x1B5E20);
    private static final int DEFAULT_DAYS = 30;
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final transient Controllers controllers;
    private final transient Navigator navigator;
    private transient Predicate<String> confirmer = this::askYesNo;

    final JTextField fromField = new JTextField(10);
    final JTextField toField = new JTextField(10);
    final JButton showButton = new JButton("Show");
    final ScheduleTableModel tableModel = new ScheduleTableModel();
    final JTable table = new JTable(tableModel);
    final JTextField numberField = new JTextField(12);
    final JComboBox<Route> routeCombo = new JComboBox<>();
    final JComboBox<Airline> airlineCombo = new JComboBox<>();
    final JComboBox<Aircraft> aircraftCombo = new JComboBox<>();
    final JTextField departureField = new JTextField(14);
    final JTextField arrivalField = new JTextField(14);
    final JTextField priceField = new JTextField(10);
    final JButton addButton = new JButton("Add");
    final JButton updateButton = new JButton("Update");
    final JButton removeButton = new JButton("Remove");
    final JButton backButton = new JButton("Back");
    private final JLabel messageLabel = new JLabel(" ");

    FlightManagementView(Controllers controllers, Navigator navigator) {
        super(new BorderLayout(10, 10));
        this.controllers = controllers;
        this.navigator = navigator;
        build();
    }

    private void build() {
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        JLabel heading = new JLabel("Flights");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 22f));
        JPanel range = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        range.add(new JLabel("From (yyyy-MM-dd)"));
        range.add(fromField);
        range.add(new JLabel("To (yyyy-MM-dd)"));
        range.add(toField);
        range.add(showButton);
        showButton.setMnemonic('S');
        JPanel top = new JPanel(new BorderLayout());
        top.add(heading, BorderLayout.NORTH);
        top.add(range, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildForm(), BorderLayout.EAST);

        addButton.setMnemonic('A');
        updateButton.setMnemonic('U');
        removeButton.setMnemonic('R');
        backButton.setMnemonic('B');
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(backButton);
        buttons.add(removeButton);
        buttons.add(updateButton);
        buttons.add(addButton);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(messageLabel, BorderLayout.WEST);
        bottom.add(buttons, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        routeCombo.setRenderer(new NameRenderer());
        airlineCombo.setRenderer(new NameRenderer());
        aircraftCombo.setRenderer(new NameRenderer());
        updateButtons();
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                rowChosen();
            }
        });
        showButton.addActionListener(e -> load());
        addButton.addActionListener(e -> add());
        updateButton.addActionListener(e -> update());
        removeButton.addActionListener(e -> remove());
        backButton.addActionListener(e -> navigator.show(ViewId.ADMIN_DASHBOARD));
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 6, 3, 6);
        c.anchor = GridBagConstraints.WEST;
        row(form, c, 0, "Flight number", 'N', numberField);
        row(form, c, 1, "Route", 'O', routeCombo);
        row(form, c, 2, "Airline", 'L', airlineCombo);
        row(form, c, 3, "Aircraft", 'C', aircraftCombo);
        row(form, c, 4, "Departure (yyyy-MM-dd HH:mm)", 'D', departureField);
        row(form, c, 5, "Arrival (yyyy-MM-dd HH:mm)", 'I', arrivalField);
        row(form, c, 6, "Base price", 'P', priceField);
        return form;
    }

    private static void row(JPanel form, GridBagConstraints c, int row, String text, char mnemonic,
                            Component field) {
        JLabel label = new JLabel(text);
        label.setDisplayedMnemonic(mnemonic);
        label.setLabelFor(field);
        c.gridx = 0;
        c.gridy = row;
        form.add(label, c);
        c.gridx = 1;
        form.add(field, c);
    }

    private void updateButtons() {
        boolean chosen = table.getSelectedRow() >= 0;
        updateButton.setEnabled(chosen);
        removeButton.setEnabled(chosen);
    }

    private Flight selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : tableModel.flightAt(table.convertRowIndexToModel(row));
    }

    private void rowChosen() {
        Flight flight = selected();
        if (flight != null) {
            numberField.setText(flight.flightNumber());
            routeCombo.setSelectedItem(find(routeCombo, r -> r.id().equals(flight.route().id())));
            airlineCombo.setSelectedItem(find(airlineCombo, a -> a.code().equals(flight.airline().code())));
            aircraftCombo.setSelectedItem(find(aircraftCombo, a -> a.id().equals(flight.aircraft().id())));
            departureField.setText(flight.departure().format(WHEN));
            arrivalField.setText(flight.arrival().format(WHEN));
            priceField.setText(flight.basePrice().toPlainString());
        }
        updateButtons();
    }

    private static <T> T find(JComboBox<T> combo, Predicate<T> match) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (match.test(combo.getItemAt(i))) {
                return combo.getItemAt(i);
            }
        }
        return null;
    }

    private FlightDetails details() {
        Route route = (Route) routeCombo.getSelectedItem();
        Airline airline = (Airline) airlineCombo.getSelectedItem();
        Aircraft aircraft = (Aircraft) aircraftCombo.getSelectedItem();
        if (route == null || airline == null || aircraft == null) {
            throw new IllegalArgumentException("Choose a route, an airline and an aircraft");
        }
        return new FlightDetails(numberField.getText(), route.id(), airline.code(), aircraft.id(),
                InputParser.dateTime(departureField.getText()), InputParser.dateTime(arrivalField.getText()),
                InputParser.money(priceField.getText(), "Base price"));
    }

    private void add() {
        User admin = controllers.session().currentUser().orElse(null);
        if (admin == null) {
            return;
        }
        try {
            Flight added = controllers.flightManagementController().addFlight(admin, details());
            load();
            showMessage("Added " + added.flightNumber() + ", departs " + added.departure().format(WHEN), NOTICE);
        } catch (RuntimeException e) {
            showMessage(ErrorMessages.forException(e), ERROR);
        }
    }

    private void update() {
        User admin = controllers.session().currentUser().orElse(null);
        Flight chosen = selected();
        if (admin == null || chosen == null) {
            return;
        }
        try {
            Flight changed = controllers.flightManagementController().updateFlight(admin, chosen.id(), details());
            load();
            showMessage("Updated " + changed.flightNumber(), NOTICE);
        } catch (RuntimeException e) {
            showMessage(ErrorMessages.forException(e), ERROR);
        }
    }

    private void remove() {
        User admin = controllers.session().currentUser().orElse(null);
        Flight chosen = selected();
        if (admin == null || chosen == null) {
            return;
        }
        if (!confirmer.test("Remove flight " + chosen.flightNumber() + "?")) {
            return;
        }
        try {
            controllers.flightManagementController().removeFlight(admin, chosen.id());
            load();
            clearForm();
            showMessage("Removed " + chosen.flightNumber(), NOTICE);
        } catch (RuntimeException e) {
            showMessage(ErrorMessages.forException(e), ERROR);
        }
    }

    /** Shows the flights of the typed period. */
    private void load() {
        User admin = controllers.session().currentUser().orElse(null);
        if (admin == null) {
            return;
        }
        try {
            LocalDate from = InputParser.date(fromField.getText());
            LocalDate to = InputParser.date(toField.getText());
            tableModel.setFlights(controllers.scheduleController().schedule(admin, from, to));
            updateButtons();
        } catch (RuntimeException e) {
            showMessage(ErrorMessages.forException(e), ERROR);
        }
    }

    private void clearForm() {
        numberField.setText("");
        departureField.setText("");
        arrivalField.setText("");
        priceField.setText("");
    }

    private boolean askYesNo(String question) {
        return JOptionPane.showConfirmDialog(this, question, "ChinookAir", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }

    private void showMessage(String text, Color color) {
        messageLabel.setForeground(color);
        messageLabel.setText(text);
    }

    private static <T> void fill(JComboBox<T> combo, List<T> items) {
        combo.setModel(new DefaultComboBoxModel<>(new Vector<>(items)));
    }

    @Override
    public void onShow() {
        Optional<User> user = controllers.session().currentUser();
        if (user.isEmpty() || user.get().role() != Role.ADMIN) {
            // only the administrator manages flights
            navigator.show(ViewId.LOGIN);
            return;
        }
        fill(routeCombo, controllers.routeController().list());
        fill(airlineCombo, controllers.flightSearchController().airlines());
        fill(aircraftCombo, controllers.aircraftController().list());
        clearForm();
        LocalDate today = controllers.flightSearchController().today();
        fromField.setText(today.toString());
        toField.setText(today.plusDays(DEFAULT_DAYS).toString());
        messageLabel.setText(" ");
        load();
    }

    // ---- for the tests

    String message() {
        return messageLabel.getText().trim();
    }

    /** Replaces the Yes/No question, so a test can answer it without a dialog. */
    void setConfirmer(Predicate<String> answer) {
        confirmer = answer;
    }

    /** Shows a route, an airline or an aircraft in words. */
    private static final class NameRenderer extends DefaultListCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                                                      boolean focus) {
            String text = "";
            if (value instanceof Route) {
                Route route = (Route) value;
                text = route.origin().code() + " → " + route.destination().code();
            } else if (value instanceof Airline) {
                Airline airline = (Airline) value;
                text = airline.code() + " - " + airline.name();
            } else if (value instanceof Aircraft) {
                Aircraft plane = (Aircraft) value;
                text = plane.model() + " (" + plane.rows() * plane.seatsPerRow() + " seats)";
            }
            return super.getListCellRendererComponent(list, text, index, selected, focus);
        }
    }
}
