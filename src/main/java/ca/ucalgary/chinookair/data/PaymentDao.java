package ca.ucalgary.chinookair.data;

import ca.ucalgary.chinookair.business.entity.Payment;
import java.util.List;
import java.util.Optional;

/** Loads and saves payments. Only the masked card is ever stored, and amounts are kept as whole cents. */
public interface PaymentDao {

    /**
     * Adds a new payment, gives it its id and returns it.
     *
     * @throws IllegalArgumentException if it already has an id
     * @throws DataAccessException      if the reservation does not exist
     */
    Payment save(Payment payment);

    /** Writes the status and the message of a saved payment, for example after it was approved or refunded. */
    void update(Payment payment);

    Optional<Payment> findById(long paymentId);

    /** The payments of a reservation, oldest first: a declined attempt comes before the approved retry. */
    List<Payment> findByReservation(long reservationId);

    /** The payments for all reservations of a customer, newest first. */
    List<Payment> findByCustomer(long customerId);
}
