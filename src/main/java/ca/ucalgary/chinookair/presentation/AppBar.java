package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The bar along the top of every screen after the login. It shows where you are (Home and the name
 * of the screen), who is logged in, and the Log out button, so these are one click away everywhere.
 * It is hidden on the login and registration screens, where nobody is logged in yet.
 */
class AppBar extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient Controllers controllers;
    private final transient Navigator navigator;
    private final JButton homeButton = Theme.link(new JButton("Home"));
    private final JLabel separator = Theme.hint("›");
    private final JLabel here = new JLabel(" ");
    private final JLabel who = Theme.hint(" ");
    private final JButton logOutButton = Theme.secondary(new JButton("Log out"));

    AppBar(Controllers controllers, Navigator navigator) {
        super(new BorderLayout());
        this.controllers = controllers;
        this.navigator = navigator;
        setBackground(Theme.SURFACE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(8, Theme.PAGE, 8, Theme.PAGE)));

        JLabel brand = new JLabel("ChinookAir");
        brand.setFont(brand.getFont().deriveFont(Font.BOLD, 18f));
        brand.setForeground(Theme.ACCENT);

        here.setFont(here.getFont().deriveFont(Font.BOLD, 14f));
        homeButton.setMnemonic('H');
        homeButton.addActionListener(e -> goHome());
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.GAP, 0));
        left.setOpaque(false);
        left.add(brand);
        left.add(Box.createVerticalStrut(24));
        left.add(homeButton);
        left.add(separator);
        left.add(here);
        add(left, BorderLayout.WEST);

        logOutButton.setMnemonic('O');
        logOutButton.addActionListener(e -> logOut());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.PAD, 0));
        right.setOpaque(false);
        right.add(who);
        right.add(logOutButton);
        add(right, BorderLayout.EAST);
        setVisible(false);
    }

    /** Called after every screen change: shows the bar for a logged-in user and fills in the place. */
    void refresh(ViewId view) {
        User user = controllers.session().currentUser().orElse(null);
        boolean loggedIn = user != null && view != ViewId.LOGIN && view != ViewId.REGISTER;
        setVisible(loggedIn);
        if (!loggedIn) {
            return;
        }
        boolean onHome = view == ViewId.dashboardFor(user.role());
        homeButton.setVisible(!onHome);
        separator.setVisible(!onHome);
        here.setText(onHome ? "Home" : view.label());
        who.setText(user.username() + " · " + Dashboard.roleName(user.role()));
    }

    private void goHome() {
        controllers.session().currentUser().ifPresent(u -> navigator.show(ViewId.dashboardFor(u.role())));
    }

    private void logOut() {
        controllers.session().logout();
        navigator.show(ViewId.LOGIN);
    }

    // ---- for the tests

    JButton homeButton() {
        return homeButton;
    }

    JButton logOutButton() {
        return logOutButton;
    }

    String place() {
        return here.getText();
    }

    String user() {
        return who.getText();
    }
}
