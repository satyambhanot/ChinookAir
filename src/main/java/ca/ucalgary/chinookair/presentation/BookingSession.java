package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.FlightOffer;
import ca.ucalgary.chinookair.business.control.PaymentResult;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Reservation;
import java.util.Optional;

/**
 * What has been picked so far while booking: the flight, who it is for, and the reservation once it
 * exists. The screens of the booking steps hand these on through this object, so they do not need to
 * know each other.
 */
final class BookingSession {

    private FlightOffer offer;
    private Customer customer;
    private Reservation reservation;
    private PaymentResult payment;

    void select(FlightOffer chosen) {
        if (chosen == null) {
            throw new IllegalArgumentException("A flight is required");
        }
        offer = chosen;
    }

    Optional<FlightOffer> selectedOffer() {
        return Optional.ofNullable(offer);
    }

    /** Who the booking is for when it is not the logged-in customer, for example an agent booking for someone. */
    void bookFor(Customer who) {
        customer = who;
    }

    Optional<Customer> customer() {
        return Optional.ofNullable(customer);
    }

    void setReservation(Reservation current) {
        reservation = current;
    }

    Optional<Reservation> reservation() {
        return Optional.ofNullable(reservation);
    }

    /** The approved payment, for the confirmation screen. */
    void setPayment(PaymentResult approved) {
        payment = approved;
    }

    Optional<PaymentResult> payment() {
        return Optional.ofNullable(payment);
    }

    /** Forgets the flight and the reservation, for a new booking. */
    void clear() {
        offer = null;
        customer = null;
        reservation = null;
        payment = null;
    }
}
