package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Seat;
import ca.ucalgary.chinookair.business.entity.SeatClass;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

/**
 * The seat map of one flight: a grid of seat buttons with the row numbers, the seat letters, a heading
 * over each cabin and a gap for the aisle. A taken seat is disabled and crossed; the arrow keys move
 * from seat to seat. The panel tells its owner which seat was picked and knows nothing about booking.
 */
class SeatMapPanel extends JPanel {

    static final Color BUSINESS = Palette.GOLD_100;
    static final Color ECONOMY = Theme.ACCENT_SOFT;
    static final Color TAKEN = Palette.TAKEN;

    private static final long serialVersionUID = 1L;
    private static final int SEAT_WIDTH = 56;
    private static final int SEAT_HEIGHT = 30;
    private static final int AISLE_WIDTH = 28;

    private transient SeatGridModel grid;
    private transient JToggleButton[][] buttons = new JToggleButton[0][0];
    private final transient Map<String, JToggleButton> seatButtons = new HashMap<>();
    private transient ButtonGroup group = new ButtonGroup();
    private transient Flight flight;
    private transient Consumer<Seat> onChosen;

    SeatMapPanel() {
        super(new GridBagLayout());
        setOpaque(false);
    }

    /** Draws the map of a flight; {@code onChosen} is told each time a free seat is picked. */
    void show(Flight flight, SeatGridModel grid, Consumer<Seat> onChosen) {
        this.flight = flight;
        this.grid = grid;
        this.onChosen = onChosen;
        removeAll();
        seatButtons.clear();
        group = new ButtonGroup();
        buttons = new JToggleButton[grid.rowCount()][grid.columnCount()];

        int aisle = grid.aisleAfter();
        int last = grid.columnCount() + (aisle >= 0 ? 1 : 0) + 1;
        GridBagConstraints at = new GridBagConstraints();
        at.insets = new Insets(2, 2, 2, 2);
        at.fill = GridBagConstraints.BOTH;
        for (int c = 0; c < grid.columnCount(); c++) {
            at.gridx = 1 + column(c, aisle);
            at.gridy = 0;
            add(centred(String.valueOf(SeatGridModel.letter(c)), Theme.MUTED), at);
        }
        int y = 1;
        for (int r = 0; r < grid.rowCount(); r++) {
            if (r == 0 || grid.seatAt(r, 0).seatClass() != grid.seatAt(r - 1, 0).seatClass()) {
                at.gridy = y++;
                at.gridx = 0;
                at.gridwidth = last + 1;
                at.insets = new Insets(r == 0 ? Space.S : Space.L, 2, Space.XS, 2);
                add(cabinLabel(r), at);
                at.gridwidth = 1;
                at.insets = new Insets(2, 2, 2, 2);
            }
            at.gridy = y++;
            at.gridx = 0;
            add(centred(String.valueOf(r + 1), Theme.MUTED), at);
            at.gridx = last;
            add(centred(String.valueOf(r + 1), Theme.MUTED), at);
            for (int c = 0; c < grid.columnCount(); c++) {
                at.gridx = 1 + column(c, aisle);
                add(seatButton(grid.seatAt(r, c), grid.isEnabled(r, c), r, c), at);
            }
        }
        if (aisle >= 0) {
            at.gridx = 1 + aisle;
            at.gridy = 0;
            add(Box.createHorizontalStrut(AISLE_WIDTH), at);
        }
        revalidate();
        repaint();
    }

    /** Takes the seats off the screen, for when the map could not be loaded. */
    void clear() {
        removeAll();
        revalidate();
    }

    SeatGridModel grid() {
        return grid;
    }

    JToggleButton seatButtonFor(String number) {
        return seatButtons.get(number);
    }

    static String className(Seat seat) {
        return seat.seatClass() == SeatClass.BUSINESS ? "Business" : "Economy";
    }

    /** "BUSINESS, rows 1 to 2": a heading over the first row of a cabin. */
    private JLabel cabinLabel(int firstRow) {
        SeatClass cabin = grid.seatAt(firstRow, 0).seatClass();
        int lastRow = firstRow;
        while (lastRow + 1 < grid.rowCount() && grid.seatAt(lastRow + 1, 0).seatClass() == cabin) {
            lastRow++;
        }
        String rows = firstRow == lastRow ? "row " + (firstRow + 1) : "rows " + (firstRow + 1) + " to " + (lastRow + 1);
        JLabel label = new JLabel((cabin == SeatClass.BUSINESS ? "Business" : "Economy") + " · " + rows);
        label.setFont(label.getFont().deriveFont(Font.BOLD, Type.CAPTION));
        label.setForeground(Theme.MUTED);
        return label;
    }

    /** The grid column of a seat column: seats after the aisle move one column to the right. */
    private static int column(int seatColumn, int aisleAfter) {
        return aisleAfter >= 0 && seatColumn >= aisleAfter ? seatColumn + 1 : seatColumn;
    }

    private static JLabel centred(String text, Color color) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setForeground(color);
        label.setFont(label.getFont().deriveFont(Type.LABEL));
        label.setPreferredSize(new Dimension(28, SEAT_HEIGHT));
        return label;
    }

    private JToggleButton seatButton(Seat seat, boolean enabled, int row, int column) {
        // a taken seat shows a cross, so it differs from a free seat by more than its colour
        JToggleButton button = new JToggleButton(enabled ? seat.number() : "×");
        button.setOpaque(true);
        button.setBackground(enabled ? (seat.seatClass() == SeatClass.BUSINESS ? BUSINESS : ECONOMY) : TAKEN);
        button.setEnabled(enabled);
        button.setMargin(new Insets(2, 2, 2, 2));
        button.setPreferredSize(new Dimension(SEAT_WIDTH, SEAT_HEIGHT));
        button.setFont(button.getFont().deriveFont(Type.LABEL));
        String price = enabled ? Format.money(flight.priceFor(seat.seatClass())) : "";
        button.getAccessibleContext().setAccessibleName("Seat " + seat.number()
                + (enabled ? ", " + className(seat) + ", " + price : ", taken"));
        button.setToolTipText(enabled ? seat.number() + " · " + className(seat) + " · " + price
                : "Seat " + seat.number() + " is taken");
        // the chosen seat turns blue, so it is easy to find in the grid
        button.putClientProperty("FlatLaf.styleClass", "seat");
        if (enabled) {
            button.addActionListener(e -> onChosen.accept(seat));
            bindArrows(button, row, column);
        }
        group.add(button);
        seatButtons.put(seat.number(), button);
        buttons[row][column] = button;
        return button;
    }

    /** The arrow keys move from seat to seat, skipping the seats that are taken. */
    private void bindArrows(JToggleButton button, int row, int column) {
        int[][] steps = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        String[] keys = {"UP", "DOWN", "LEFT", "RIGHT"};
        for (int i = 0; i < keys.length; i++) {
            int[] step = steps[i];
            String name = "seat-" + keys[i];
            button.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(keys[i]), name);
            button.getActionMap().put(name, new AbstractAction() {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    int[] next = grid.nextEnabled(row, column, step[0], step[1]);
                    if (next != null) {
                        buttons[next[0]][next[1]].requestFocusInWindow();
                    }
                }
            });
        }
    }
}
