package ca.ucalgary.chinookair.presentation;

import javax.swing.JButton;
import javax.swing.JComponent;

/** A screen that wants to know when it is shown, so it can clear its fields or reload what it displays. */
interface Screen {

    void onShow();

    /** The button that Enter presses on this screen, or null when no button is the obvious one. */
    default JButton defaultButton() {
        return null;
    }

    /** What has the keyboard focus when the screen opens, or null to leave it where it is. */
    default JComponent initialFocus() {
        return null;
    }
}
