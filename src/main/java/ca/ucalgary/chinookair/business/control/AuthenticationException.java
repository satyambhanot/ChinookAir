package ca.ucalgary.chinookair.business.control;

/** The username or password was wrong. The message never says which one. */
public class AuthenticationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }
}
