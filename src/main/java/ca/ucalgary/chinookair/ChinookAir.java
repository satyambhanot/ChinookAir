package ca.ucalgary.chinookair;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.presentation.Brand;
import ca.ucalgary.chinookair.presentation.MainFrame;
import ca.ucalgary.chinookair.presentation.SplashWindow;
import ca.ucalgary.chinookair.presentation.Theme;
import java.awt.GraphicsEnvironment;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Entry point. A small window with the logo opens at once, the database and the controllers are made in
 * the background, and the main window replaces the small one when everything is ready.
 */
public class ChinookAir {

    public static void main(String[] args) {
        // Swing isn't thread safe, so build the UI on the event thread
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            Brand.installTaskbarIcon();
            SplashWindow splash = new SplashWindow();
            splash.setVisible(true);
            Thread loader = new Thread(() -> load(splash), "start-up");
            loader.start();
        });
    }

    /** Runs off the event thread: the first start takes several seconds to make the flights. */
    private static void load(SplashWindow splash) {
        try {
            Controllers controllers = AppContext.create();
            SwingUtilities.invokeLater(() -> {
                new MainFrame(controllers).setVisible(true);
                splash.dispose();
            });
        } catch (RuntimeException e) {
            SwingUtilities.invokeLater(() -> {
                splash.dispose();
                reportStartupProblem(e);
                System.exit(1);
            });
        }
    }

    private static void reportStartupProblem(RuntimeException e) {
        String message = "ChinookAir could not start: " + e.getMessage();
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println(message);
        } else {
            JOptionPane.showMessageDialog(null, message, "ChinookAir", JOptionPane.ERROR_MESSAGE);
        }
    }
}
