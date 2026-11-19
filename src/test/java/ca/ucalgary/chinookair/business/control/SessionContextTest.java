package ca.ucalgary.chinookair.business.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Administrator;
import ca.ucalgary.chinookair.business.entity.FlightAgent;
import ca.ucalgary.chinookair.business.entity.User;
import org.junit.jupiter.api.Test;

class SessionContextTest {

    private static final User ADMIN = new Administrator(1L, "admin", "pbkdf2$1$AAAA$BBBB");
    private static final User AGENT = new FlightAgent(2L, "agent1", "pbkdf2$1$AAAA$BBBB");

    @Test
    void nobodyIsLoggedInAtFirst() {
        assertTrue(new SessionContext().currentUser().isEmpty());
    }

    @Test
    void theUserIsThereAfterLoginAndGoneAfterLogout() {
        SessionContext session = new SessionContext();
        session.login(ADMIN);
        assertSame(ADMIN, session.currentUser().orElseThrow());
        session.logout();
        assertTrue(session.currentUser().isEmpty());
    }

    @Test
    void anotherLoginReplacesTheUser() {
        SessionContext session = new SessionContext();
        session.login(ADMIN);
        session.login(AGENT);
        assertEquals("agent1", session.currentUser().orElseThrow().username());
    }

    @Test
    void logoutWithoutLoginIsHarmless() {
        SessionContext session = new SessionContext();
        session.logout();
        assertTrue(session.currentUser().isEmpty());
    }

    @Test
    void aUserIsRequiredToLogIn() {
        assertThrows(IllegalArgumentException.class, () -> new SessionContext().login(null));
    }

    @Test
    void twoSessionsDoNotShareTheirUser() {
        SessionContext first = new SessionContext();
        SessionContext second = new SessionContext();
        first.login(ADMIN);
        assertTrue(second.currentUser().isEmpty());
    }
}
