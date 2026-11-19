package ca.ucalgary.chinookair.business.control;

import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.ReservationStatus;
import ca.ucalgary.chinookair.business.entity.Role;
import ca.ucalgary.chinookair.business.entity.Seat;
import ca.ucalgary.chinookair.business.entity.User;
import ca.ucalgary.chinookair.data.FlightDao;
import ca.ucalgary.chinookair.data.ReservationDao;
import ca.ucalgary.chinookair.data.SeatDao;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Use case Book Flight: starts a reservation, lists the free seats, takes the chosen seat and
 * can give up a reservation that is not paid yet. It has no wording for the screen: the view
 * turns the exceptions into messages, including {@code SeatUnavailableException} when two
 * people pick the same seat.
 */
public class BookingController {

    private static final int NUMBER_ATTEMPTS = 5;

    private final FlightDao flights;
    private final SeatDao seats;
    private final ReservationDao reservations;
    private final ConfirmationNumberGenerator numbers;
    private final Clock clock;

    public BookingController(FlightDao flights, SeatDao seats, ReservationDao reservations,
                             ConfirmationNumberGenerator numbers, Clock clock) {
        this.flights = flights;
        this.seats = seats;
        this.reservations = reservations;
        this.numbers = numbers;
        this.clock = clock;
    }

    /**
     * Creates a reservation without a seat. A customer may only reserve for themselves, a flight
     * agent for anybody, and an administrator not at all.
     *
     * @throws AccessDeniedException    if the actor may not reserve for that customer
     * @throws IllegalArgumentException if the flight does not exist
     * @throws IllegalStateException    if the flight has already departed or was removed
     */
    public Reservation startReservation(User actor, Customer customer, long flightId) {
        Authorizer.require(actor, Role.CUSTOMER, Role.AGENT);
        if (customer == null || customer.id() == null) {
            throw new IllegalArgumentException("A saved customer is required");
        }
        if (actor.role() == Role.CUSTOMER && !Objects.equals(actor.id(), customer.id())) {
            throw new AccessDeniedException("You can only reserve for yourself");
        }
        Flight flight = bookableFlight(flightId);
        Reservation reservation = new Reservation(freshConfirmationNumber(), customer, flight,
                LocalDateTime.now(clock));
        return reservations.save(reservation);
    }

    /** The seats of the flight that nobody holds, in row order. */
    public List<Seat> availableSeats(long flightId) {
        requireFlight(flightId);
        return seats.findByFlight(flightId).stream().filter(Seat::available).toList();
    }

    /**
     * Gives the reservation the chosen seat at its price and moves it to pending payment. If another
     * reservation took the seat first, the reservation stays as it was and the
     * {@code SeatUnavailableException} of the database passes up unchanged.
     *
     * @throws IllegalArgumentException if the reservation or the seat does not exist
     * @throws IllegalStateException    if the flight has departed or the reservation already has a seat
     */
    public Reservation selectSeat(long reservationId, String seatNo) {
        Reservation reservation = reservation(reservationId);
        Flight flight = reservation.flight();
        if (flight.hasDeparted(clock)) {
            throw new IllegalStateException("That flight has already departed");
        }
        String wanted = seatNo == null ? "" : seatNo.trim().toUpperCase();
        Seat seat = seats.findByFlight(flight.id()).stream().filter(s -> s.number().equals(wanted)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Seat " + wanted + " does not exist on this flight"));
        reservation.assignSeat(seat.number(), flight.priceFor(seat.seatClass()));
        reservations.update(reservation);
        return reservation;
    }

    /**
     * Gives up a reservation that is not paid yet and frees its seat.
     *
     * @throws IllegalStateException if the reservation is already paid, cancelled or completed
     */
    public void cancelPending(long reservationId) {
        Reservation reservation = reservation(reservationId);
        ReservationStatus status = reservation.status();
        if (status != ReservationStatus.CREATED && status != ReservationStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Only a reservation that is not paid yet can be given up");
        }
        reservation.cancel();
        reservations.update(reservation);
    }

    // ---- helpers

    private Flight requireFlight(long flightId) {
        return flights.findById(flightId).orElseThrow(() -> new IllegalArgumentException("That flight does not exist"));
    }

    private Flight bookableFlight(long flightId) {
        Flight flight = requireFlight(flightId);
        if (flights.isArchived(flightId)) {
            throw new IllegalStateException("That flight is no longer available");
        }
        if (flight.hasDeparted(clock)) {
            throw new IllegalStateException("That flight has already departed");
        }
        return flight;
    }

    private Reservation reservation(long reservationId) {
        return reservations.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("That reservation does not exist"));
    }

    /** A number nobody has used yet; a clash is rare, so a few tries are enough. */
    private String freshConfirmationNumber() {
        for (int attempt = 0; attempt < NUMBER_ATTEMPTS; attempt++) {
            String number = numbers.next();
            if (reservations.findByConfirmationNumber(number).isEmpty()) {
                return number;
            }
        }
        throw new IllegalStateException("Could not make a confirmation number, please try again");
    }
}
