package ca.ucalgary.chinookair.business.entity;

import java.math.BigDecimal;
import java.time.Clock;

/**
 * Every action is refused unless a state overrides it, so a new state only
 * has to write the few actions it allows.
 */
abstract class AbstractReservationState implements ReservationState {

    @Override
    public void assignSeat(Reservation reservation, String seatNo, BigDecimal price) {
        refuse("assign a seat to");
    }

    @Override
    public void confirmPayment(Reservation reservation) {
        refuse("confirm the payment of");
    }

    @Override
    public void changeFlight(Reservation reservation, Flight flight, String seatNo, BigDecimal newPrice) {
        refuse("change the flight of");
    }

    @Override
    public void cancel(Reservation reservation) {
        refuse("cancel");
    }

    @Override
    public void complete(Reservation reservation, Clock clock) {
        refuse("complete");
    }

    private void refuse(String action) {
        String state = status().name().toLowerCase().replace('_', ' ');
        throw new IllegalStateException("Cannot " + action + " a " + state + " reservation");
    }
}
