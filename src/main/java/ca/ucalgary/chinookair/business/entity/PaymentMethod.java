package ca.ucalgary.chinookair.business.entity;

import java.math.BigDecimal;

/** Strategy: how one kind of card decides to approve or decline a payment. */
public interface PaymentMethod {

    /** Assumes the details were already validated. */
    AuthorizationResult authorize(PaymentDetails details, BigDecimal amount);
}
