package ca.ucalgary.chinookair.business.entity;

import java.math.BigDecimal;

/** The reservation exists but has no seat yet. */
class CreatedState extends AbstractReservationState {

    @Override
    public ReservationStatus status() {
        return ReservationStatus.CREATED;
    }

    @Override
    public void assignSeat(Reservation reservation, String seatNo, BigDecimal price) {
        reservation.holdSeat(seatNo, price);
        reservation.moveTo(new PendingPaymentState());
    }

    @Override
    public void cancel(Reservation reservation) {
        reservation.moveTo(new CancelledState());
    }
}
