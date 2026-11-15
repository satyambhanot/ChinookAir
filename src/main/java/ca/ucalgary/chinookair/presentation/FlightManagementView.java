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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.function.Predicate;
import javax.swing.JComponent;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.ListSelectionModel;

/**
 * For the administrator: the flights of a period, which can be sorted and filtered, and a card to add,
 * change or remove a flight. The card shows when a flight is chosen or Add flight is pressed, so the
 * table has the whole width the rest of the time. The rules (free aircraft, future departure, no active
 * reservations) are the FlightManagementController's; the screen shows its messages as they are.
 */
class FlightManagementView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_DAYS = 30;
    private static final int CARD_WIDTH = 360;
    private static final int ARRIVES_COLUMN = 3;
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final transient Controllers controllers;
    private final transient Navigator navigator;
    private transient Predicate<String> confirmer = this::askYesNo;

    final DatePickerField fromField;
    final DatePickerField toField;
    final JButton showButton = Theme.secondary(new JButton("Show"));
    final SearchField filterField = new SearchField("Filter by flight or route", 20);
    final ScheduleTableModel tableModel = new ScheduleTableModel();
    final HintTable table = new HintTable(tableModel, "No flights in these dates.");
    final JTextField numberField = new JTextField(12);
    final JComboBox<Route> routeCombo = new JComboBox<>();
    final JComboBox<Airline> airlineCombo = new JComboBox<>();
    final JComboBox<Aircraft> aircraftCombo = new JComboBox<>();
    final JTextField departureField = Theme.placeholder(new JTextField(14), "yyyy-MM-dd HH:mm");
    final JTextField arrivalField = Theme.placeholder(new JTextField(14), "yyyy-MM-dd HH:mm");
    final JTextField priceField = new JTextField(10);
    final JButton newButton = Theme.secondary(new JButton("Add flight"));
    final JButton addButton = Theme.primary(new JButton("Add"));
    final JButton updateButton = Theme.secondary(new JButton("Update"));
    final JButton removeButton = Theme.danger(new JButton("Remove"));
    private final FormField numberRow = new FormField("Flight number", numberField).mnemonic('N');
    private final FormField aircraftRow = new FormField("Aircraft", aircraftCombo).mnemonic('C');
    private final FormField priceRow = new FormField("Base price", priceField).mnemonic('P');
    private final Alert alert = new Alert(CARD_WIDTH - 90);
    private final JLabel cardTitle = new JLabel("Flight details");
    private final JPanel card = new JPanel(new BorderLayout(0, 12));
    private String shown = "";

    FlightManagementView(Controllers controllers, Navigator navigator) {
        super(new BorderLayout(16, 12));
        this.controllers = controllers;
        this.navigator = navigator;
        this.fromField = new DatePickerField(10, () -> controllers.flightSearchController().today());
        this.toField = new DatePickerField(10, () -> controllers.flightSearchController().today());
        build();
    }

    private void build() {
        setBorder(BorderFactory.createEmptyBorder(Theme.PAGE - 8, Theme.PAGE, Theme.PAGE - 8, Theme.PAGE));
        JLabel heading = new JLabel("Flights");
        Theme.heading(heading);
        newButton.setMnemonic('W');
        JPanel title = new JPanel(new BorderLayout());
        title.setOpaque(false);
        title.add(heading, BorderLayout.WEST);
        title.add(newButton, BorderLayout.EAST);

        showButton.setMnemonic('S');
        JPanel range = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        range.setOpaque(false);
        range.add(new FormField("From", fromField).mnemonic('F'));
        range.add(new FormField("To", toField).mnemonic('T'));
        JPanel showHolder = new JPanel(new BorderLayout());
        showHolder.setOpaque(false);
        showHolder.setBorder(BorderFactory.createEmptyBorder(22, 0, 0, 0));
        showHolder.add(showButton, BorderLayout.CENTER);
        range.add(showHolder);
        JPanel filter = new JPanel(new BorderLayout());
        filter.setOpaque(false);
        filter.setBorder(BorderFactory.createEmptyBorder(22, 0, 0, 0));
        filter.add(filterField, BorderLayout.NORTH);
        JPanel controls = new JPanel(new BorderLayout(12, 0));
        controls.setOpaque(false);
        controls.add(range, BorderLayout.WEST);
        controls.add(filter, BorderLayout.CENTER);
        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(title, BorderLayout.NORTH);
        top.add(controls, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.describe("Flights");
        // a click on a heading sorts by it
        table.setAutoCreateRowSorter(true);
        // the arrival time is in the card, and the table needs the room beside it
        table.removeColumn(table.getColumnModel().getColumn(ARRIVES_COLUMN));
        add(new JScrollPane(table), BorderLayout.CENTER);

        addButton.setMnemonic('A');
        updateButton.setMnemonic('U');
        removeButton.setMnemonic('R');
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(removeButton);
        buttons.add(updateButton);
        buttons.add(addButton);
        JPanel south = new JPanel(new BorderLayout(0, 12));
        south.setOpaque(false);
        south.add(alert, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);

        cardTitle.setFont(cardTitle.getFont().deriveFont(java.awt.Font.BOLD, Type.SECTION));
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(new LineBorder(Theme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(20, 22, 16, 22)));
        card.setPreferredSize(new Dimension(CARD_WIDTH, 10));
        card.add(cardTitle, BorderLayout.NORTH);
        card.add(buildForm(), BorderLayout.CENTER);
        card.add(south, BorderLayout.SOUTH);
        card.setVisible(false);
        add(card, BorderLayout.EAST);

        routeCombo.setRenderer(new NameRenderer());
        airlineCombo.setRenderer(new NameRenderer());
        aircraftCombo.setRenderer(new NameRenderer());
        updateButtons();
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                rowChosen();
            }
        });
        filterField.onChange(text -> applyFilter());
        showButton.addActionListener(e -> load());
        fromField.addActionListener(e -> load());
        toField.addActionListener(e -> load());
        newButton.addActionListener(e -> startNew());
        addButton.addActionListener(e -> add());
        updateButton.addActionListener(e -> update());
        removeButton.addActionListener(e -> remove());
    }

    private JScrollPane buildForm() {
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new javax.swing.BoxLayout(form, javax.swing.BoxLayout.Y_AXIS));
        form.add(pair(numberRow, priceRow));
        form.add(single(new FormField("Route", routeCombo).mnemonic('O')));
        form.add(single(new FormField("Airline", airlineCombo).mnemonic('L')));
        form.add(single(aircraftRow));
        form.add(pair(new FormField("Departure", departureField).mnemonic('D'),
                new FormField("Arrival", arrivalField).mnemonic('I')));
        // a short window scrolls the form; the buttons below it stay in view
        return ScrollableForm.around(form);
    }

    private static JPanel single(FormField row) {
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.setAlignmentX(LEFT_ALIGNMENT);
        holder.add(row, BorderLayout.CENTER);
        return holder;
    }

    /** Two short fields side by side, to save height. */
    private static JPanel pair(FormField left, FormField right) {
        JPanel holder = new JPanel(new java.awt.GridLayout(1, 2, 12, 0));
        holder.setOpaque(false);
        holder.setAlignmentX(LEFT_ALIGNMENT);
        holder.add(left);
        holder.add(right);
        return holder;
    }

    /** Opens the card with an empty form, ready for a new flight. */
    private void startNew() {
        table.clearSelection();
        clearForm();
        clearErrors();
        card.setVisible(true);
        updateButtons();
    }

    private void applyFilter() {
        // the flight number is column 0 and the route column 1
        table.filterRows(filterField.getText(), 0, 1);
        boolean filtered = !filterField.getText().isBlank();
        table.setEmptyText(filtered && tableModel.getRowCount() > 0
                ? "No flights match \u201C" + filterField.getText().trim() + "\u201D."
                : "No flights in these dates.");
    }

    private void updateButtons() {
        boolean chosen = table.getSelectedRow() >= 0;
        updateButton.setEnabled(chosen);
        removeButton.setEnabled(chosen);
        // the main action of the card is Update for a chosen flight and Add for a new one
        addButton.putClientProperty("FlatLaf.styleClass", chosen ? "secondary" : "primary");
        updateButton.putClientProperty("FlatLaf.styleClass", chosen ? "primary" : "secondary");
        addButton.setText(chosen ? "Duplicate" : "Add");
        cardTitle.setText(chosen ? "Flight " + selected().flightNumber() : "New flight");
    }

    private Flight selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : tableModel.flightAt(table.convertRowIndexToModel(row));
    }

    private void rowChosen() {
        Flight flight = selected();
        if (flight != null) {
            clearErrors();
            numberField.setText(flight.flightNumber());
            routeCombo.setSelectedItem(find(routeCombo, r -> r.id().equals(flight.route().id())));
            airlineCombo.setSelectedItem(find(airlineCombo, a -> a.code().equals(flight.airline().code())));
            aircraftCombo.setSelectedItem(find(aircraftCombo, a -> a.id().equals(flight.aircraft().id())));
            departureField.setText(flight.departure().format(WHEN));
            arrivalField.setText(flight.arrival().format(WHEN));
            priceField.setText(flight.basePrice().toPlainString());
            card.setVisible(true);
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
            clearErrors();
            Flight added = controllers.flightManagementController().addFlight(admin, details());
            load();
            alert.show(Alert.Kind.SUCCESS, "Added " + added.flightNumber() + ", departs "
                    + added.departure().format(WHEN));
        } catch (RuntimeException e) {
            showError(ErrorMessages.forException(e));
        }
    }

    private void update() {
        User admin = controllers.session().currentUser().orElse(null);
        Flight chosen = selected();
        if (admin == null || chosen == null) {
            return;
        }
        try {
            clearErrors();
            Flight changed = controllers.flightManagementController().updateFlight(admin, chosen.id(), details());
            load();
            alert.show(Alert.Kind.SUCCESS, "Updated " + changed.flightNumber());
        } catch (RuntimeException e) {
            showError(ErrorMessages.forException(e));
        }
    }

    private void remove() {
        User admin = controllers.session().currentUser().orElse(null);
        Flight chosen = selected();
        if (admin == null || chosen == null) {
            return;
        }
        if (!confirmer.test("Flight " + chosen.flightNumber() + " will be taken off the schedule. "
                + "A flight with reservations cannot be removed.")) {
            return;
        }
        try {
            clearErrors();
            controllers.flightManagementController().removeFlight(admin, chosen.id());
            load();
            clearForm();
            alert.show(Alert.Kind.SUCCESS, "Removed " + chosen.flightNumber());
        } catch (RuntimeException e) {
            showError(ErrorMessages.forException(e));
        }
    }

    /** Shows the flights of the chosen period. */
    private void load() {
        User admin = controllers.session().currentUser().orElse(null);
        if (admin == null) {
            return;
        }
        try {
            LocalDate from = InputParser.date(fromField.getText());
            LocalDate to = InputParser.date(toField.getText());
            tableModel.setFlights(controllers.scheduleController().schedule(admin, from, to));
            applyFilter();
            updateButtons();
        } catch (RuntimeException e) {
            showError(ErrorMessages.forException(e));
        }
    }

    private void clearForm() {
        numberField.setText("");
        departureField.setText("");
        arrivalField.setText("");
        priceField.setText("");
    }

    /** Puts a problem under the field it is about, or in the strip when it is about the whole flight. */
    private void showError(String text) {
        shown = text;
        String lower = text.toLowerCase();
        if (lower.contains("flight number")) {
            numberRow.showError(text);
        } else if (lower.contains("aircraft")) {
            aircraftRow.showError(text);
        } else if (lower.contains("price")) {
            priceRow.showError(text);
        } else {
            alert.show(Alert.Kind.ERROR, text);
        }
    }

    private void clearErrors() {
        shown = "";
        alert.clear();
        numberRow.clearError();
        aircraftRow.clearError();
        priceRow.clearError();
    }

    private boolean askYesNo(String question) {
        return ConfirmDialog.ask(this, "Remove this flight?", question, "Remove flight", true);
    }

    private static <T> void fill(JComboBox<T> combo, List<T> items) {
        combo.setModel(new DefaultComboBoxModel<>(new Vector<>(items)));
    }

    @Override
    public JComponent initialFocus() {
        return filterField;
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
        clearErrors();
        filterField.setText("");
        card.setVisible(false);
        LocalDate today = controllers.flightSearchController().today();
        fromField.setText(today.toString());
        toField.setText(today.plusDays(DEFAULT_DAYS).toString());
        load();
    }

    // ---- for the tests

    /** The last message shown, wherever it appears on the screen. */
    String message() {
        return shown.isEmpty() ? alert.text() : shown;
    }

    boolean cardShown() {
        return card.isVisible();
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
