package ca.ucalgary.chinookair.business.control;

import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Payment;
import ca.ucalgary.chinookair.business.entity.PaymentStatus;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.ReservationStatus;
import ca.ucalgary.chinookair.business.entity.Role;
import ca.ucalgary.chinookair.business.entity.Seat;
import ca.ucalgary.chinookair.business.entity.User;
import ca.ucalgary.chinookair.data.FlightDao;
import ca.ucalgary.chinookair.data.PaymentDao;
import ca.ucalgary.chinookair.data.ReservationDao;
import ca.ucalgary.chinookair.data.SeatDao;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

/**
 * Use cases View Reservation History, Cancel Reservation and Modify Reservation. A reservation, its
 * seat and its payments always change together: a cancelled reservation frees its seat and its paid
 * payments are refunded.
 */
public class ReservationController {

    private final ReservationDao reservations;
    private final PaymentDao payments;
    private final FlightDao flights;
    private final SeatDao seats;
    private final Clock clock;

    public ReservationController(ReservationDao reservations, PaymentDao payments, FlightDao flights, SeatDao seats,
                                 Clock clock) {
        this.reservations = reservations;
        this.payments = payments;
        this.flights = flights;
        this.seats = seats;
        this.clock = clock;
    }

    /**
     * The reservations of a customer, newest first, for the customer themself or an agent. A confirmed
     * reservation whose flight has left is first marked as completed.
     *
     * @throws AccessDeniedException if the actor may not see them
     */
    public List<Reservation> history(User actor, long customerId) {
        Authorizer.require(actor, Role.CUSTOMER, Role.AGENT);
        if (actor.role() == Role.CUSTOMER && !Objects.equals(actor.id(), customerId)) {
            throw new AccessDeniedException("You can only see your own reservations");
        }
        List<Reservation> history = reservations.findByCustomer(customerId);
        for (Reservation reservation : history) {
            if (reservation.status() == ReservationStatus.CONFIRMED && reservation.flight().hasDeparted(clock)) {
                reservation.complete(clock);
                reservations.update(reservation);
            }
        }
        return history;
    }

    /**
     * Cancels a reservation until its flight departs. The seat is free again and every approved
     * payment is refunded in full. A customer may cancel their own, an agent anyone's.
     *
     * @throws AccessDeniedException    if the actor may not cancel this reservation
     * @throws IllegalStateException    if the flight has departed or the reservation is already cancelled
     */
    public Reservation cancel(User actor, long reservationId) {
        Authorizer.require(actor, Role.CUSTOMER, Role.AGENT);
        Reservation reservation = reservation(reservationId);
        if (actor.role() == Role.CUSTOMER && !Objects.equals(actor.id(), reservation.customer().id())) {
            throw new AccessDeniedException("You can only cancel your own reservations");
        }
        if (reservation.flight().hasDeparted(clock)) {
            throw new IllegalStateException("That flight has already departed");
        }
        reservation.cancel();
        reservations.update(reservation);
        refundApprovedPayments(reservation);
        return reservation;
    }

    /** True if the reservation can still be cancelled: it is waiting for payment or confirmed and has not left. */
    public boolean canCancel(Reservation reservation) {
        ReservationStatus status = reservation.status();
        return (status == ReservationStatus.PENDING_PAYMENT || status == ReservationStatus.CONFIRMED)
                && !reservation.flight().hasDeparted(clock);
    }

    /**
     * Moves a confirmed reservation to another flight and seat. Only an agent may do this. If the new
     * price is the same the reservation stays confirmed; if it differs, the old payment is refunded and
     * the reservation waits for a new payment. A seat that was taken in the meantime is refused by the
     * database before anything else changes, and the reservation stays as it was.
     *
     * @throws AccessDeniedException    if the actor is not an agent
     * @throws IllegalArgumentException if the reservation, the flight or the seat does not exist
     * @throws IllegalStateException    if the reservation is not confirmed or a flight has departed
     * @throws ca.ucalgary.chinookair.business.entity.SeatUnavailableException if the seat is taken
     */
    public Reservation modify(User actor, long reservationId, long newFlightId, String newSeatNo) {
        Authorizer.require(actor, Role.AGENT);
        Reservation reservation = reservation(reservationId);
        if (reservation.status() != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only a confirmed reservation can be changed");
        }
        if (reservation.flight().hasDeparted(clock)) {
            throw new IllegalStateException("That flight has already departed");
        }
        Flight newFlight = flights.findById(newFlightId)
                .orElseThrow(() -> new IllegalArgumentException("The new flight does not exist"));
        if (flights.isArchived(newFlightId)) {
            throw new IllegalStateException("The new flight is no longer available");
        }
        if (newFlight.hasDeparted(clock)) {
            throw new IllegalStateException("The new flight has already departed");
        }
        String wanted = newSeatNo == null ? "" : newSeatNo.trim().toUpperCase();
        Seat seat = seats.findByFlight(newFlightId).stream().filter(s -> s.number().equals(wanted)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Seat " + wanted + " does not exist on this flight"));

        reservation.changeFlight(newFlight, seat.number(), newFlight.priceFor(seat.seatClass()));
        // the update comes first: the database rejects a taken seat here, before any payment is touched
        reservations.update(reservation);
        if (reservation.status() == ReservationStatus.PENDING_PAYMENT) {
            refundApprovedPayments(reservation);
        }
        return reservation;
    }

    // ---- helpers

    private Reservation reservation(long reservationId) {
        return reservations.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("That reservation does not exist"));
    }

    private void refundApprovedPayments(Reservation reservation) {
        for (Payment payment : payments.findByReservation(reservation.id())) {
            if (payment.status() == PaymentStatus.APPROVED) {
                payment.refund();
                payments.update(payment);
            }
        }
    }
}
