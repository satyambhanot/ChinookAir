package ca.ucalgary.chinookair.presentation;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingConstants;

/**
 * The small window with the logo that shows while the program gets ready. The first start makes the
 * database and the flights, which takes several seconds, so without it the program would look frozen.
 * The bar has no percentage because the program cannot tell how far along it is.
 */
public class SplashWindow extends JWindow {

    private static final long serialVersionUID = 1L;
    private static final int LOGO_WIDTH = 300;

    private final JLabel status = new JLabel("Getting ChinookAir ready…", SwingConstants.CENTER);
    private final JProgressBar bar = new JProgressBar();

    public SplashWindow() {
        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(Theme.SURFACE);
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(Space.XXL, Space.XXXL, Space.XL, Space.XXXL)));

        JLabel logo = new JLabel(new ImageIcon(Brand.scaledToWidth(Brand.fullLogo(), LOGO_WIDTH)));
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        logo.getAccessibleContext().setAccessibleName("ChinookAir");
        status.setForeground(Theme.MUTED);
        bar.setIndeterminate(true);
        bar.getAccessibleContext().setAccessibleName("Starting");
        JPanel south = new JPanel();
        south.setOpaque(false);
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        status.setAlignmentX(CENTER_ALIGNMENT);
        bar.setAlignmentX(CENTER_ALIGNMENT);
        south.add(status);
        south.add(javax.swing.Box.createVerticalStrut(10));
        south.add(bar);
        content.add(logo, BorderLayout.CENTER);
        content.add(south, BorderLayout.SOUTH);
        setContentPane(content);
        setIconImages(Brand.icons());
        pack();
        setMinimumSize(new Dimension(380, 0));
        setLocationRelativeTo(null);
    }

    /** What the window says under the logo, like "Getting ChinookAir ready". */
    public void setStatus(String text) {
        status.setText(text);
    }

    String statusText() {
        return status.getText();
    }

    boolean isMoving() {
        return bar.isIndeterminate();
    }
}
