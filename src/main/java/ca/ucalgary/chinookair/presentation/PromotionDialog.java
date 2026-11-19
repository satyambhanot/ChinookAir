package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.entity.Promotion;
import java.awt.Frame;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

/**
 * The monthly promotion in a modal window over the main window, so it has to be dismissed before
 * the login screen can be used. OK, Enter and Escape all close it.
 */
class PromotionDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    PromotionDialog(Frame owner, Promotion promotion) {
        super(owner, "ChinookAir promotion", true);
        PromotionPanel panel = new PromotionPanel(promotion, this::dispose);
        setContentPane(panel);
        getRootPane().setDefaultButton(panel.okButton);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(owner);
    }
}
