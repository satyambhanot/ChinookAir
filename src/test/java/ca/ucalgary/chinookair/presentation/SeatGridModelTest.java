package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Aircraft;
import ca.ucalgary.chinookair.business.entity.Seat;
import ca.ucalgary.chinookair.business.entity.SeatClass;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import ca.ucalgary.chinookair.business.entity.SeatLayout;
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

    @Test
    void sixSeatsPerRowHaveTheAisleAfterCAndFourAfterB() {
        assertEquals(3, SeatGridModel.of(aircraft(30, 6), List.of()).aisleAfter());
        assertEquals(2, SeatGridModel.of(aircraft(20, 4), List.of()).aisleAfter());
    }

    @Test
    void shortRowsHaveNoAisle() {
        assertEquals(-1, SeatGridModel.of(aircraft(10, 3), List.of()).aisleAfter());
        assertEquals(-1, SeatGridModel.of(aircraft(10, 2), List.of()).aisleAfter());
    }

    @Test
    void anArrowKeyMovesToTheNextFreeSeatInThatDirection() {
        SeatGridModel grid = SeatGridModel.of(aircraft(4, 4), freeExcept(Set.of("2A", "2C")));
        // 1B has a free seat to its right and left, and below it
        assertEquals(List.of(0, 2), asList(grid.nextEnabled(0, 1, 0, 1)));
        assertEquals(List.of(0, 0), asList(grid.nextEnabled(0, 1, 0, -1)));
        assertEquals(List.of(1, 1), asList(grid.nextEnabled(0, 1, 1, 0)));
        // from 1A down skips the taken 2A and lands on 3A
        assertEquals(List.of(2, 0), asList(grid.nextEnabled(0, 0, 1, 0)));
        // from 1C right goes to 1D; nothing is beyond the last column
        assertEquals(List.of(0, 3), asList(grid.nextEnabled(0, 2, 0, 1)));
        assertNull(grid.nextEnabled(0, 3, 0, 1));
        assertNull(grid.nextEnabled(0, 0, -1, 0));
    }

    private static Aircraft aircraft(int rows, int perRow) {
        return new Aircraft(1L, "Test plane", rows, perRow);
    }

    private static List<Seat> freeExcept(Set<String> taken) {
        return SeatLayout.generate(aircraft(4, 4)).stream().filter(s -> !taken.contains(s.number())).toList();
    }

    private static List<Integer> asList(int[] cell) {
        return List.of(cell[0], cell[1]);
    }
}
