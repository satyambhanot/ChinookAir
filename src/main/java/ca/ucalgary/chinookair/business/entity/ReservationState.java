package ca.ucalgary.chinookair.business.entity;

import java.math.BigDecimal;
import java.time.Clock;

/**
 * What a reservation does in one state (State pattern). A state class decides
 * if an action is allowed and which state comes next; the Reservation only
 * keeps the data.
 */
interface ReservationState {

    ReservationStatus status();

    void assignSeat(Reservation reservation, String seatNo, BigDecimal price);

    void confirmPayment(Reservation reservation);

    void changeFlight(Reservation reservation, Flight flight, String seatNo, BigDecimal newPrice);

    void cancel(Reservation reservation);

    void complete(Reservation reservation, Clock clock);
}
