package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.EnumSet;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

/**
 * The one window of the program. It holds the screens as cards and shows one at a time; screens ask
 * for another screen through the {@link Navigator}. Closing the window closes the database.
 */
public class MainFrame extends JFrame implements Navigator {

    private static final long serialVersionUID = 1L;

    private final transient Controllers controllers;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Set<ViewId> added = EnumSet.noneOf(ViewId.class);
    private ViewId current;

    public MainFrame(Controllers controllers) {
        super("ChinookAir");
        this.controllers = controllers;
        setSize(1000, 700);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);
        setContentPane(content);
        // the window closes itself, and the database is closed first
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                controllers.shutdown().run();
            }
        });
        show(ViewId.LOGIN);
    }

    /** Shows a screen. A screen that is not built yet shows a "coming soon" page. */
    @Override
    public final void show(ViewId view) {
        if (view == null) {
            throw new IllegalArgumentException("A screen is required");
        }
        if (!added.contains(view)) {
            register(view, new ComingSoonView(view));
        }
        cards.show(content, view.name());
        current = view;
    }

    /** Puts a built screen in place of the one that stands for it. */
    void register(ViewId view, JComponent screen) {
        content.add(screen, view.name());
        added.add(view);
    }

    /** The screen that is showing now, for tests. */
    ViewId currentView() {
        return current;
    }

    Controllers controllers() {
        return controllers;
    }
}
