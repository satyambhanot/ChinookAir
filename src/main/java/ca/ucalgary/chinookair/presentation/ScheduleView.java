package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Role;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/**
 * The flight schedule for a period of days, for an agent or an administrator. It only reads; the
 * administrator's version adds a way to move a flight on top of this one.
 */
class ScheduleView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int DEFAULT_DAYS = 7;
    private static final int MONTH_DAYS = 30;

    final transient Controllers controllers;
    final transient Navigator navigator;
    final DatePickerField fromField;
    final DatePickerField toField;
    final JButton showButton = Theme.secondary(new JButton("Show"));
    final JButton todayButton = Theme.link(new JButton("Today"));
    final JButton weekButton = Theme.link(new JButton("Next 7 days"));
    final JButton monthButton = Theme.link(new JButton("Next 30 days"));
    final ScheduleTableModel tableModel = new ScheduleTableModel();
    final HintTable table = new HintTable(tableModel, "No flights in these dates.");
    // the administrator also moves flights; the agent does not see these
    final JTextField newDepartureField = Theme.placeholder(new JTextField(14), "yyyy-MM-dd HH:mm");
    final JTextField newArrivalField = Theme.placeholder(new JTextField(14), "yyyy-MM-dd HH:mm");
    final JButton rescheduleButton = Theme.primary(new JButton("Reschedule"));
    private final JPanel rescheduleControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    private final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    private final Alert alert = new Alert(700);

    ScheduleView(Controllers controllers, Navigator navigator) {
        super(new BorderLayout(0, 12));
        this.controllers = controllers;
        this.navigator = navigator;
        this.fromField = new DatePickerField(10, () -> controllers.flightSearchController().today());
        this.toField = new DatePickerField(10, () -> controllers.flightSearchController().today());
        setBorder(BorderFactory.createEmptyBorder(Theme.PAGE - 8, Theme.PAGE, Theme.PAGE - 8, Theme.PAGE));
        JLabel heading = new JLabel("Flight schedule");
        Theme.heading(heading);
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        form.setOpaque(false);
        form.add(new FormField("From", fromField).mnemonic('F'));
        form.add(new FormField("To", toField).mnemonic('T'));
        showButton.setMnemonic('S');
        JPanel showHolder = new JPanel(new BorderLayout());
        showHolder.setOpaque(false);
        showHolder.setBorder(BorderFactory.createEmptyBorder(22, 0, 0, 0));
        showHolder.add(showButton, BorderLayout.CENTER);
        form.add(showHolder);
        JPanel quick = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        quick.setOpaque(false);
        quick.setBorder(BorderFactory.createEmptyBorder(22, 8, 0, 0));
        quick.add(todayButton);
        quick.add(weekButton);
        quick.add(monthButton);
        form.add(quick);
        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(heading, BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        rescheduleControls.add(new JLabel("New departure"));
        rescheduleControls.add(newDepartureField);
        rescheduleControls.add(new JLabel("New arrival"));
        rescheduleControls.add(newArrivalField);
        rescheduleControls.add(rescheduleButton);
        rescheduleButton.setMnemonic('R');
        rescheduleControls.setVisible(false);
        buttons.add(rescheduleControls);
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
        if (flight != null) {
            newDepartureField.setText(flight.departure().format(WHEN));
            newArrivalField.setText(flight.arrival().format(WHEN));
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
    public void onShow() {
        if (controllers.session().currentUser().isEmpty()) {
            navigator.show(ViewId.LOGIN);
            return;
        }
        boolean admin = controllers.session().currentUser().get().role() == Role.ADMIN;
        rescheduleControls.setVisible(admin);
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
}
