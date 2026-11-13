package ca.ucalgary.chinookair.presentation;

import java.awt.Component;
import javax.swing.JOptionPane;

/** The two little pop-ups every screen needs, so they all look and read the same. */
final class Dialogs {

    private static final String TITLE = "ChinookAir";

    private Dialogs() {
    }

    /** Shows what went wrong in one plain sentence. */
    static void error(Component parent, Throwable problem) {
        JOptionPane.showMessageDialog(parent, ErrorMessages.forException(problem), TITLE, JOptionPane.ERROR_MESSAGE);
    }

    static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, TITLE, JOptionPane.INFORMATION_MESSAGE);
    }
}
