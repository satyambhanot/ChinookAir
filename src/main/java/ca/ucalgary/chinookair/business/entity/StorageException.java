package ca.ucalgary.chinookair.business.entity;

/**
 * The program could not read or write its stored data. The data layer throws a more specific
 * subclass; the screens only need to know this much, so they never depend on the data layer.
 */
public class StorageException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
