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

    /**
     * The least height, in pixels, this screen is useful in. In a window lower than that the page scrolls
     * instead of squeezing the screen. Most screens shrink and scroll their own lists, so they say 0.
     */
    default int minimumHeight() {
        return 0;
    }

    /** What has the keyboard focus when the screen opens, or null to leave it where it is. */
    default JComponent initialFocus() {
        return null;
    }
}
