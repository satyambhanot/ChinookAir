package ca.ucalgary.chinookair.business.entity;

/** The seat is already held by another active reservation. */
public class SeatUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SeatUnavailableException(String message) {
        super(message);
    }

    public SeatUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
