package ca.ucalgary.chinookair.presentation;

import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

/**
 * The chosen day and the three days either side of it, each with the lowest price that day, so a
 * traveller can see that moving a day is cheaper. A day in the past cannot be picked.
 */
final class DayStrip extends JPanel {

    private static final long serialVersionUID = 1L;
    /** How many days are shown on each side of the chosen one. */
    static final int AROUND = 3;

    private final transient List<JToggleButton> days = new ArrayList<>();
    private final transient List<LocalDate> dates = new ArrayList<>();
    private transient Consumer<LocalDate> onPick = day -> { };

    DayStrip() {
        super(new GridLayout(1, 2 * AROUND + 1, Space.S, 0));
        setOpaque(false);
        getAccessibleContext().setAccessibleName("Nearby days");
    }

    void onPick(Consumer<LocalDate> listener) {
        this.onPick = listener;
    }

    /** Shows the days around {@code chosen}; {@code lowest} says what the cheapest flight costs on a day, if any. */
    void show(LocalDate chosen, LocalDate today, Function<LocalDate, Optional<BigDecimal>> lowest) {
        removeAll();
        days.clear();
        dates.clear();
        ButtonGroup group = new ButtonGroup();
        for (int offset = -AROUND; offset <= AROUND; offset++) {
            LocalDate day = chosen.plusDays(offset);
            Optional<BigDecimal> price = day.isBefore(today) ? Optional.empty() : lowest.apply(day);
            String cost = price.isPresent() ? Format.money(price.get()) : "–";
            JToggleButton button = Theme.day(new JToggleButton("<html><div style='text-align:center'>"
                    + Format.shortDate(day) + "<br><b>" + cost + "</b></div></html>"));
            button.setEnabled(!day.isBefore(today));
            button.setSelected(offset == 0);
            button.getAccessibleContext().setAccessibleName(Format.date(day)
                    + (price.isPresent() ? ", from " + cost : ", no flights"));
            if (offset != 0) {
                button.addActionListener(e -> onPick.accept(day));
            }
            group.add(button);
            add(button);
            days.add(button);
            dates.add(day);
        }
        revalidate();
        repaint();
    }

    /** The day buttons, earliest first, for tests. */
    List<JToggleButton> days() {
        return List.copyOf(days);
    }

    /** The day each button stands for. */
    List<LocalDate> dates() {
        return List.copyOf(dates);
    }
}
