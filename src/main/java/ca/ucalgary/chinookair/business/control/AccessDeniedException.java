package ca.ucalgary.chinookair.business.control;

/** The user may not do this: their role is not one of the allowed ones, or nobody is logged in. */
public class AccessDeniedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AccessDeniedException(String message) {
        super(message);
    }
}
