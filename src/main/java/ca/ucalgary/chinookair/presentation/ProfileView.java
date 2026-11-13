package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** The customer's own profile: the name, email and phone can be changed, the username can not. */
class ProfileView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final Color ERROR = new Color(0xB00020);
    private static final Color NOTICE = new Color(0x1B5E20);

    private final transient Controllers controllers;
    private final transient Navigator navigator;

    final CustomerForm form = new CustomerForm();
    final JButton saveButton = new JButton("Save");
    final JButton backButton = new JButton("Back");
    private final JLabel messageLabel = new JLabel(" ");

    ProfileView(Controllers controllers, Navigator navigator) {
        super(new BorderLayout(0, 10));
        this.controllers = controllers;
        this.navigator = navigator;
        setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        JLabel heading = new JLabel("My profile");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 22f));
        add(heading, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout());
        center.add(form, BorderLayout.NORTH);
        center.add(messageLabel, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
        saveButton.setMnemonic('V');
        backButton.setMnemonic('B');
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(backButton);
        buttons.add(saveButton);
        add(buttons, BorderLayout.SOUTH);
        saveButton.addActionListener(e -> save());
        backButton.addActionListener(e -> goBack());
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
            form.show(saved);
            showMessage("Saved", NOTICE);
        } catch (RuntimeException e) {
            showMessage(ErrorMessages.forException(e), ERROR);
        }
    }

    private void goBack() {
        Optional<User> user = controllers.session().currentUser();
        navigator.show(user.isPresent() ? ViewId.dashboardFor(user.get().role()) : ViewId.LOGIN);
    }

    private void showMessage(String text, Color color) {
        messageLabel.setForeground(color);
        messageLabel.setText(text);
    }

    @Override
    public void onShow() {
        Optional<User> user = controllers.session().currentUser();
        if (user.isEmpty() || !(user.get() instanceof Customer)) {
            navigator.show(ViewId.LOGIN);
            return;
        }
        form.show((Customer) user.get());
        messageLabel.setText(" ");
    }

    String message() {
        return messageLabel.getText().trim();
    }
}
