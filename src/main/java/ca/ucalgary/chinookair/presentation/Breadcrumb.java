package ca.ucalgary.chinookair.presentation;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * "Customers › Casey Customer › Modify reservation": where you are, with a link back to each place
 * you can return to. The last part is the current screen and is not a link; a part without an action
 * is plain text.
 */
final class Breadcrumb extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String SEPARATOR = "›";

    /** One part of the path; the action is what a click does, or null for plain text. */
    record Crumb(String text, Runnable action) {

        static Crumb link(String text, Runnable action) {
            return new Crumb(text, action);
        }

        static Crumb text(String text) {
            return new Crumb(text, null);
        }
    }

    private final transient List<JButton> links = new ArrayList<>();
    private final transient List<String> texts = new ArrayList<>();

    Breadcrumb() {
        super();
        setOpaque(false);
        setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        getAccessibleContext().setAccessibleName("You are here");
    }

    /** Shows this path instead of the one shown before. */
    void show(List<Crumb> path) {
        removeAll();
        links.clear();
        texts.clear();
        for (int i = 0; i < path.size(); i++) {
            Crumb crumb = path.get(i);
            boolean last = i == path.size() - 1;
            if (i > 0) {
                JLabel separator = Theme.hint(SEPARATOR);
                separator.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, Space.S, 0, Space.S));
                add(separator);
            }
            texts.add(crumb.text());
            if (crumb.action() != null && !last) {
                JButton link = Theme.link(new JButton(crumb.text()));
                link.addActionListener(e -> crumb.action().run());
                links.add(link);
                add(link);
            } else {
                JLabel label = last ? new JLabel(crumb.text()) : Theme.hint(crumb.text());
                if (last) {
                    label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD, Type.LABEL));
                    label.setForeground(Theme.TEXT);
                }
                add(label);
            }
        }
        setVisible(!path.isEmpty());
        revalidate();
        repaint();
    }

    /** The parts that can be clicked, left to right. */
    List<JButton> links() {
        return List.copyOf(links);
    }

    /** Every part of the path as text, left to right. */
    List<String> texts() {
        return List.copyOf(texts);
    }
}
