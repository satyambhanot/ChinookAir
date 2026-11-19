package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.entity.Promotion;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

/** What the promotion dialog shows: a heading, the title, the description and an OK button. */
class PromotionPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int MARK_WIDTH = 150;

    final JButton okButton = Theme.primary(new JButton("OK"));
    private final JLabel titleLabel;
    private final JTextArea descriptionArea;

    PromotionPanel(Promotion promotion, Runnable onOk) {
        super(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(Space.XL, Space.XL, Space.L, Space.XL));

        setBackground(Theme.SURFACE);
        JLabel mark = new JLabel(new ImageIcon(Brand.scaledToWidth(Brand.mark(), MARK_WIDTH)), SwingConstants.CENTER);
        mark.getAccessibleContext().setAccessibleName("ChinookAir");
        JLabel heading = Theme.hint("This month's promotion");
        heading.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel = new JLabel(promotion.title(), SwingConstants.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, Type.PAGE_HEADING));
        titleLabel.setForeground(Palette.NAVY_900);
        // one short gold line, like the sun in the logo
        JPanel rule = new JPanel();
        rule.setBackground(Palette.GOLD_500);
        rule.setPreferredSize(new Dimension(48, 3));
        JPanel ruleHolder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
        ruleHolder.setOpaque(false);
        ruleHolder.add(rule);
        JPanel words = new JPanel(new BorderLayout(0, 4));
        words.setOpaque(false);
        words.add(heading, BorderLayout.NORTH);
        words.add(titleLabel, BorderLayout.CENTER);
        words.add(ruleHolder, BorderLayout.SOUTH);
        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(mark, BorderLayout.NORTH);
        top.add(words, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        descriptionArea = new JTextArea(promotion.description(), 3, 32);
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setOpaque(false);
        descriptionArea.setFont(descriptionArea.getFont().deriveFont(Type.SECTION));
        add(descriptionArea, BorderLayout.CENTER);

        okButton.setMnemonic('O');
        okButton.addActionListener(e -> onOk.run());
        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
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
