package ca.ucalgary.chinookair.presentation;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Stands in for a screen that is not built yet. */
class ComingSoonView extends JPanel {

    private static final long serialVersionUID = 1L;

    ComingSoonView(ViewId view) {
        super(new BorderLayout());
        add(new JLabel(view.title() + " is coming soon", SwingConstants.CENTER), BorderLayout.CENTER);
    }
}
