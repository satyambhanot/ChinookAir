package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Role;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.awt.event.ActionEvent;
import java.awt.Toolkit;
import java.awt.GraphicsEnvironment;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.KeyStroke;
import javax.swing.InputMap;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The navy bar along the top of every screen after the login: the logo (a click goes Home), the tabs
 * of the user's role with the current one underlined, who is logged in, and Log out. Under it a strip
 * shows the steps of a booking or a change while the user is in one. Everything is hidden on the login
 * and registration screens, where nobody is logged in yet.
 */
class AppBar extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int BAR_HEIGHT = 60;

    private final transient Controllers controllers;
    private final transient Navigator navigator;
    private final JButton homeButton = new JButton("ChinookAir");
    private final JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    private final List<TabButton> tabs = new ArrayList<>();
    private final Avatar avatar = new Avatar();
    private final JLabel nameLabel = new JLabel(" ");
    private final JLabel roleLabel = new JLabel(" ");
    private final JButton logOutButton = new JButton("Log out");
    private final StepStrip strip = new StepStrip();
    private Role tabsFor;

    AppBar(Controllers controllers, Navigator navigator) {
        super();
        this.controllers = controllers;
        this.navigator = navigator;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(buildBar());
        add(strip);
        setVisible(false);
    }

    private JPanel buildBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Palette.NAVY_900);
        bar.setBorder(BorderFactory.createEmptyBorder(0, Space.XL, 0, Space.PAGE));
        bar.setPreferredSize(new Dimension(10, BAR_HEIGHT));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, BAR_HEIGHT));

        homeButton.setIcon(new LogoTile());
        homeButton.setIconTextGap(10);
        homeButton.setFont(homeButton.getFont().deriveFont(Font.BOLD, Type.SECTION + 2));
        homeButton.setToolTipText("Home");
        homeButton.getAccessibleContext().setAccessibleName("ChinookAir home");
        homeButton.putClientProperty("FlatLaf.styleClass", "brand");
        homeButton.addActionListener(e -> goHome());

        tabPanel.setOpaque(false);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        left.setOpaque(false);
        left.add(homeButton);
        left.add(tabPanel);
        bar.add(left, BorderLayout.WEST);

        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, Type.LABEL));
        nameLabel.setForeground(Color.WHITE);
        roleLabel.setFont(roleLabel.getFont().deriveFont(Type.CAPTION));
        roleLabel.setForeground(Palette.BORDER);
        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        words.add(nameLabel);
        words.add(roleLabel);
        logOutButton.putClientProperty("FlatLaf.styleClass", "onNavy");
        logOutButton.addActionListener(e -> logOut());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(avatar);
        right.add(words);
        right.add(logOutButton);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    /** Called after every screen change: shows the bar for a logged-in user and marks where they are. */
    void refresh(ViewId view) {
        User user = controllers.session().currentUser().orElse(null);
        boolean loggedIn = user != null && view != ViewId.LOGIN && view != ViewId.REGISTER;
        setVisible(loggedIn);
        if (!loggedIn) {
            return;
        }
        Role role = user.role();
        NavModel model = NavModel.forRole(role);
        if (role != tabsFor) {
            buildTabs(model);
            tabsFor = role;
        }
        Optional<NavModel.Tab> selected = model.selected(view);
        for (int i = 0; i < tabs.size(); i++) {
            tabs.get(i).mark(selected.isPresent() && selected.get().equals(model.tabs().get(i)));
        }
        String shown = displayName(user);
        nameLabel.setText(shown);
        roleLabel.setText(Dashboard.roleName(role));
        avatar.setInitial(shown);
        strip.show(StepperModel.flowFor(role, view));
        revalidate();
        repaint();
    }

    private void buildTabs(NavModel model) {
        tabPanel.removeAll();
        tabs.clear();
        InputMap keys = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        keys.clear();
        getActionMap().clear();
        int number = 1;
        for (NavModel.Tab tab : model.tabs()) {
            TabButton button = new TabButton(tab.label());
            button.addActionListener(e -> navigator.show(tab.opens()));
            // the shortcut key and a number: the first tab is 1, the second 2, and so on
            String name = "tab-" + number;
            keys.put(shortcut(number), name);
            getActionMap().put(name, new AbstractAction() {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    button.doClick(0);
                }
            });
            button.setToolTipText(tab.label() + " (" + shortcutText() + number + ")");
            tabs.add(button);
            tabPanel.add(button);
            number++;
        }
    }

    /** The key for tab number 1 to 9: Command on a Mac, Control elsewhere, with the digit. */
    static KeyStroke shortcut(int number) {
        int mask = GraphicsEnvironment.isHeadless() ? InputEvent.CTRL_DOWN_MASK
                : Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        return KeyStroke.getKeyStroke(KeyEvent.VK_0 + number, mask);
    }

    private static String shortcutText() {
        return System.getProperty("os.name", "").toLowerCase().startsWith("mac") ? "\u2318" : "Ctrl+";
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

    List<JButton> tabs() {
        return List.copyOf(tabs);
    }

    /** The words of the tab that is marked, or empty when no tab is (on Home, for example). */
    String selectedTab() {
        return tabs.stream().filter(TabButton::isSelectedTab).map(JButton::getText).findFirst().orElse("");
    }

    StepStrip steps() {
        return strip;
    }

    /** A customer is shown by name; the people who work here are shown by their username. */
    static String displayName(User user) {
        if (user instanceof Customer && !((Customer) user).name().isBlank()) {
            return ((Customer) user).name();
        }
        return user.username();
    }

    String user() {
        return nameLabel.getText();
    }

    String role() {
        return roleLabel.getText();
    }

    String initial() {
        return avatar.initial();
    }

    /** A tab of the bar: light text on navy, white and underlined in sky blue when it is the current one. */
    private static final class TabButton extends JButton {

        private static final long serialVersionUID = 1L;

        private boolean selected;

        TabButton(String text) {
            super(text);
            setFont(getFont().deriveFont(Font.PLAIN, Type.BODY));
            putClientProperty("FlatLaf.styleClass", "tab");
        }

        void mark(boolean selected) {
            this.selected = selected;
            putClientProperty("FlatLaf.styleClass", selected ? "tab tabSelected" : "tab");
            getAccessibleContext().setAccessibleDescription(selected ? "Current page" : "");
            repaint();
        }

        boolean isSelectedTab() {
            return selected;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (selected) {
                g.setColor(Palette.SKY_500);
                g.fillRect(10, getHeight() - 4, getWidth() - 20, 3);
            }
        }
    }

    /** The logo mark on a white rounded tile, so the picture is clear on the navy bar. */
    private static final class LogoTile implements Icon {

        private static final int WIDTH = 64;
        private static final int HEIGHT = 40;
        private final java.awt.Image mark = Brand.scaledToWidth(Brand.mark(), 52);

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(x, y, WIDTH, HEIGHT, 12, 12);
            g2.drawImage(mark, x + (WIDTH - mark.getWidth(null)) / 2, y + (HEIGHT - mark.getHeight(null)) / 2, null);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return WIDTH;
        }

        @Override
        public int getIconHeight() {
            return HEIGHT;
        }
    }

    /** A blue circle with the first letter of the username. */
    private static final class Avatar extends JComponent {

        private static final long serialVersionUID = 1L;
        private static final int SIZE = 34;

        private String initial = "?";

        void setInitial(String username) {
            this.initial = username == null || username.isEmpty() ? "?" : username.substring(0, 1).toUpperCase();
            repaint();
        }

        String initial() {
            return initial;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(SIZE, SIZE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Palette.BLUE_600);
            g2.fillOval(0, 0, SIZE, SIZE);
            g2.setColor(Color.WHITE);
            g2.setFont(getFont().deriveFont(Font.BOLD, Type.BODY));
            java.awt.FontMetrics metrics = g2.getFontMetrics();
            g2.drawString(initial, (SIZE - metrics.stringWidth(initial)) / 2,
                    (SIZE + metrics.getAscent() - metrics.getDescent()) / 2);
            g2.dispose();
        }
    }
}
