package ca.ucalgary.chinookair.data;

/** The confirmation file could not be written. The payment flow catches it, the payment itself stays. */
public class ConfirmationWriteException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConfirmationWriteException(String message) {
        super(message);
    }

    public ConfirmationWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
