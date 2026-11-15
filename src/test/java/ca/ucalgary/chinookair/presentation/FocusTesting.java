package ca.ucalgary.chinookair.presentation;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.JComponent;

/** Lets a test say that the cursor left a box, without a window that has the keyboard focus. */
final class FocusTesting {

    private FocusTesting() {
    }

    static void leave(JComponent box) {
        FocusEvent lost = new FocusEvent(box, FocusEvent.FOCUS_LOST, false);
        for (FocusListener listener : box.getFocusListeners()) {
            listener.focusLost(lost);
        }
    }
}
