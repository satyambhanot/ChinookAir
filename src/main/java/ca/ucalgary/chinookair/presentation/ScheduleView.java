package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/**
 * The flight schedule for a period of days, for an agent or an administrator. It only reads; the
 * administrator's version adds a way to move a flight on top of this one.
 */
class ScheduleView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final Color ERROR = new Color(0xB00020);
    private static final int DEFAULT_DAYS = 7;

    final transient Controllers controllers;
    final transient Navigator navigator;
    final JTextField fromField = new JTextField(10);
    final JTextField toField = new JTextField(10);
    final JButton showButton = new JButton("Show");
    final JButton backButton = new JButton("Back");
    final ScheduleTableModel tableModel = new ScheduleTableModel();
    final JTable table = new JTable(tableModel);
    /** Room beside Back for buttons that a subclass adds. */
    final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    private final JLabel messageLabel = new JLabel(" ");

    ScheduleView(Controllers controllers, Navigator navigator) {
        super(new BorderLayout(0, 10));
        this.controllers = controllers;
        this.navigator = navigator;
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        JLabel heading = new JLabel("Flight schedule");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 22f));
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        form.add(label("From (yyyy-MM-dd)", 'F', fromField));
        form.add(fromField);
        form.add(label("To (yyyy-MM-dd)", 'T', toField));
        form.add(toField);
        form.add(showButton);
        showButton.setMnemonic('S');
        JPanel top = new JPanel(new BorderLayout());
        top.add(heading, BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        backButton.setMnemonic('B');
        buttons.add(backButton);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(messageLabel, BorderLayout.WEST);
        bottom.add(buttons, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        showButton.addActionListener(e -> load());
        fromField.addActionListener(e -> load());
        toField.addActionListener(e -> load());
        backButton.addActionListener(e -> goBack());
    }

    private static JLabel label(String text, char mnemonic, JTextField field) {
        JLabel label = new JLabel(text);
        label.setDisplayedMnemonic(mnemonic);
        label.setLabelFor(field);
        return label;
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
            showMessage(flights.isEmpty() ? "No flights in this period." : " ", Color.DARK_GRAY);
        } catch (RuntimeException e) {
            // the table keeps what it showed before
            showMessage(ErrorMessages.forException(e), ERROR);
        }
    }

    /** The flight in the chosen row, if any. */
    Flight selectedFlight() {
        int row = table.getSelectedRow();
        return row < 0 ? null : tableModel.flightAt(table.convertRowIndexToModel(row));
    }

    void showMessage(String text, Color color) {
        messageLabel.setForeground(color);
        messageLabel.setText(text);
    }

    private void goBack() {
        Optional<User> user = controllers.session().currentUser();
        navigator.show(user.isPresent() ? ViewId.dashboardFor(user.get().role()) : ViewId.LOGIN);
    }

    @Override
    public void onShow() {
        if (controllers.session().currentUser().isEmpty()) {
            navigator.show(ViewId.LOGIN);
            return;
        }
        LocalDate today = controllers.flightSearchController().today();
        fromField.setText(today.toString());
        toField.setText(today.plusDays(DEFAULT_DAYS).toString());
        load();
    }

    String message() {
        return messageLabel.getText().trim();
    }
}
