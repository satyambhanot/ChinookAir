package ca.ucalgary.chinookair.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Seat;
import ca.ucalgary.chinookair.business.entity.SeatClass;
import ca.ucalgary.chinookair.business.entity.SeatLayout;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqliteSeatDaoTest {

    private Connection db;
    private TestWorld world;
    private SeatDao seats;
    private long flightId;

    @BeforeEach
    void open() {
        db = TestDatabase.inMemory();
        world = new TestWorld(db);
        seats = new SqliteSeatDao(db);
        Flight flight = new SqliteFlightDao(db).save(
                world.flight("AC123", world.yycYvr, world.ac, world.small, LocalDateTime.of(2026, 12, 1, 8, 0)));
        flightId = flight.id();
        seats.createSeats(flightId, SeatLayout.generate(world.small));
    }

    @AfterEach
    void close() throws SQLException {
        db.close();
    }

    private Seat seat(String number) {
        return seats.findByFlight(flightId).stream().filter(s -> s.number().equals(number)).findFirst()
                .orElseThrow();
    }

    @Test
    void aNewFlightHasAllItsSeatsAvailable() {
        assertEquals(12, seats.countAvailable(flightId));
        List<Seat> all = seats.findByFlight(flightId);
        assertEquals(12, all.size());
        assertTrue(all.stream().allMatch(Seat::available));
    }

    @Test
    void seatsComeBackInRowOrderWithTheirClass() {
        List<Seat> all = seats.findByFlight(flightId);
        assertEquals("1A", all.get(0).number());
        assertEquals("3D", all.get(11).number());
        assertEquals(SeatClass.BUSINESS, seat("2D").seatClass());
        assertEquals(SeatClass.ECONOMY, seat("3A").seatClass());
    }

    @Test
    void rowTenComesAfterRowNine() {
        Flight big = new SqliteFlightDao(db).save(
                world.flight("AC200", world.yycYvr, world.ac, world.big, LocalDateTime.of(2026, 12, 2, 8, 0)));
        seats.createSeats(big.id(), SeatLayout.generate(world.big));
        List<Seat> all = seats.findByFlight(big.id());
        assertEquals(180, all.size());
        assertEquals("9F", all.get(53).number());
        assertEquals("10A", all.get(54).number());
        assertEquals("30F", all.get(179).number());
    }

    @Test
    void aConfirmedReservationTakesTheSeatAndCancellingGivesItBack() {
        long reservation = world.reserve("CA000001", flightId, "2A", "CONFIRMED");
        assertEquals(11, seats.countAvailable(flightId));
        assertFalse(seat("2A").available());
        assertTrue(seat("2B").available());

        world.setStatus(reservation, "CANCELLED");
        assertEquals(12, seats.countAvailable(flightId));
        assertTrue(seat("2A").available());
    }

    @Test
    void aPendingPaymentAlsoHoldsTheSeat() {
        world.reserve("CA000001", flightId, "1A", "PENDING_PAYMENT");
        assertEquals(11, seats.countAvailable(flightId));
        assertFalse(seat("1A").available());
    }

    @Test
    void createdCancelledAndCompletedReservationsDoNotHoldASeat() {
        world.reserve("CA000001", flightId, null, "CREATED");
        world.reserve("CA000002", flightId, "1A", "CANCELLED");
        world.reserve("CA000003", flightId, "1B", "COMPLETED");
        assertEquals(12, seats.countAvailable(flightId));
    }

    @Test
    void aReservationOnAnotherFlightDoesNotMatter() {
        Flight other = new SqliteFlightDao(db).save(
                world.flight("AC456", world.yycYvr, world.ac, world.small, LocalDateTime.of(2026, 12, 2, 8, 0)));
        seats.createSeats(other.id(), SeatLayout.generate(world.small));
        world.reserve("CA000001", other.id(), "1A", "CONFIRMED");
        assertEquals(12, seats.countAvailable(flightId));
        assertEquals(11, seats.countAvailable(other.id()));
    }

    @Test
    void aFlightWithoutSeatsHasNoneAvailable() {
        assertEquals(0, seats.countAvailable(999));
        assertTrue(seats.findByFlight(999).isEmpty());
    }

    @Test
    void createSeatsIsAllOrNothing() {
        Flight other = new SqliteFlightDao(db).save(
                world.flight("AC456", world.yycYvr, world.ac, world.small, LocalDateTime.of(2026, 12, 2, 8, 0)));
        List<Seat> withDuplicate = List.of(new Seat("1A", SeatClass.BUSINESS, true),
                new Seat("1B", SeatClass.BUSINESS, true), new Seat("1A", SeatClass.BUSINESS, true));
        assertThrows(DataAccessException.class, () -> seats.createSeats(other.id(), withDuplicate));
        assertEquals(0, seats.findByFlight(other.id()).size());
    }

    @Test
    void replaceSeatsSwapsTheSeatsOfAFlight() {
        seats.replaceSeats(flightId, SeatLayout.generate(world.big));
        assertEquals(180, seats.findByFlight(flightId).size());
        assertEquals(180, seats.countAvailable(flightId));
        assertEquals("30F", seats.findByFlight(flightId).get(179).number());
    }

    @Test
    void replaceSeatsIsRefusedWhileAReservationUsesASeatAndKeepsTheOldSeats() {
        world.reserve("CA000001", flightId, "1A", "CANCELLED");
        assertThrows(DataAccessException.class, () -> seats.replaceSeats(flightId, SeatLayout.generate(world.big)));
        assertEquals(12, seats.findByFlight(flightId).size());
    }

    @Test
    void seatsNeedAFlightThatExists() {
        assertThrows(DataAccessException.class,
                () -> seats.createSeats(999, List.of(new Seat("1A", SeatClass.BUSINESS, true))));
    }
}
