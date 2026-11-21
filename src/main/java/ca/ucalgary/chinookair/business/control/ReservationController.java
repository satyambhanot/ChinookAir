package ca.ucalgary.chinookair.business.control;

import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Payment;
import ca.ucalgary.chinookair.business.entity.PaymentStatus;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.ReservationStatus;
import ca.ucalgary.chinookair.business.entity.Role;
import ca.ucalgary.chinookair.business.entity.Seat;
import ca.ucalgary.chinookair.business.entity.User;
import ca.ucalgary.chinookair.data.ConfirmationWriteException;
import ca.ucalgary.chinookair.data.ConfirmationWriter;
import ca.ucalgary.chinookair.data.FlightDao;
import ca.ucalgary.chinookair.data.PaymentDao;
import ca.ucalgary.chinookair.data.ReservationDao;
import ca.ucalgary.chinookair.data.SeatDao;
import ca.ucalgary.chinookair.data.UnitOfWork;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Use cases View Reservation History, Cancel Reservation and Modify Reservation. A reservation, its
 * seat and its payments always change together: a cancelled reservation frees its seat and its paid
 * payments are refunded.
 */
public class ReservationController {

    private static final Logger LOG = Logger.getLogger(ReservationController.class.getName());

    private final ReservationDao reservations;
    private final PaymentDao payments;
    private final FlightDao flights;
    private final SeatDao seats;
    private final ConfirmationWriter writer;
    private final UnitOfWork transactions;
    private final Clock clock;

    public ReservationController(ReservationDao reservations, PaymentDao payments, FlightDao flights, SeatDao seats,
                                 ConfirmationWriter writer, UnitOfWork transactions, Clock clock) {
        this.reservations = reservations;
        this.payments = payments;
        this.flights = flights;
        this.seats = seats;
        this.writer = writer;
        this.transactions = transactions;
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
        // the seat is freed and the money goes back together, or nothing changes
        transactions.run(() -> {
            reservations.update(reservation);
            refundApprovedPayments(reservation);
        });
        return reservation;
    }

    /**
     * True if the reservation can still be cancelled: it was started, is waiting for payment or is confirmed,
     * and its flight has not left. A reservation that was started but never got a seat can be cancelled too,
     * so none is left behind when a booking is abandoned.
     */
    public boolean canCancel(Reservation reservation) {
        ReservationStatus status = reservation.status();
        return (status == ReservationStatus.CREATED || status == ReservationStatus.PENDING_PAYMENT
                || status == ReservationStatus.CONFIRMED) && !reservation.flight().hasDeparted(clock);
    }

    /** True if an agent can still move the reservation: it is confirmed and its flight has not left. */
    public boolean canModify(Reservation reservation) {
        return reservation.status() == ReservationStatus.CONFIRMED && !reservation.flight().hasDeparted(clock);
    }

    /**
     * Moves a confirmed reservation to another flight and seat. Only an agent may do this. If the new
     * price is the same the reservation stays confirmed; if it differs, the old payment is refunded and
     * the reservation waits for a new payment. A seat that was taken in the meantime is refused by the
     * database before anything else changes, and the reservation stays as it was. If the price is the same
     * the confirmation file is written again with the new flight and seat; a problem with that file is
     * logged but does not undo the change.
     *
     * @throws AccessDeniedException    if the actor is not an agent
     * @throws IllegalArgumentException if the reservation, the flight or the seat does not exist, or the
     *                                  reservation already has that flight and seat
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
        if (newFlightId == reservation.flight().id() && wanted.equals(reservation.seatNo())) {
            throw new IllegalArgumentException("The reservation already has that flight and seat");
        }
        Seat seat = seats.findByFlight(newFlightId).stream().filter(s -> s.number().equals(wanted)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Seat " + wanted + " does not exist on this flight"));

        reservation.changeFlight(newFlight, seat.number(), newFlight.priceFor(seat.seatClass()));
        // the update comes first: the database rejects a taken seat here, before any payment is touched
        transactions.run(() -> {
            reservations.update(reservation);
            if (reservation.status() == ReservationStatus.PENDING_PAYMENT) {
                refundApprovedPayments(reservation);
            }
        });
        if (reservation.status() == ReservationStatus.CONFIRMED) {
            rewriteConfirmation(reservation);
        }
        return reservation;
    }

    /** The price did not change, so the paid confirmation stays valid; its file has to show the new trip. */
    private void rewriteConfirmation(Reservation reservation) {
        payments.findByReservation(reservation.id()).stream()
                .filter(p -> p.status() == PaymentStatus.APPROVED).findFirst().ifPresent(payment -> {
                    try {
                        writer.write(reservation, payment);
                    } catch (ConfirmationWriteException e) {
                        LOG.log(Level.WARNING, "The confirmation file could not be written again", e);
                    }
                });
    }

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
