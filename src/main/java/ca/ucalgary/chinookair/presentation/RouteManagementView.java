package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Airport;
import ca.ucalgary.chinookair.business.entity.Role;
import ca.ucalgary.chinookair.business.entity.Route;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
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

/** For the administrator: the routes there are, and a card to add one between two airports. */
class RouteManagementView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final int CARD_WIDTH = 340;

    private final transient Controllers controllers;
    private final transient Navigator navigator;

    final JComboBox<Airport> originCombo = new JComboBox<>();
    final JComboBox<Airport> destinationCombo = new JComboBox<>();
    final JButton addButton = Theme.primary(new JButton("Add route"));
    final JButton removeButton = Theme.danger(new JButton("Remove route"));
    final RouteTableModel tableModel = new RouteTableModel();
    final HintTable table = new HintTable(tableModel, "No routes yet. Add one with the form.");
    private final Alert alert = new Alert(CARD_WIDTH - 90);

    RouteManagementView(Controllers controllers, Navigator navigator) {
        super(new BorderLayout(16, 12));
        this.controllers = controllers;
        this.navigator = navigator;
        setBorder(BorderFactory.createEmptyBorder(Space.XL, Space.PAGE, Space.XL, Space.PAGE));
        JLabel heading = new JLabel("Routes");
        Theme.heading(heading);
        add(heading, BorderLayout.NORTH);

        table.setFillsViewportHeight(true);
        table.describe("Routes");
        // each starting airport is named once, over the routes that leave from it
        table.getColumnModel().getColumn(0).setCellRenderer(HintTable.repeatBlankRenderer());
        add(new JScrollPane(table), BorderLayout.CENTER);

        JLabel title = new JLabel("Add a route");
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, Type.SECTION));
        JPanel fields = new JPanel();
        fields.setOpaque(false);
        fields.setLayout(new javax.swing.BoxLayout(fields, javax.swing.BoxLayout.Y_AXIS));
        for (FormField row : new FormField[] {new FormField("From", originCombo).mnemonic('F'),
                new FormField("To", destinationCombo).mnemonic('T')}) {
            row.setAlignmentX(LEFT_ALIGNMENT);
            fields.add(row);
        }
        addButton.setMnemonic('A');
        removeButton.setMnemonic('R');
        removeButton.setEnabled(false);
        ButtonRow buttons = ButtonRow.right();
        buttons.setOpaque(false);
        buttons.add(removeButton);
        buttons.add(addButton);
        JPanel south = new JPanel(new BorderLayout(0, 12));
        south.setOpaque(false);
        south.add(alert, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Theme.SURFACE);
        SurfaceBorder.apply(card, Space.XL, Space.XL, Space.L, Space.XL);
        card.add(title, BorderLayout.NORTH);
        card.add(fields, BorderLayout.CENTER);
        card.add(south, BorderLayout.SOUTH);
        JPanel side = new JPanel(new BorderLayout());
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(CARD_WIDTH, 10));
        side.add(card, BorderLayout.NORTH);
        add(side, BorderLayout.EAST);

        DefaultListCellRenderer renderer = new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                String text = value instanceof Airport ? ((Airport) value).code() + " - " + ((Airport) value).city()
                        : "";
                return super.getListCellRendererComponent(list, text, index, selected, focus);
            }
        };
        originCombo.setRenderer(renderer);
        destinationCombo.setRenderer(renderer);
        addButton.addActionListener(e -> add());
        removeButton.addActionListener(e -> remove());
        table.getSelectionModel().addListSelectionListener(e -> removeButton.setEnabled(table.getSelectedRow() >= 0));
    }

    private void remove() {
        User admin = controllers.session().currentUser().orElse(null);
        int row = table.getSelectedRow();
        if (admin == null || row < 0) {
            return;
        }
        Route route = tableModel.routeAt(table.convertRowIndexToModel(row));
        try {
            controllers.routeController().removeRoute(admin, route.id());
            tableModel.setRoutes(controllers.routeController().list());
            alert.show(Alert.Kind.SUCCESS, "Removed route " + route.origin().code() + " to "
                    + route.destination().code());
        } catch (RuntimeException e) {
            alert.show(Alert.Kind.ERROR, ErrorMessages.forException(e));
        }
    }

    private void add() {
        User admin = controllers.session().currentUser().orElse(null);
        Airport origin = (Airport) originCombo.getSelectedItem();
        Airport destination = (Airport) destinationCombo.getSelectedItem();
        if (admin == null || origin == null || destination == null) {
            return;
        }
        try {
            controllers.routeController().addRoute(admin, origin.code(), destination.code());
            tableModel.setRoutes(controllers.routeController().list());
            alert.show(Alert.Kind.SUCCESS, "Added route " + origin.code() + " to " + destination.code());
        } catch (RuntimeException e) {
            alert.show(Alert.Kind.ERROR, ErrorMessages.forException(e));
        }
    }

    @Override
    public JButton defaultButton() {
        return addButton;
    }

    @Override
    public JComponent initialFocus() {
        return originCombo;
    }

    @Override
    public void onShow() {
        Optional<User> user = controllers.session().currentUser();
        if (user.isEmpty() || user.get().role() != Role.ADMIN) {
            navigator.show(ViewId.LOGIN);
            return;
        }
        List<Airport> airports = controllers.flightSearchController().airports();
        originCombo.setModel(new DefaultComboBoxModel<>(new Vector<>(airports)));
        destinationCombo.setModel(new DefaultComboBoxModel<>(new Vector<>(airports)));
        if (airports.size() > 1) {
            destinationCombo.setSelectedIndex(1);
        }
        tableModel.setRoutes(controllers.routeController().list());
        alert.clear();
    }

    String message() {
        return alert.text();
    }

    Alert.Kind messageKind() {
        return alert.kind();
    }
}
