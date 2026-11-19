package ca.ucalgary.chinookair.business.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SeatLayoutTest {

    private static long count(List<Seat> seats, SeatClass seatClass) {
        return seats.stream().filter(s -> s.seatClass() == seatClass).count();
    }

    @Test
    void threeRowsOfFourGivesTwelveSeats() {
        List<Seat> seats = SeatLayout.generate(new Aircraft(null, "Small", 3, 4));
        assertEquals(12, seats.size());
        assertEquals("1A", seats.get(0).number());
        assertEquals("3D", seats.get(11).number());
        assertEquals(8, count(seats, SeatClass.BUSINESS));
        assertEquals(4, count(seats, SeatClass.ECONOMY));
        assertTrue(seats.stream().allMatch(Seat::available));
    }

    @Test
    void firstTwoRowsAreBusiness() {
        List<Seat> seats = SeatLayout.generate(new Aircraft(null, "A320", 30, 6));
        assertEquals(180, seats.size());
        assertEquals(SeatClass.BUSINESS, seats.get(11).seatClass());   // 2F
        assertEquals("3A", seats.get(12).number());
        assertEquals(SeatClass.ECONOMY, seats.get(12).seatClass());
    }

    @Test
    void anAircraftWithOneRowIsAllBusiness() {
        List<Seat> seats = SeatLayout.generate(new Aircraft(null, "Tiny", 1, 2));
        assertEquals(2, seats.size());
        assertEquals(2, count(seats, SeatClass.BUSINESS));
    }

    @Test
    void numbersAreUnique() {
        List<Seat> seats = SeatLayout.generate(new Aircraft(null, "Big", 60, 6));
        assertEquals(360, seats.stream().map(Seat::number).distinct().count());
        assertEquals("60F", seats.get(359).number());
    }
}
