package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.entity.Promotion;
import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

/** What the promotion dialog shows: a heading, the title, the description and an OK button. */
class PromotionPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    final JButton okButton = new JButton("OK");
    private final JLabel titleLabel;
    private final JTextArea descriptionArea;

    PromotionPanel(Promotion promotion, Runnable onOk) {
        super(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));

        JLabel heading = new JLabel("This month's promotion", SwingConstants.CENTER);
        titleLabel = new JLabel(promotion.title(), SwingConstants.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.add(heading, BorderLayout.NORTH);
        top.add(titleLabel, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        descriptionArea = new JTextArea(promotion.description(), 3, 32);
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setOpaque(false);
        descriptionArea.setFont(descriptionArea.getFont().deriveFont(15f));
        add(descriptionArea, BorderLayout.CENTER);

        okButton.setMnemonic('O');
        okButton.addActionListener(e -> onOk.run());
        JPanel bottom = new JPanel();
        bottom.add(okButton);
        add(bottom, BorderLayout.SOUTH);
    }

    String titleText() {
        return titleLabel.getText();
    }

    String descriptionText() {
        return descriptionArea.getText();
    }
}
