package ca.ucalgary.chinookair.data;

import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.SeatUnavailableException;
import java.util.List;
import java.util.Optional;

/**
 * Loads and saves reservations. The database allows only one pending or confirmed
 * reservation per seat, so two people can never hold the same seat.
 */
public interface ReservationDao {

    /**
     * Adds a new reservation, gives it its id and returns it. The customer and the flight must be saved.
     *
     * @throws IllegalArgumentException if it already has an id
     * @throws SeatUnavailableException if another active reservation holds the seat
     * @throws DataAccessException      for any other database problem, such as a confirmation number that is
     *                                  already used
     */
    Reservation save(Reservation reservation);

    /**
     * Writes the flight, seat, price and status of a saved reservation.
     *
     * @throws SeatUnavailableException if another active reservation holds the seat
     */
    void update(Reservation reservation);

    Optional<Reservation> findById(long reservationId);

    Optional<Reservation> findByConfirmationNumber(String confirmationNumber);

    /** The reservations of a customer, newest first. */
    List<Reservation> findByCustomer(long customerId);

    /** The reservations on a flight, oldest first. */
    List<Reservation> findByFlight(long flightId);

    /** How many reservations on the flight are still active: created, pending payment or confirmed. */
    int countActiveByFlight(long flightId);
}
