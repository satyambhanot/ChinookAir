package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.entity.Aircraft;
import ca.ucalgary.chinookair.business.entity.Seat;
import ca.ucalgary.chinookair.business.entity.SeatLayout;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The seat map as rows and columns. It has no Swing in it, so the layout can be tested without a window.
 * Row 0 is seat row 1 and column 0 is letter A.
 */
final class SeatGridModel {

    private final Seat[][] cells;

    /** @param seats every seat of the aircraft, named like 12A, with its availability */
    SeatGridModel(List<Seat> seats) {
        int rows = 0;
        int columns = 0;
        for (Seat seat : seats) {
            rows = Math.max(rows, rowOf(seat));
            columns = Math.max(columns, columnOf(seat) + 1);
        }
        cells = new Seat[rows][columns];
        for (Seat seat : seats) {
            cells[rowOf(seat) - 1][columnOf(seat)] = seat;
        }
    }

    /** The map of an aircraft: every seat of its layout, available only if the free list has it. */
    static SeatGridModel of(Aircraft aircraft, List<Seat> freeSeats) {
        Set<String> free = new HashSet<>();
        for (Seat seat : freeSeats) {
            free.add(seat.number());
        }
        List<Seat> all = SeatLayout.generate(aircraft).stream()
                .map(s -> new Seat(s.number(), s.seatClass(), free.contains(s.number())))
                .toList();
        return new SeatGridModel(all);
    }

    int rowCount() {
        return cells.length;
    }

    int columnCount() {
        return cells.length == 0 ? 0 : cells[0].length;
    }

    /** The seat in a cell, or null if the aircraft has none there. */
    Seat seatAt(int row, int column) {
        return cells[row][column];
    }

    /** True if the seat in the cell can be picked. */
    boolean isEnabled(int row, int column) {
        Seat seat = cells[row][column];
        return seat != null && seat.available();
    }

    /** The letter that heads a column. */
    static char letter(int column) {
        return (char) ('A' + column);
    }

    private static int rowOf(Seat seat) {
        String number = seat.number();
        return Integer.parseInt(number.substring(0, number.length() - 1));
    }

    private static int columnOf(Seat seat) {
        String number = seat.number();
        return number.charAt(number.length() - 1) - 'A';
    }
}
