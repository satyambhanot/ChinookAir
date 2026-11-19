package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Role;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

/**
 * The flight schedule for a period of days, for an agent or an administrator. It only reads; the
 * administrator's version adds a way to move a flight on top of this one.
 */
class ScheduleView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_DAYS = 7;
    private static final int MONTH_DAYS = 30;

    final transient Controllers controllers;
    final transient Navigator navigator;
    final DatePickerField fromField;
    final DatePickerField toField;
    final JButton showButton = Theme.secondary(new JButton("Show"));
    FilterBar filterBar;
    final JButton todayButton = Theme.link(new JButton("Today"));
    final JButton weekButton = Theme.link(new JButton("Next 7 days"));
    final JButton monthButton = Theme.link(new JButton("Next 30 days"));
    final ScheduleTableModel tableModel = new ScheduleTableModel();
    final HintTable table = new HintTable(new DayColumns(tableModel), "No flights in these dates.");
    // the administrator also moves flights; the agent does not see these
    final DateTimeField newDepartureField;
    final DateTimeField newArrivalField;
    final JButton rescheduleButton = Theme.primary(new JButton("Reschedule"));
    private final JPanel rescheduleControls = new JPanel();
    private final JPanel buttons = new JPanel();
    private boolean administrator;
    private final Alert alert = new Alert(700);

    ScheduleView(Controllers controllers, Navigator navigator) {
        super(new BorderLayout(0, 12));
        this.controllers = controllers;
        this.navigator = navigator;
        this.fromField = new DatePickerField(10, () -> controllers.flightSearchController().today());
        this.toField = new DatePickerField(10, () -> controllers.flightSearchController().today());
        this.newDepartureField = new DateTimeField(() -> controllers.flightSearchController().today());
        this.newArrivalField = new DateTimeField(() -> controllers.flightSearchController().today());
        setBorder(BorderFactory.createEmptyBorder(Space.XL, Space.PAGE, Space.XL, Space.PAGE));
        JLabel heading = new JLabel("Schedule");
        Theme.heading(heading);
        showButton.setMnemonic('S');
        filterBar = new FilterBar()
                .field("From", fromField, 'F')
                .field("To", toField, 'T')
                .action(showButton)
                .action(todayButton)
                .action(weekButton)
                .action(monthButton);
        JPanel form = filterBar;
        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(heading, BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.describe("Flight schedule");
        // the day is written once, on the first flight of the day, in bold
        table.getColumnModel().getColumn(0).setCellRenderer(HintTable.boldRenderer());
        add(new TableArea(table), BorderLayout.CENTER);

        // the administrator moves the chosen flight here; the card shows only for a chosen flight
        JLabel moveTitle = new JLabel("Move the chosen flight");
        moveTitle.setFont(moveTitle.getFont().deriveFont(java.awt.Font.BOLD, Type.SECTION));
        rescheduleButton.setMnemonic('R');
        JPanel moveRow = new FilterBar()
                .field("New departure", newDepartureField, 'D')
                .field("New arrival", newArrivalField, 'A')
                .action(rescheduleButton);
        rescheduleControls.setLayout(new BorderLayout(0, 8));
        rescheduleControls.setBackground(Theme.SURFACE);
        SurfaceBorder.apply(rescheduleControls, Space.M, Space.L, Space.S, Space.L);
        rescheduleControls.add(moveTitle, BorderLayout.NORTH);
        rescheduleControls.add(moveRow, BorderLayout.CENTER);
        rescheduleControls.setVisible(false);
        buttons.setLayout(new BorderLayout());
        buttons.setOpaque(false);
        buttons.add(rescheduleControls, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);
        bottom.add(alert, BorderLayout.NORTH);
        bottom.add(buttons, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        showButton.addActionListener(e -> load());
        fromField.addActionListener(e -> load());
        toField.addActionListener(e -> load());
        todayButton.addActionListener(e -> showDays(0));
        weekButton.addActionListener(e -> showDays(DEFAULT_DAYS));
        monthButton.addActionListener(e -> showDays(MONTH_DAYS));
        rescheduleButton.setEnabled(false);
        table.getSelectionModel().addListSelectionListener(e -> flightChosen());
        rescheduleButton.addActionListener(e -> reschedule());
    }

    private void flightChosen() {
        Flight flight = selectedFlight();
        rescheduleButton.setEnabled(flight != null);
        rescheduleControls.setVisible(administrator && flight != null);
        if (flight != null) {
            newDepartureField.setText(Format.input(flight.departure()));
            newArrivalField.setText(Format.input(flight.arrival()));
        }
    }

    /** The administrator moves the chosen flight to the typed times. */
    private void reschedule() {
        User admin = controllers.session().currentUser().orElse(null);
        Flight flight = selectedFlight();
        if (admin == null || flight == null) {
            return;
        }
        try {
            LocalDateTime departure = InputParser.dateTime(newDepartureField.getText());
            LocalDateTime arrival = InputParser.dateTime(newArrivalField.getText());
            controllers.scheduleController().reschedule(admin, flight.id(), departure, arrival);
            load();
            alert.show(Alert.Kind.SUCCESS, "Rescheduled " + flight.flightNumber());
        } catch (RuntimeException e) {
            alert.show(Alert.Kind.ERROR, ErrorMessages.forException(e));
        }
    }

    /** Shows the days from today to this many days ahead; 0 means today only. */
    private void showDays(int ahead) {
        LocalDate today = controllers.flightSearchController().today();
        fromField.setDate(today);
        toField.setDate(today.plusDays(ahead));
        load();
    }

    /** Shows the flights of the typed period. */
    void load() {
        User user = controllers.session().currentUser().orElse(null);
        if (user == null) {
            return;
        }
        try {
            LocalDate from = InputParser.date(fromField.getText());
            LocalDate to = InputParser.date(toField.getText());
            List<Flight> flights = controllers.scheduleController().schedule(user, from, to);
            tableModel.setFlights(flights);
            alert.clear();
        } catch (RuntimeException e) {
            // the table keeps what it showed before
            alert.show(Alert.Kind.ERROR, ErrorMessages.forException(e));
        }
    }

    /** The flight in the chosen row, if any. */
    Flight selectedFlight() {
        int row = table.getSelectedRow();
        return row < 0 ? null : tableModel.flightAt(table.convertRowIndexToModel(row));
    }

    @Override
    public JButton defaultButton() {
        return showButton;
    }

    @Override
    public JComponent initialFocus() {
        return fromField;
    }

    @Override
    public void onShow() {
        if (controllers.session().currentUser().isEmpty()) {
            navigator.show(ViewId.LOGIN);
            return;
        }
        administrator = controllers.session().currentUser().get().role() == Role.ADMIN;
        rescheduleControls.setVisible(false);
        alert.clear();
        newDepartureField.setText("");
        newArrivalField.setText("");
        LocalDate today = controllers.flightSearchController().today();
        fromField.setText(today.toString());
        toField.setText(today.plusDays(DEFAULT_DAYS).toString());
        load();
    }

    String message() {
        return alert.text();
    }

    /** True while the card for moving a flight shows: an administrator with a flight chosen. */
    boolean rescheduleShown() {
        return rescheduleControls.isVisible();
    }
}
