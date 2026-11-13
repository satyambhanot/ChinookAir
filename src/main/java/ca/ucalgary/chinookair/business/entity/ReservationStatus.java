package ca.ucalgary.chinookair.business.entity;

/** Where a reservation is in its life. These are the states of the reservation state diagram. */
public enum ReservationStatus {
    CREATED,
    PENDING_PAYMENT,
    CONFIRMED,
    CANCELLED,
    COMPLETED
}
