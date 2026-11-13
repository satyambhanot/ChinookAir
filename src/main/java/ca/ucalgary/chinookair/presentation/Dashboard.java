package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Role;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * The home screen of a role: who is logged in, one button for every task of that role, and Log out.
 * It only navigates; every task is its own screen. Someone with the wrong role, or nobody, is sent
 * back to the login screen.
 */
abstract class Dashboard extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;

    private final transient Controllers controllers;
    private final transient Navigator navigator;
    private final Role role;
    private final JLabel header = new JLabel(" ", SwingConstants.LEFT);
    private final List<JButton> taskButtons = new ArrayList<>();
    private final JButton logOutButton = new JButton("Log out");

    /**
     * @param title    what the dashboard is called, like "Customer"
     * @param tasks    the label of each button and the screen it opens; the labels need distinct mnemonics
     */
    Dashboard(Controllers controllers, Navigator navigator, Role role, String title, List<Task> tasks) {
        super(new BorderLayout(0, 20));
        this.controllers = controllers;
        this.navigator = navigator;
        this.role = role;
        setBorder(BorderFactory.createEmptyBorder(20, 30, 30, 30));

        header.setFont(header.getFont().deriveFont(Font.BOLD, 16f));
        JLabel heading = new JLabel(title + " dashboard", SwingConstants.LEFT);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 24f));
        JPanel top = new JPanel(new BorderLayout());
        top.add(heading, BorderLayout.NORTH);
        top.add(header, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new GridLayout(0, 2, 20, 20));
        for (Task task : tasks) {
            JButton button = new JButton(task.label());
            button.setMnemonic(task.mnemonic());
            button.setFont(button.getFont().deriveFont(16f));
            button.addActionListener(e -> navigator.show(task.view()));
            taskButtons.add(button);
            buttons.add(button);
        }
        logOutButton.setMnemonic('O');
        logOutButton.setFont(logOutButton.getFont().deriveFont(16f));
        logOutButton.addActionListener(e -> logOut());
        buttons.add(logOutButton);
        add(buttons, BorderLayout.CENTER);
    }

    /** One button: its text, the key that presses it, and the screen it opens. */
    record Task(String label, char mnemonic, ViewId view) {
    }

    /** The name of a role in words, as shown in the header. */
    static String roleName(Role role) {
        switch (role) {
            case CUSTOMER:
                return "Customer";
            case AGENT:
                return "Flight agent";
            case ADMIN:
                return "Administrator";
            default:
                throw new IllegalArgumentException("Unknown role " + role);
        }
    }

    private void logOut() {
        controllers.session().logout();
        navigator.show(ViewId.LOGIN);
    }

    @Override
    public void onShow() {
        Optional<User> user = controllers.session().currentUser();
        if (user.isEmpty() || user.get().role() != role) {
            // nobody, or somebody else's dashboard: back to the login screen
            navigator.show(ViewId.LOGIN);
            return;
        }
        header.setText("Logged in as " + user.get().username() + " (" + roleName(role) + ")");
    }

    // ---- for the tests

    /** The text of the header, like "Logged in as admin (Administrator)". */
    String headerText() {
        return header.getText();
    }

    List<JButton> taskButtons() {
        return taskButtons;
    }

    JButton logOutButton() {
        return logOutButton;
    }
}
