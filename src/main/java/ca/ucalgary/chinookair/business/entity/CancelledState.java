package ca.ucalgary.chinookair.business.entity;

/** The reservation was cancelled and the seat is free. Nothing can be done any more. */
class CancelledState extends AbstractReservationState {

    @Override
    public ReservationStatus status() {
        return ReservationStatus.CANCELLED;
    }
}
