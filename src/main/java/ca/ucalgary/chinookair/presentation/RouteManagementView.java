package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Airport;
import ca.ucalgary.chinookair.business.entity.Role;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
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

/** For the administrator: the routes there are, and a way to add one between two airports. */
class RouteManagementView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final Color ERROR = new Color(0xB00020);
    private static final Color NOTICE = new Color(0x1B5E20);

    private final transient Controllers controllers;
    private final transient Navigator navigator;

    final JComboBox<Airport> originCombo = new JComboBox<>();
    final JComboBox<Airport> destinationCombo = new JComboBox<>();
    final JButton addButton = new JButton("Add route");
    final JButton backButton = new JButton("Back");
    final RouteTableModel tableModel = new RouteTableModel();
    final JTable table = new JTable(tableModel);
    private final JLabel messageLabel = new JLabel(" ");

    RouteManagementView(Controllers controllers, Navigator navigator) {
        super(new BorderLayout(0, 10));
        this.controllers = controllers;
        this.navigator = navigator;
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        JLabel heading = new JLabel("Routes");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 22f));
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        form.add(label("From", 'F', originCombo));
        form.add(originCombo);
        form.add(label("To", 'T', destinationCombo));
        form.add(destinationCombo);
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
        Airport origin = (Airport) originCombo.getSelectedItem();
        Airport destination = (Airport) destinationCombo.getSelectedItem();
        if (admin == null || origin == null || destination == null) {
            return;
        }
        try {
            controllers.routeController().addRoute(admin, origin.code(), destination.code());
            tableModel.setRoutes(controllers.routeController().list());
            showMessage("Added route " + origin.code() + " to " + destination.code(), NOTICE);
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
        List<Airport> airports = controllers.flightSearchController().airports();
        originCombo.setModel(new DefaultComboBoxModel<>(new Vector<>(airports)));
        destinationCombo.setModel(new DefaultComboBoxModel<>(new Vector<>(airports)));
        if (airports.size() > 1) {
            destinationCombo.setSelectedIndex(1);
        }
        tableModel.setRoutes(controllers.routeController().list());
        messageLabel.setText(" ");
    }

    String message() {
        return messageLabel.getText().trim();
    }
}
