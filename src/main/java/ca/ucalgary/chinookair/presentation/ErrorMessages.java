package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.AccessDeniedException;
import ca.ucalgary.chinookair.business.control.AuthenticationException;
import ca.ucalgary.chinookair.business.entity.SeatUnavailableException;
import ca.ucalgary.chinookair.business.entity.StorageException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Turns whatever went wrong into one plain sentence for the person at the screen, never a stack trace. */
final class ErrorMessages {

    static final String SEAT_TAKEN = "That seat was just taken. Please pick another.";
    static final String NOT_ALLOWED = "You do not have permission to do that.";
    static final String STORAGE = "The database had a problem. Your data was not changed.";
    static final String NUMBER = "Please enter a valid number.";
    static final String UNEXPECTED = "Something unexpected happened. Please try again.";

    private static final Logger LOG = Logger.getLogger(ErrorMessages.class.getName());

    private ErrorMessages() {
    }

    /**
     * The sentence to show. The messages of the controllers are already written for people, so they are
     * shown as they are; anything the program did not expect gets a general sentence, and the real error
     * goes to the log instead of the screen.
     */
    static String forException(Throwable problem) {
        if (problem instanceof SeatUnavailableException) {
            return SEAT_TAKEN;
        }
        if (problem instanceof AccessDeniedException) {
            return NOT_ALLOWED;
        }
        if (problem instanceof StorageException) {
            LOG.log(Level.WARNING, "Database problem", problem);
            return STORAGE;
        }
        if (problem instanceof NumberFormatException) {
            return NUMBER;
        }
        if (problem instanceof AuthenticationException || problem instanceof IllegalArgumentException
                || problem instanceof IllegalStateException) {
            String message = problem.getMessage();
            if (message != null && !message.isBlank()) {
                return message;
            }
        }
        LOG.log(Level.WARNING, "Unexpected error", problem);
        return UNEXPECTED;
    }
}
