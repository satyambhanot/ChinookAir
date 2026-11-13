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

    /** The seats in row order, each with its availability. */
    List<Seat> findByFlight(long flightId);

    int countAvailable(long flightId);
}
