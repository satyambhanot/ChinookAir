package ca.ucalgary.chinookair.business.entity;

/** The seat is held and the customer has to pay. */
class PendingPaymentState extends AbstractReservationState {

    @Override
    public ReservationStatus status() {
        return ReservationStatus.PENDING_PAYMENT;
    }

    @Override
    public void confirmPayment(Reservation reservation) {
        reservation.moveTo(new ConfirmedState());
    }

    @Override
    public void cancel(Reservation reservation) {
        reservation.moveTo(new CancelledState());
    }
}
