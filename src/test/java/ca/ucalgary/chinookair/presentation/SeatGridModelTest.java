package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Aircraft;
import ca.ucalgary.chinookair.business.entity.Seat;
import ca.ucalgary.chinookair.business.entity.SeatClass;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SeatGridModelTest {

    private static final Aircraft THREE_BY_FOUR = new Aircraft(1L, "Small", 3, 4);

    private static List<Seat> everySeatFreeExcept(String... taken) {
        List<Seat> free = new ArrayList<>();
        for (int row = 1; row <= 3; row++) {
            for (char letter = 'A'; letter <= 'D'; letter++) {
                String number = row + String.valueOf(letter);
                if (!List.of(taken).contains(number)) {
                    free.add(new Seat(number, row <= 2 ? SeatClass.BUSINESS : SeatClass.ECONOMY, true));
                }
            }
        }
        return free;
    }

    @Test
    void aThreeByFourAircraftHasThreeRowsAndFourColumns() {
        SeatGridModel grid = SeatGridModel.of(THREE_BY_FOUR, everySeatFreeExcept());
        assertEquals(3, grid.rowCount());
        assertEquals(4, grid.columnCount());
    }

    @Test
    void theCellOfSecondRowAndSecondColumnIsSeat2B() {
        SeatGridModel grid = SeatGridModel.of(THREE_BY_FOUR, everySeatFreeExcept());
        assertEquals("2B", grid.seatAt(1, 1).number());
        assertEquals("1A", grid.seatAt(0, 0).number());
        assertEquals("3D", grid.seatAt(2, 3).number());
    }

    @Test
    void aTakenSeatIsStillOnTheMapButDisabled() {
        SeatGridModel grid = SeatGridModel.of(THREE_BY_FOUR, everySeatFreeExcept("2B"));
        assertEquals("2B", grid.seatAt(1, 1).number());
        assertFalse(grid.isEnabled(1, 1));
        assertTrue(grid.isEnabled(1, 0));
    }

    @Test
    void theFirstTwoRowsAreBusinessAndTheRestEconomy() {
        SeatGridModel grid = SeatGridModel.of(THREE_BY_FOUR, everySeatFreeExcept());
        assertEquals(SeatClass.BUSINESS, grid.seatAt(1, 2).seatClass());
        assertEquals(SeatClass.ECONOMY, grid.seatAt(2, 2).seatClass());
    }

    @Test
    void theColumnsAreHeadedByLetters() {
        assertEquals('A', SeatGridModel.letter(0));
        assertEquals('D', SeatGridModel.letter(3));
    }

    @Test
    void anEmptyListMakesAnEmptyMap() {
        SeatGridModel grid = new SeatGridModel(List.of());
        assertEquals(0, grid.rowCount());
        assertEquals(0, grid.columnCount());
    }
}
