package ca.ucalgary.chinookair.business.entity;

/** Where a payment is in its life. These are the states of the payment state diagram. */
public enum PaymentStatus {
    PENDING,
    APPROVED,
    DECLINED,
    REFUNDED
}
