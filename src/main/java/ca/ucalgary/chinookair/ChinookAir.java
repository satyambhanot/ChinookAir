package ca.ucalgary.chinookair;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.presentation.MainFrame;
import java.awt.GraphicsEnvironment;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/** Entry point: builds the controllers, then opens the main window. */
public class ChinookAir {

    public static void main(String[] args) {
        try {
            Controllers controllers = AppContext.create();
            // Swing isn't thread safe, so build the UI on the event thread
            SwingUtilities.invokeLater(() -> {
                useSystemLookAndFeel();
                new MainFrame(controllers).setVisible(true);
            });
        } catch (RuntimeException e) {
            reportStartupProblem(e);
            System.exit(1);
        }
    }

    /** The look of the operating system, or Nimbus if that is not possible. */
    private static void useSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    try {
                        UIManager.setLookAndFeel(info.getClassName());
                    } catch (ReflectiveOperationException | UnsupportedLookAndFeelException ignored) {
                        // the default look is fine too
                    }
                }
            }
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
