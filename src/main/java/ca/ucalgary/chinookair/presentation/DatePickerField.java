package ca.ucalgary.chinookair.presentation;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.function.Supplier;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

/**
 * A text field for a date, written as yyyy-MM-dd, with a calendar button that opens a month to click
 * in. It is still a text field, so typing, pasting and reading the text work as before. A date can
 * only be picked from the calendar when it lies between the minimum and maximum date.
 */
class DatePickerField extends JTextField {

    private static final long serialVersionUID = 1L;
    private static final String FORMAT = "yyyy-MM-dd";

    private final transient Supplier<LocalDate> today;
    private final JPopupMenu popup = new JPopupMenu();
    private LocalDate min;
    private LocalDate max;
    private MonthGridModel shown;

    /**
     * @param today where "today" is, so the calendar follows the program's clock
     */
    DatePickerField(int columns, Supplier<LocalDate> today) {
        super(columns);
        this.today = today;
        putClientProperty("JTextField.placeholderText", FORMAT);
        JButton button = new JButton(Icons.muted("calendar", 16));
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.setToolTipText("Choose a date");
        button.getAccessibleContext().setAccessibleName("Choose a date");
        button.setFocusable(false);
        button.addActionListener(e -> openCalendar());
        putClientProperty("JTextField.trailingComponent", button);
        getAccessibleContext().setAccessibleName("Date");
        // Alt + Down opens the calendar, like on most date fields
        getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("alt DOWN"), "openCalendar");
        getActionMap().put("openCalendar", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                openCalendar();
            }
        });
        popup.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
    }

    /** The first date the calendar lets the user pick, or null for no limit. */
    void setMinDate(LocalDate date) {
        this.min = date;
    }

    /** The last date the calendar lets the user pick, or null for no limit. */
    void setMaxDate(LocalDate date) {
        this.max = date;
    }

    /** The date in the text, or empty when the text is not a date. */
    Optional<LocalDate> date() {
        try {
            return Optional.of(LocalDate.parse(getText().trim()));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    void setDate(LocalDate date) {
        setText(date == null ? "" : date.toString());
    }

    /** Opens the calendar on the month of the typed date, or this month when there is none. */
    void openCalendar() {
        LocalDate chosen = date().orElse(null);
        shown = new MonthGridModel(YearMonth.from(chosen != null ? chosen : today.get()), today.get(), chosen,
                min, max);
        popup.removeAll();
        popup.add(buildCalendar());
        if (isShowing()) {
            popup.show(this, 0, getHeight() + 2);
        }
    }

    /** The calendar for the month now shown; a new one is built after every change of month. */
    JPanel calendar() {
        return (JPanel) popup.getComponent(0);
    }

    private JPanel buildCalendar() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(Theme.SURFACE);

        JButton previous = nav("chevron-left", "Previous month", shown.canGoPrevious(), shown.previous());
        JButton next = nav("chevron-right", "Next month", shown.canGoNext(), shown.next());
        JLabel title = new JLabel(shown.title(), SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, Type.BODY));
        title.setName("month title");
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(previous, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);
        header.add(next, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(MonthGridModel.WEEKS + 1, 7, 2, 2));
        grid.setOpaque(false);
        for (String name : shown.weekdayNames()) {
            JLabel label = new JLabel(name, SwingConstants.CENTER);
            label.setFont(label.getFont().deriveFont(Type.CAPTION));
            label.setForeground(Theme.MUTED);
            grid.add(label);
        }
        for (java.util.List<MonthGridModel.Day> week : shown.weeks()) {
            for (MonthGridModel.Day day : week) {
                grid.add(dayButton(day));
            }
        }
        panel.add(grid, BorderLayout.CENTER);
        return panel;
    }

    private JButton nav(String icon, String tip, boolean enabled, MonthGridModel target) {
        JButton button = new JButton(Icons.muted(icon, 16));
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.setToolTipText(tip);
        button.setName(tip);
        button.getAccessibleContext().setAccessibleName(tip);
        button.setEnabled(enabled);
        button.addActionListener(e -> {
            shown = target;
            popup.removeAll();
            popup.add(buildCalendar());
            popup.revalidate();
            popup.pack();
            popup.repaint();
        });
        return button;
    }

    private JComponent dayButton(MonthGridModel.Day day) {
        if (!day.inMonth()) {
            return new JLabel();
        }
        JButton button = new JButton(String.valueOf(day.date().getDayOfMonth()));
        button.setName(day.date().toString());
        button.setPreferredSize(new Dimension(36, 32));
        button.putClientProperty("JButton.buttonType", "borderless");
        button.getAccessibleContext().setAccessibleName(day.date().toString());
        button.setEnabled(day.enabled());
        if (day.selected()) {
            button.putClientProperty("FlatLaf.styleClass", "primary day");
        } else if (day.today()) {
            // today is bold and blue, so it does not depend on colour alone
            button.putClientProperty("FlatLaf.styleClass", "today day");
        } else {
            button.putClientProperty("FlatLaf.styleClass", "day");
        }
        button.addActionListener(e -> {
            setDate(day.date());
            popup.setVisible(false);
            requestFocusInWindow();
            // choosing a day counts like pressing Enter in the field
            postActionEvent();
        });
        return button;
    }
}
