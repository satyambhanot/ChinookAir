package ca.ucalgary.chinookair.presentation;

import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.util.function.Supplier;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * A day and a time of day in two boxes side by side: the day has a calendar, the time is typed as
 * "14:30". Together they read and write the same text a single box would, "2026-12-02 14:30", so the
 * screens parse it the way they always did.
 */
final class DateTimeField extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int TIME_COLUMNS = 5;

    final DatePickerField date;
    final JTextField time = Theme.placeholder(new JTextField(TIME_COLUMNS), "HH:mm");

    DateTimeField(Supplier<LocalDate> today) {
        super();
        this.date = new DatePickerField(10, today);
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(date);
        add(Box.createHorizontalStrut(Space.S));
        add(time);
        date.getAccessibleContext().setAccessibleName("Day");
        time.getAccessibleContext().setAccessibleName("Time, hours and minutes");
    }

    /** The day, a space and the time; just the day while no time is typed, so a mistake is explained as before. */
    String getText() {
        String day = date.getText().trim();
        String clock = time.getText().trim();
        return clock.isEmpty() ? day : day + " " + clock;
    }

    /** Fills both boxes from text like "2026-12-02 14:30"; text without a space fills only the day. */
    void setText(String text) {
        String value = text == null ? "" : text.trim();
        int space = value.indexOf(' ');
        date.setText(space < 0 ? value : value.substring(0, space));
        time.setText(space < 0 ? "" : value.substring(space + 1).trim());
    }

    /** Hears Enter in the time box or a day picked from the calendar. */
    void addActionListener(ActionListener listener) {
        date.addActionListener(listener);
        time.addActionListener(listener);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        date.setEnabled(enabled);
        time.setEnabled(enabled);
    }

    @Override
    public boolean requestFocusInWindow() {
        return date.requestFocusInWindow();
    }
}
