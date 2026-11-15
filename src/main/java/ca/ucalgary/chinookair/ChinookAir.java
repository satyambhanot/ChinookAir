package ca.ucalgary.chinookair;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.presentation.MainFrame;
import ca.ucalgary.chinookair.presentation.Theme;
import java.awt.GraphicsEnvironment;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Entry point: builds the controllers, then opens the main window. */
public class ChinookAir {

    public static void main(String[] args) {
        try {
            Controllers controllers = AppContext.create();
            // Swing isn't thread safe, so build the UI on the event thread
            SwingUtilities.invokeLater(() -> {
                Theme.install();
                new MainFrame(controllers).setVisible(true);
            });
        } catch (RuntimeException e) {
            reportStartupProblem(e);
            System.exit(1);
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
