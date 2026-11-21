package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Customer;
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
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

/**
 * The customer's own profile: the name, email and phone can be changed, the username can not. Save
 * stays off until something is different from what is stored, and the answer shows in a strip.
 */
class ProfileView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final int CARD_WIDTH = 560;

    private final transient Controllers controllers;
    private final transient Navigator navigator;

    final CustomerForm form = new CustomerForm();
    final JButton saveButton = Theme.primary(new JButton("Save changes"));
    private final Alert alert = new Alert(CARD_WIDTH - 100);
    private transient Customer loaded;

    ProfileView(Controllers controllers, Navigator navigator) {
        super(new BorderLayout(0, 12));
        this.controllers = controllers;
        this.navigator = navigator;
        setBorder(BorderFactory.createEmptyBorder(Space.XL, Space.PAGE, Space.PAGE, Space.PAGE));
        JLabel heading = new JLabel("My profile");
        Theme.heading(heading);
        add(heading, BorderLayout.NORTH);

        saveButton.setMnemonic('V');
        ButtonRow buttons = ButtonRow.right();
        buttons.setOpaque(false);
        buttons.add(saveButton);
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Theme.SURFACE);
        SurfaceBorder.apply(card, Space.XL, Space.XL, Space.XL, Space.XL);
        card.add(form, BorderLayout.NORTH);
        JPanel south = new JPanel(new BorderLayout(0, 12));
        south.setOpaque(false);
        south.add(alert, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);
        card.add(south, BorderLayout.SOUTH);
        card.setPreferredSize(new Dimension(CARD_WIDTH, card.getPreferredSize().height));
        JPanel holder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        holder.setOpaque(false);
        holder.add(card);
        add(holder, BorderLayout.CENTER);

        DocumentListener changes = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateSave();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateSave();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateSave();
            }
        };
        form.nameField.getDocument().addDocumentListener(changes);
        form.emailField.getDocument().addDocumentListener(changes);
        form.phoneField.getDocument().addDocumentListener(changes);
        saveButton.setEnabled(false);
        saveButton.addActionListener(e -> save());
    }

    /** Save is only useful when the form differs from what is stored. */
    private void updateSave() {
        boolean changed = loaded != null && !(form.nameField.getText().equals(loaded.name())
                && form.emailField.getText().equals(loaded.email())
                && form.phoneField.getText().equals(loaded.phone() == null ? "" : loaded.phone()));
        saveButton.setEnabled(changed);
    }

    private void save() {
        Optional<User> user = controllers.session().currentUser();
        if (user.isEmpty()) {
            return;
        }
        try {
            Customer saved = controllers.customerController().update(user.get(), user.get().id(),
                    form.nameField.getText(), form.emailField.getText(), form.phoneField.getText());
            // the session keeps the new data too
            controllers.session().login(saved);
            loaded = saved;
            form.show(saved);
            updateSave();
            alert.show(Alert.Kind.SUCCESS, "Saved");
        } catch (RuntimeException e) {
            alert.show(Alert.Kind.ERROR, ErrorMessages.forException(e));
        }
    }

    @Override
    public JButton defaultButton() {
        return saveButton;
    }

    @Override
    public JComponent initialFocus() {
        return form.nameField;
    }

    @Override
    public void onShow() {
        Optional<User> user = controllers.session().currentUser();
        if (user.isEmpty() || !(user.get() instanceof Customer)) {
            navigator.show(ViewId.LOGIN);
            return;
        }
        Customer shown = (Customer) user.get();
        try {
            // what is stored, in case the profile was changed somewhere else since the login
            shown = controllers.customerController().view(user.get(), user.get().id());
            controllers.session().login(shown);
        } catch (RuntimeException e) {
            // the data of the login is good enough to show
        }
        loaded = shown;
        form.show(loaded);
        alert.clear();
        updateSave();
    }

    String message() {
        return alert.text();
    }

    Alert.Kind messageKind() {
        return alert.kind();
    }
}
