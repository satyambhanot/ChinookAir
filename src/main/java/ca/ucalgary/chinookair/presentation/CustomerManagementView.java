package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.LineBorder;

/**
 * The agent's customers: the list with a search that follows the typing, and the chosen customer on the
 * right. From there the agent changes the data, opens the reservations or starts changing a reservation.
 * New customer opens the same panel empty. The rules are the CustomerController's; the screen only shows
 * its answers.
 */
class CustomerManagementView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final int SIDE_WIDTH = 360;
    private static final String EMPTY = "empty";
    private static final String DETAILS = "details";

    private final transient Controllers controllers;
    private final transient Navigator navigator;
    private final transient BookingSession booking;

    final SearchField searchField = new SearchField("Search by name or email", 24);
    final CustomerTableModel tableModel = new CustomerTableModel();
    final HintTable table = new HintTable(tableModel, "No customers match. Try another name or email.");
    final CustomerForm form = new CustomerForm();
    final JButton newButton = Theme.secondary(new JButton("New customer"));
    final JButton saveButton = Theme.primary(new JButton("Save"));
    final JButton reservationsButton = Theme.secondary(new JButton("Reservations"));
    final JButton modifyButton = Theme.secondary(new JButton("Modify a reservation"));
    private final Alert alert = new Alert(SIDE_WIDTH - 90);
    private final JLabel detailsTitle = new JLabel("Customer");
    private final CardLayout cards = new CardLayout();
    private final JPanel side = new JPanel(cards);

    CustomerManagementView(Controllers controllers, Navigator navigator, BookingSession booking) {
        super(new BorderLayout(0, 12));
        this.controllers = controllers;
        this.navigator = navigator;
        this.booking = booking;
        build();
    }

    private void build() {
        setBorder(BorderFactory.createEmptyBorder(Theme.PAGE - 8, Theme.PAGE, Theme.PAGE - 8, Theme.PAGE));
        JLabel heading = new JLabel("Customers");
        Theme.heading(heading);
        newButton.setMnemonic('W');
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(heading, BorderLayout.WEST);
        top.add(newButton, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // the list on the left, with the search above it
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.describe("Customers");
        JPanel list = new JPanel(new BorderLayout(0, 10));
        list.setOpaque(false);
        list.add(searchField, BorderLayout.NORTH);
        list.add(new JScrollPane(table), BorderLayout.CENTER);
        add(list, BorderLayout.CENTER);

        // the chosen customer on the right, or a hint to choose one
        EmptyState hint = new EmptyState("Choose a customer", "Pick someone from the list, or add a new customer.");
        JPanel emptyHolder = new JPanel(new BorderLayout());
        emptyHolder.setBackground(Theme.SURFACE);
        emptyHolder.setBorder(new LineBorder(Theme.BORDER, 1, true));
        emptyHolder.add(hint, BorderLayout.NORTH);

        detailsTitle.setFont(detailsTitle.getFont().deriveFont(java.awt.Font.BOLD, Type.SECTION));
        saveButton.setMnemonic('V');
        reservationsButton.setMnemonic('R');
        modifyButton.setMnemonic('M');
        // one under the other, so both labels always fit in the narrow panel
        JPanel secondary = new JPanel(new java.awt.GridLayout(2, 1, 0, 8));
        secondary.setOpaque(false);
        secondary.add(reservationsButton);
        secondary.add(modifyButton);
        JPanel save = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        save.setOpaque(false);
        save.add(saveButton);
        JPanel actions = new JPanel(new BorderLayout(0, 12));
        actions.setOpaque(false);
        actions.add(alert, BorderLayout.NORTH);
        actions.add(secondary, BorderLayout.CENTER);
        actions.add(save, BorderLayout.SOUTH);
        JPanel details = new JPanel(new BorderLayout(0, 12));
        details.setBackground(Theme.SURFACE);
        details.setBorder(BorderFactory.createCompoundBorder(new LineBorder(Theme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(20, 22, 16, 22)));
        details.add(detailsTitle, BorderLayout.NORTH);
        details.add(ScrollableForm.around(form), BorderLayout.CENTER);
        details.add(actions, BorderLayout.SOUTH);

        side.setOpaque(false);
        side.setPreferredSize(new Dimension(SIDE_WIDTH, 10));
        side.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
        side.add(emptyHolder, EMPTY);
        side.add(details, DETAILS);
        add(side, BorderLayout.EAST);

        updateButtons();
        searchField.onChange(text -> search());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                rowChosen();
            }
        });
        newButton.addActionListener(e -> startNew());
        saveButton.addActionListener(e -> save());
        reservationsButton.addActionListener(e -> openReservations());
        modifyButton.addActionListener(e -> openModify());
    }

    private void updateButtons() {
        boolean chosen = table.getSelectedRow() >= 0;
        reservationsButton.setEnabled(chosen);
        modifyButton.setEnabled(chosen);
        saveButton.setEnabled(chosen || form.isNewCustomer());
        cards.show(side, chosen || form.isNewCustomer() ? DETAILS : EMPTY);
        detailsTitle.setText(form.isNewCustomer() ? "New customer" : "Customer");
    }

    private void rowChosen() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            alert.clear();
            form.show(tableModel.customerAt(table.convertRowIndexToModel(row)));
        }
        updateButtons();
    }

    private Customer selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : tableModel.customerAt(table.convertRowIndexToModel(row));
    }

    /** Looks up the customers that match the search box; it runs after every change of the text. */
    void search() {
        User agent = controllers.session().currentUser().orElse(null);
        if (agent == null) {
            return;
        }
        try {
            List<Customer> found = controllers.customerController().search(agent, searchField.getText());
            tableModel.setCustomers(found);
            form.clear();
            updateButtons();
        } catch (RuntimeException e) {
            alert.show(Alert.Kind.ERROR, ErrorMessages.forException(e));
        }
    }

    private void startNew() {
        table.clearSelection();
        form.startNew();
        alert.clear();
        updateButtons();
    }

    private void save() {
        User agent = controllers.session().currentUser().orElse(null);
        if (agent == null) {
            return;
        }
        try {
            Customer saved;
            if (form.isNewCustomer()) {
                saved = controllers.customerController().register(form.usernameField.getText(),
                        new String(form.password()), form.nameField.getText(), form.emailField.getText(),
                        form.phoneField.getText());
            } else {
                Customer chosen = selected();
                if (chosen == null) {
                    return;
                }
                saved = controllers.customerController().update(agent, chosen.id(), form.nameField.getText(),
                        form.emailField.getText(), form.phoneField.getText());
            }
            // look for the saved customer again, so the table shows what is stored
            // setText counts as a change even when the text is the same, so the search runs again
            searchField.setText(saved.email());
            if (tableModel.getRowCount() > 0) {
                table.setRowSelectionInterval(0, 0);
            }
            alert.show(Alert.Kind.SUCCESS, "Saved " + saved.name());
        } catch (RuntimeException e) {
            alert.show(Alert.Kind.ERROR, ErrorMessages.forException(e));
        } finally {
            form.wipePassword();
        }
    }

    private void openReservations() {
        Customer chosen = selected();
        if (chosen == null) {
            return;
        }
        booking.clear();
        booking.bookFor(chosen);
        navigator.show(ViewId.MY_RESERVATIONS);
    }

    private void openModify() {
        Customer chosen = selected();
        if (chosen == null) {
            return;
        }
        booking.clear();
        booking.bookFor(chosen);
        navigator.show(ViewId.MODIFY_RESERVATION);
    }

    @Override
    public JComponent initialFocus() {
        return searchField;
    }

    @Override
    public void onShow() {
        if (controllers.session().currentUser().isEmpty()) {
            navigator.show(ViewId.LOGIN);
            return;
        }
        alert.clear();
        searchField.setText("");
        search();
    }

    String message() {
        return alert.text();
    }

    Alert.Kind messageKind() {
        return alert.kind();
    }

    /** True while the right-hand panel shows a customer (or an empty new one) instead of the hint. */
    boolean detailsShown() {
        return form.isShowing() || table.getSelectedRow() >= 0 || form.isNewCustomer();
    }
}
