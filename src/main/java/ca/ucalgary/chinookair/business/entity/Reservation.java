package ca.ucalgary.chinookair.business.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;

/**
 * A customer's hold on one seat on one flight. The actions are handed to the
 * current state object, so an action that does not fit the state is refused.
 */
public final class Reservation {

    private Long id;
    private final String confirmationNumber;
    private final Customer customer;
    private Flight flight;
    private String seatNo;
    private BigDecimal price;
    private final LocalDateTime createdAt;
    private ReservationState state;

    /** A new reservation: no seat yet and no id until it is saved. */
    public Reservation(String confirmationNumber, Customer customer, Flight flight, LocalDateTime createdAt) {
        this(null, confirmationNumber, customer, flight, null, null, createdAt, new CreatedState());
    }

    private Reservation(Long id, String confirmationNumber, Customer customer, Flight flight, String seatNo,
                        BigDecimal price, LocalDateTime createdAt, ReservationState state) {
        if (confirmationNumber == null || confirmationNumber.isBlank()) {
            throw new IllegalArgumentException("A confirmation number is required");
        }
        if (customer == null || flight == null || createdAt == null) {
            throw new IllegalArgumentException("A reservation needs a customer, a flight and a creation time");
        }
        this.id = id;
        this.confirmationNumber = confirmationNumber.trim();
        this.customer = customer;
        this.flight = flight;
        this.seatNo = seatNo;
        this.price = price;
        this.createdAt = createdAt;
        this.state = state;
    }

    /** Rebuilds a saved reservation, with the state object that belongs to its status. */
    public static Reservation restore(Long id, String confirmationNumber, Customer customer, Flight flight,
                                      String seatNo, BigDecimal price, LocalDateTime createdAt,
                                      ReservationStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("A status is required");
        }
        boolean needsSeat = status == ReservationStatus.PENDING_PAYMENT
                || status == ReservationStatus.CONFIRMED || status == ReservationStatus.COMPLETED;
        if (needsSeat && (seatNo == null || price == null)) {
            throw new IllegalArgumentException("A " + status + " reservation needs a seat and a price");
        }
        BigDecimal scaled = price == null ? null : price.setScale(2, RoundingMode.HALF_UP);
        return new Reservation(id, confirmationNumber, customer, flight, seatNo, scaled, createdAt,
                stateFor(status));
    }

    private static ReservationState stateFor(ReservationStatus status) {
        switch (status) {
            case CREATED:
                return new CreatedState();
            case PENDING_PAYMENT:
                return new PendingPaymentState();
            case CONFIRMED:
                return new ConfirmedState();
            case CANCELLED:
                return new CancelledState();
            case COMPLETED:
                return new CompletedState();
            default:
                throw new IllegalArgumentException("Unknown status " + status);
        }
    }

    // ---- the actions of the state diagram

    public void assignSeat(String seatNo, BigDecimal price) {
        state.assignSeat(this, seatNo, price);
    }

    public void confirmPayment() {
        state.confirmPayment(this);
    }

    public void changeFlight(Flight newFlight, String newSeatNo, BigDecimal newPrice) {
        state.changeFlight(this, newFlight, newSeatNo, newPrice);
    }

    public void cancel() {
        state.cancel(this);
    }

    public void complete(Clock clock) {
        state.complete(this, clock);
    }

    // ---- what the reservation knows

    public Long id() {
        return id;
    }

    /** Called once by the data layer when the reservation is saved for the first time. */
    public void assignId(long newId) {
        if (id != null) {
            throw new IllegalStateException("This reservation already has an id");
        }
        id = newId;
    }

    public String confirmationNumber() {
        return confirmationNumber;
    }

    public Customer customer() {
        return customer;
    }

    public Flight flight() {
        return flight;
    }

    /** @return the seat number, or null before a seat is assigned */
    public String seatNo() {
        return seatNo;
    }

    /** @return the price of the seat, or null before a seat is assigned */
    public BigDecimal price() {
        return price;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public ReservationStatus status() {
        return state.status();
    }

    /** True while the reservation holds its seat: created, waiting for payment, or confirmed. */
    public boolean isActive() {
        ReservationStatus status = status();
        return status == ReservationStatus.CREATED || status == ReservationStatus.PENDING_PAYMENT
                || status == ReservationStatus.CONFIRMED;
    }

    @Override
    public String toString() {
        return "Reservation[" + confirmationNumber + ", " + status() + "]";
    }

    // ---- used by the state classes only (same package)

    void holdSeat(String newSeatNo, BigDecimal newPrice) {
        checkSeatAndPrice(newSeatNo, newPrice);
        this.seatNo = newSeatNo.trim().toUpperCase();
        this.price = newPrice.setScale(2, RoundingMode.HALF_UP);
    }

    void moveToFlight(Flight newFlight, String newSeatNo, BigDecimal newPrice) {
        if (newFlight == null) {
            throw new IllegalArgumentException("A flight is required");
        }
        checkSeatAndPrice(newSeatNo, newPrice);
        this.flight = newFlight;
        this.seatNo = newSeatNo.trim().toUpperCase();
        this.price = newPrice.setScale(2, RoundingMode.HALF_UP);
    }

    void moveTo(ReservationState next) {
        this.state = next;
    }

    private static void checkSeatAndPrice(String seatNo, BigDecimal price) {
        if (seatNo == null || seatNo.isBlank()) {
            throw new IllegalArgumentException("A seat number is required");
        }
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("The price must be greater than 0");
        }
    }
}
