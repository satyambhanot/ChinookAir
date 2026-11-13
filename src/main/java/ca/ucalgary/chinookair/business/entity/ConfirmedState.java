package ca.ucalgary.chinookair.business.entity;

import java.math.BigDecimal;
import java.time.Clock;

/** The payment is approved. The reservation can still move, be cancelled, or be completed after the flight. */
class ConfirmedState extends AbstractReservationState {

    @Override
    public ReservationStatus status() {
        return ReservationStatus.CONFIRMED;
    }

    @Override
    public void changeFlight(Reservation reservation, Flight flight, String seatNo, BigDecimal newPrice) {
        boolean samePrice = newPrice != null && reservation.price().compareTo(newPrice) == 0;
        reservation.moveToFlight(flight, seatNo, newPrice);
        // a different price has to be paid again, the same price needs nothing
        if (!samePrice) {
            reservation.moveTo(new PendingPaymentState());
        }
    }

    @Override
    public void cancel(Reservation reservation) {
        reservation.moveTo(new CancelledState());
    }

    @Override
    public void complete(Reservation reservation, Clock clock) {
        if (!reservation.flight().hasDeparted(clock)) {
            throw new IllegalStateException("Cannot complete a reservation before its flight departs");
        }
        reservation.moveTo(new CompletedState());
    }
}
