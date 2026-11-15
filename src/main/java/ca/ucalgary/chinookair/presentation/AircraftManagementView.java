package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Role;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Optional;
import javax.swing.JComponent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * For the administrator: the aircraft there are with their seat counts, and a card to add one. While the
 * rows and seats per row are typed, the card shows how many seats that makes.
 */
class AircraftManagementView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final int CARD_WIDTH = 340;

    private final transient Controllers controllers;
    private final transient Navigator navigator;

    final JTextField modelField = Theme.placeholder(new JTextField(14), "For example Airbus A320");
    final JTextField rowsField = new JTextField(4);
    final JTextField seatsField = new JTextField(4);
    final JButton addButton = Theme.primary(new JButton("Add aircraft"));
    final AircraftTableModel tableModel = new AircraftTableModel();
    final HintTable table = new HintTable(tableModel, "No aircraft yet. Add one with the form.");
    private final FormField modelRow = new FormField("Model", modelField).mnemonic('M');
    private final FormField rowsRow = new FormField("Rows", rowsField).mnemonic('R');
    private final FormField seatsRow = new FormField("Seats per row", seatsField).mnemonic('S');
    private final JLabel total = Theme.hint(" ");
    private final Alert alert = new Alert(CARD_WIDTH - 90);
    private String shown = "";

    AircraftManagementView(Controllers controllers, Navigator navigator) {
        super(new BorderLayout(16, 12));
        this.controllers = controllers;
        this.navigator = navigator;
        setBorder(BorderFactory.createEmptyBorder(Theme.PAGE - 8, Theme.PAGE, Theme.PAGE - 8, Theme.PAGE));
        JLabel heading = new JLabel("Aircraft");
        Theme.heading(heading);
        add(heading, BorderLayout.NORTH);

        table.setFillsViewportHeight(true);
        table.describe("Aircraft");
        add(new JScrollPane(table), BorderLayout.CENTER);

        JLabel title = new JLabel("Add an aircraft");
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, Type.SECTION));
        JPanel fields = new JPanel();
        fields.setOpaque(false);
        fields.setLayout(new javax.swing.BoxLayout(fields, javax.swing.BoxLayout.Y_AXIS));
        for (FormField row : new FormField[] {modelRow, rowsRow, seatsRow}) {
            row.setAlignmentX(LEFT_ALIGNMENT);
            fields.add(row);
        }
        total.setAlignmentX(LEFT_ALIGNMENT);
        fields.add(total);
        addButton.setMnemonic('A');
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttons.setOpaque(false);
        buttons.add(addButton);
        JPanel south = new JPanel(new BorderLayout(0, 12));
        south.setOpaque(false);
        south.add(alert, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(new LineBorder(Theme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(20, 22, 16, 22)));
        card.add(title, BorderLayout.NORTH);
        card.add(fields, BorderLayout.CENTER);
        card.add(south, BorderLayout.SOUTH);
        JPanel side = new JPanel(new BorderLayout());
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(CARD_WIDTH, 10));
        side.add(card, BorderLayout.NORTH);
        add(side, BorderLayout.EAST);

        DocumentListener recount = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                showTotal();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                showTotal();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                showTotal();
            }
        };
        rowsField.getDocument().addDocumentListener(recount);
        seatsField.getDocument().addDocumentListener(recount);

        // Enter in any field presses Add aircraft
        modelField.addActionListener(e -> add());
        rowsField.addActionListener(e -> add());
        seatsField.addActionListener(e -> add());
        addButton.addActionListener(e -> add());
    }

    /** The number of seats of a plane with these rows and seats per row, or empty if either is not a number. */
    static Optional<Integer> seatCount(String rows, String seatsPerRow) {
        try {
            int r = Integer.parseInt(rows.trim());
            int s = Integer.parseInt(seatsPerRow.trim());
            return r > 0 && s > 0 ? Optional.of(r * s) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private void showTotal() {
        Optional<Integer> seats = seatCount(rowsField.getText(), seatsField.getText());
        total.setText(seats.map(n -> n + " seats in total").orElse(" "));
    }

    private void add() {
        User admin = controllers.session().currentUser().orElse(null);
        if (admin == null) {
            return;
        }
        clearErrors();
        try {
            int rows = InputParser.positiveInt(rowsField.getText(), "Rows");
            int seats = InputParser.positiveInt(seatsField.getText(), "Seats per row");
            controllers.aircraftController().addAircraft(admin, modelField.getText(), rows, seats);
            tableModel.setAircraft(controllers.aircraftController().list());
            alert.show(Alert.Kind.SUCCESS, "Added " + modelField.getText().trim() + " with " + rows * seats
                    + " seats");
            modelField.setText("");
            rowsField.setText("");
            seatsField.setText("");
        } catch (RuntimeException e) {
            showError(ErrorMessages.forException(e));
        }
    }

    /** Puts a problem under the field it is about. */
    private void showError(String text) {
        shown = text;
        String lower = text.toLowerCase();
        if (lower.contains("seats per row")) {
            seatsRow.showError(text);
        } else if (lower.contains("rows")) {
            rowsRow.showError(text);
        } else if (lower.contains("model")) {
            modelRow.showError(text);
        } else {
            alert.show(Alert.Kind.ERROR, text);
        }
    }

    private void clearErrors() {
        shown = "";
        alert.clear();
        modelRow.clearError();
        rowsRow.clearError();
        seatsRow.clearError();
    }

    @Override
    public JButton defaultButton() {
        return addButton;
    }

    @Override
    public JComponent initialFocus() {
        return modelField;
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
        clearErrors();
    }

    /** The last message shown, wherever it appears on the screen. */
    String message() {
        return shown.isEmpty() ? alert.text() : shown;
    }

    String totalText() {
        return total.getText().trim();
    }

    FormField rowsRow() {
        return rowsRow;
    }

    FormField seatsRow() {
        return seatsRow;
    }

    FormField modelRow() {
        return modelRow;
    }
}
