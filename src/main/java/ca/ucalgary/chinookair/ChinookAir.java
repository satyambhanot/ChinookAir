package ca.ucalgary.chinookair;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Entry point. For now it just opens an empty window so we know the
 * jar runs - the login screen will replace this.
 */
public class ChinookAir {

    public static void main(String[] args) {
        // Swing isn't thread safe, so build the UI on the event thread
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("ChinookAir");
            frame.add(new JLabel("ChinookAir - coming soon", SwingConstants.CENTER));
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}
