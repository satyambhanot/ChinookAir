package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Role;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

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
    private final JLabel greeting = Theme.title(" ");
    private final List<JButton> taskButtons = new ArrayList<>();

    /**
     * @param title    what the dashboard is called, like "Customer"
     * @param tasks    one card for every task; the labels need distinct mnemonics
     */
    Dashboard(Controllers controllers, Navigator navigator, Role role, String title, List<Task> tasks) {
        super(new BorderLayout(0, 24));
        this.controllers = controllers;
        this.navigator = navigator;
        this.role = role;
        setBorder(BorderFactory.createEmptyBorder(Space.PAGE, Space.PAGE, Space.PAGE, Space.PAGE));
        setName(title + " dashboard");

        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.setOpaque(false);
        top.add(greeting, BorderLayout.NORTH);
        top.add(Theme.hint("What would you like to do?"), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(0, 2, 16, 16));
        cards.setOpaque(false);
        for (Task task : tasks) {
            TaskCard card = new TaskCard(task.label(), task.description(), task.icon());
            card.setMnemonic(task.mnemonic());
            card.addActionListener(e -> navigator.show(task.view()));
            taskButtons.add(card);
            cards.add(card);
        }
        // the cards sit at the top and keep their size when the window grows
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(cards, BorderLayout.NORTH);
        add(holder, BorderLayout.CENTER);
    }

    /** One card: its text, what it is for, its icon, the key that presses it, and the screen it opens. */
    record Task(String label, String description, String icon, char mnemonic, ViewId view) {
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

    @Override
    public JComponent initialFocus() {
        return taskButtons.isEmpty() ? null : taskButtons.get(0);
    }

    @Override
    public void onShow() {
        Optional<User> user = controllers.session().currentUser();
        if (user.isEmpty() || user.get().role() != role) {
            // nobody, or somebody else's dashboard: back to the login screen
            navigator.show(ViewId.LOGIN);
            return;
        }
        greeting.setText("Hello, " + user.get().username());
    }

    // ---- for the tests

    /** The greeting at the top, like "Hello, admin". */
    String headerText() {
        return greeting.getText();
    }

    List<JButton> taskButtons() {
        return taskButtons;
    }
}
