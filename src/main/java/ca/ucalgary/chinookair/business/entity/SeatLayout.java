package ca.ucalgary.chinookair.business.entity;

import java.util.ArrayList;
import java.util.List;

/** Works out which seats an aircraft has. The layout is never stored, it follows from rows and seats per row. */
public final class SeatLayout {

    /** The first rows are business class. */
    public static final int BUSINESS_ROWS = 2;

    private SeatLayout() {
    }

    /** Every seat of the aircraft, row by row, named like 12A. All of them are available. */
    public static List<Seat> generate(Aircraft aircraft) {
        List<Seat> seats = new ArrayList<>();
        for (int row = 1; row <= aircraft.rows(); row++) {
            SeatClass seatClass = row <= BUSINESS_ROWS ? SeatClass.BUSINESS : SeatClass.ECONOMY;
            for (int k = 0; k < aircraft.seatsPerRow(); k++) {
                char letter = (char) ('A' + k);
                seats.add(new Seat(row + String.valueOf(letter), seatClass, true));
            }
        }
        return seats;
    }
}
