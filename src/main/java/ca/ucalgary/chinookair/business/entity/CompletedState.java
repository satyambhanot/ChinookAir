package ca.ucalgary.chinookair.business.entity;

/** The flight has departed. Nothing can be done any more. */
class CompletedState extends AbstractReservationState {

    @Override
    public ReservationStatus status() {
        return ReservationStatus.COMPLETED;
    }
}
