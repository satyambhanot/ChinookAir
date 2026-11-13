package ca.ucalgary.chinookair.business.control;

import ca.ucalgary.chinookair.business.entity.AuthorizationResult;
import ca.ucalgary.chinookair.business.entity.Payment;
import ca.ucalgary.chinookair.business.entity.PaymentDetails;
import ca.ucalgary.chinookair.business.entity.PaymentMethodType;
import ca.ucalgary.chinookair.business.entity.PaymentMethods;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.ReservationStatus;
import ca.ucalgary.chinookair.business.entity.Role;
import ca.ucalgary.chinookair.business.entity.User;
import ca.ucalgary.chinookair.data.ConfirmationWriteException;
import ca.ucalgary.chinookair.data.ConfirmationWriter;
import ca.ucalgary.chinookair.data.PaymentDao;
import ca.ucalgary.chinookair.data.ReservationDao;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Use cases Make Payment and View Payment Records. The payment is only simulated: the card type
 * picks a strategy that approves or declines. An approved payment confirms the reservation and
 * writes the confirmation; a declined one leaves the reservation waiting so the customer can try again.
 */
public class PaymentController {

    private final ReservationDao reservations;
    private final PaymentDao payments;
    private final ConfirmationWriter writer;
    private final Clock clock;

    public PaymentController(ReservationDao reservations, PaymentDao payments, ConfirmationWriter writer,
                             Clock clock) {
        this.reservations = reservations;
        this.payments = payments;
        this.writer = writer;
        this.clock = clock;
    }

    /**
     * Pays the price of a reservation that is waiting for payment. A customer may only pay for their own
     * reservations, an agent for anybody's. A declined payment is saved too, as a record of the attempt.
     *
     * @throws AccessDeniedException    if the actor may not pay for this reservation
     * @throws IllegalArgumentException if the reservation does not exist or the card details are not valid;
     *                                  nothing is saved then
     * @throws IllegalStateException    if the reservation is not waiting for payment
     */
    public PaymentResult pay(User actor, long reservationId, PaymentMethodType type, PaymentDetails details) {
        Authorizer.require(actor, Role.CUSTOMER, Role.AGENT);
        Reservation reservation = reservations.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("That reservation does not exist"));
        if (actor.role() == Role.CUSTOMER && !Objects.equals(actor.id(), reservation.customer().id())) {
            throw new AccessDeniedException("You can only pay for your own reservations");
        }
        if (reservation.status() != ReservationStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Only a reservation that is waiting for payment can be paid");
        }
        if (type == null) {
            throw new IllegalArgumentException("A card type is required");
        }
        if (details == null) {
            throw new IllegalArgumentException("Card details are required");
        }
        details.validate(clock);

        BigDecimal amount = reservation.price();
        AuthorizationResult answer = PaymentMethods.forType(type).authorize(details, amount);
        // only the masked card goes into the payment
        Payment payment = new Payment(reservation.id(), amount, type, details.maskedCard(), LocalDateTime.now(clock));
        if (!answer.approved()) {
            payment.decline(answer.message());
            payments.save(payment);
            return new PaymentResult(payment, reservation, null, answer.message());
        }
        payment.approve();
        payments.save(payment);
        reservation.confirmPayment();
        reservations.update(reservation);
        return approved(reservation, payment);
    }

    /**
     * The payments for all reservations of a customer, newest first: the customer themself or an agent.
     *
     * @throws AccessDeniedException if the actor may not see them
     */
    public List<Payment> recordsForCustomer(User actor, long customerId) {
        Authorizer.require(actor, Role.CUSTOMER, Role.AGENT);
        if (actor.role() == Role.CUSTOMER && !Objects.equals(actor.id(), customerId)) {
            throw new AccessDeniedException("You can only see your own payments");
        }
        return payments.findByCustomer(customerId);
    }

    /** The payment is final, so a problem with the file only adds a warning, it never undoes the payment. */
    private PaymentResult approved(Reservation reservation, Payment payment) {
        try {
            Path file = writer.write(reservation, payment);
            return new PaymentResult(payment, reservation, file, "Payment approved");
        } catch (ConfirmationWriteException e) {
            return new PaymentResult(payment, reservation, null,
                    "Payment approved, but the confirmation file could not be saved: " + e.getMessage());
        }
    }
}
