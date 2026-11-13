package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.control.AccessDeniedException;
import ca.ucalgary.chinookair.business.control.AuthenticationException;
import ca.ucalgary.chinookair.business.entity.SeatUnavailableException;
import ca.ucalgary.chinookair.data.DataAccessException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ErrorMessagesTest {

    private final List<LogRecord> logged = new ArrayList<>();
    private Logger logger;
    private Handler handler;
    private boolean usedParentHandlers;

    @BeforeEach
    void captureTheLog() {
        logger = Logger.getLogger(ErrorMessages.class.getName());
        usedParentHandlers = logger.getUseParentHandlers();
        logger.setUseParentHandlers(false);
        handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                logged.add(record);
            }

            @Override
            public void flush() {
                // nothing to flush
            }

            @Override
            public void close() {
                // nothing to close
            }
        };
        logger.addHandler(handler);
    }

    @AfterEach
    void restoreTheLog() {
        logger.removeHandler(handler);
        logger.setUseParentHandlers(usedParentHandlers);
    }

    @Test
    void theMessagesOfTheControllersAreShownAsTheyAre() {
        assertEquals("Flight number already exists",
                ErrorMessages.forException(new IllegalArgumentException("Flight number already exists")));
        assertEquals("That flight has already departed",
                ErrorMessages.forException(new IllegalStateException("That flight has already departed")));
        assertEquals("Invalid username or password",
                ErrorMessages.forException(new AuthenticationException("Invalid username or password")));
        assertTrue(logged.isEmpty());
    }

    @Test
    void aTakenSeatHasItsOwnSentence() {
        assertEquals("That seat was just taken. Please pick another.",
                ErrorMessages.forException(new SeatUnavailableException("Seat 1A was just taken")));
    }

    @Test
    void aMissingPermissionHasItsOwnSentence() {
        assertEquals("You do not have permission to do that.",
                ErrorMessages.forException(new AccessDeniedException("Please log in first")));
    }

    @Test
    void aDatabaseProblemSaysTheDataWasNotChangedAndHidesTheDetails() {
        String message = ErrorMessages.forException(new DataAccessException("Insert failed: [SQLITE_BUSY] locked"));
        assertEquals("The database had a problem. Your data was not changed.", message);
        assertFalse(message.contains("SQLITE"));
        assertEquals(1, logged.size());
    }

    @Test
    void aNumberFormatProblemIsNotShownAsTechnicalText() {
        String message = ErrorMessages.forException(new NumberFormatException("For input string: \"abc\""));
        assertEquals("Please enter a valid number.", message);
        assertFalse(message.contains("For input string"));
    }

    @Test
    void anythingUnexpectedGetsTheGeneralSentenceAndIsLoggedNotShown() {
        String message = ErrorMessages.forException(new RuntimeException("boom"));
        assertEquals("Something unexpected happened. Please try again.", message);
        assertFalse(message.contains("boom"));
        assertEquals(1, logged.size());
        assertEquals(Level.WARNING, logged.get(0).getLevel());
        assertEquals("boom", logged.get(0).getThrown().getMessage());
    }

    @Test
    void aMissingMessageFallsBackToTheGeneralSentence() {
        assertEquals("Something unexpected happened. Please try again.",
                ErrorMessages.forException(new IllegalArgumentException()));
        assertEquals("Something unexpected happened. Please try again.",
                ErrorMessages.forException(new IllegalStateException("  ")));
        assertEquals("Something unexpected happened. Please try again.", ErrorMessages.forException(null));
    }

    @Test
    void aNullPointerNeverReachesTheScreen() {
        String message = ErrorMessages.forException(new NullPointerException("secret detail"));
        assertFalse(message.contains("secret detail"));
        assertFalse(message.contains("NullPointer"));
    }
}
