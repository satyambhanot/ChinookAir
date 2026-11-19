package ca.ucalgary.chinookair.data;

import ca.ucalgary.chinookair.business.entity.Seat;
import java.util.List;

/**
 * The seats of a flight. A seat is available when no active reservation (pending payment or
 * confirmed) holds it; this is worked out from the reservations, it is never stored.
 */
public interface SeatDao {

    /** Adds all the seats of a new flight, all or none. */
    void createSeats(long flightId, List<Seat> seats);

    /**
     * Swaps all the seats of a flight for new ones, all or none. Only possible while no reservation of
     * any status refers to a seat of the flight, otherwise the database refuses and nothing changes.
     */
    void replaceSeats(long flightId, List<Seat> seats);

    /** The seats in row order, each with its availability. */
    List<Seat> findByFlight(long flightId);

    int countAvailable(long flightId);
}
