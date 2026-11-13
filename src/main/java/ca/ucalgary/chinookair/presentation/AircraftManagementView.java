package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Role;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

/** For the administrator: the aircraft there are with their seat counts, and a way to add one. */
class AircraftManagementView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final Color ERROR = new Color(0xB00020);
    private static final Color NOTICE = new Color(0x1B5E20);

    private final transient Controllers controllers;
    private final transient Navigator navigator;

    final JTextField modelField = new JTextField(14);
    final JTextField rowsField = new JTextField(4);
    final JTextField seatsField = new JTextField(4);
    final JButton addButton = new JButton("Add aircraft");
    final JButton backButton = new JButton("Back");
    final AircraftTableModel tableModel = new AircraftTableModel();
    final JTable table = new JTable(tableModel);
    private final JLabel messageLabel = new JLabel(" ");

    AircraftManagementView(Controllers controllers, Navigator navigator) {
        super(new BorderLayout(0, 10));
        this.controllers = controllers;
        this.navigator = navigator;
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        JLabel heading = new JLabel("Aircraft");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 22f));
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        form.add(label("Model", 'M', modelField));
        form.add(modelField);
        form.add(label("Rows", 'R', rowsField));
        form.add(rowsField);
        form.add(label("Seats per row", 'S', seatsField));
        form.add(seatsField);
        form.add(addButton);
        addButton.setMnemonic('A');
        JPanel top = new JPanel(new BorderLayout());
        top.add(heading, BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        table.setFillsViewportHeight(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        backButton.setMnemonic('B');
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(backButton);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(messageLabel, BorderLayout.WEST);
        bottom.add(buttons, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        // Enter in any field presses Add aircraft
        modelField.addActionListener(e -> add());
        rowsField.addActionListener(e -> add());
        seatsField.addActionListener(e -> add());
        addButton.addActionListener(e -> add());
        backButton.addActionListener(e -> navigator.show(ViewId.ADMIN_DASHBOARD));
    }

    private static JLabel label(String text, char mnemonic, Component field) {
        JLabel label = new JLabel(text);
        label.setDisplayedMnemonic(mnemonic);
        label.setLabelFor(field);
        return label;
    }

    private void add() {
        User admin = controllers.session().currentUser().orElse(null);
        if (admin == null) {
            return;
        }
        try {
            int rows = InputParser.positiveInt(rowsField.getText(), "Rows");
            int seats = InputParser.positiveInt(seatsField.getText(), "Seats per row");
            controllers.aircraftController().addAircraft(admin, modelField.getText(), rows, seats);
            tableModel.setAircraft(controllers.aircraftController().list());
            showMessage("Added " + modelField.getText().trim() + " with " + rows * seats + " seats", NOTICE);
            modelField.setText("");
            rowsField.setText("");
            seatsField.setText("");
        } catch (RuntimeException e) {
            showMessage(ErrorMessages.forException(e), ERROR);
        }
    }

    private void showMessage(String text, Color color) {
        messageLabel.setForeground(color);
        messageLabel.setText(text);
    }

    @Override
    public void onShow() {
        Optional<User> user = controllers.session().currentUser();
        if (user.isEmpty() || user.get().role() != Role.ADMIN) {
            navigator.show(ViewId.LOGIN);
            return;
        }
        tableModel.setAircraft(controllers.aircraftController().list());
        modelField.setText("");
        rowsField.setText("");
        seatsField.setText("");
        messageLabel.setText(" ");
    }

    String message() {
        return messageLabel.getText().trim();
    }
}
