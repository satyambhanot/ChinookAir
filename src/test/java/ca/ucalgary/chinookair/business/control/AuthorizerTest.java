package ca.ucalgary.chinookair.business.control;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.ucalgary.chinookair.business.entity.Administrator;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.FlightAgent;
import ca.ucalgary.chinookair.business.entity.Role;
import ca.ucalgary.chinookair.business.entity.User;
import org.junit.jupiter.api.Test;

class AuthorizerTest {

    private static final String HASH = "pbkdf2$1$AAAA$BBBB";
    private static final User CUSTOMER = new Customer(1L, "customer1", HASH, "Casey", "c@x.ca", null);
    private static final User AGENT = new FlightAgent(2L, "agent1", HASH);
    private static final User ADMIN = new Administrator(3L, "admin", HASH);

    @Test
    void aCustomerCannotDoWhatOnlyAnAdministratorMay() {
        assertThrows(AccessDeniedException.class, () -> Authorizer.require(CUSTOMER, Role.ADMIN));
    }

    @Test
    void anAdministratorPassesAnAdministratorCheck() {
        assertDoesNotThrow(() -> Authorizer.require(ADMIN, Role.ADMIN));
    }

    @Test
    void anyOfSeveralRolesIsEnough() {
        assertDoesNotThrow(() -> Authorizer.require(AGENT, Role.AGENT, Role.ADMIN));
        assertDoesNotThrow(() -> Authorizer.require(ADMIN, Role.AGENT, Role.ADMIN));
        assertThrows(AccessDeniedException.class, () -> Authorizer.require(CUSTOMER, Role.AGENT, Role.ADMIN));
    }

    @Test
    void nobodyLoggedInIsRefusedWithAHelpfulMessage() {
        AccessDeniedException e = assertThrows(AccessDeniedException.class,
                () -> Authorizer.require(null, Role.CUSTOMER));
        assertEquals("Please log in first", e.getMessage());
    }

    @Test
    void anEmptyListOfAllowedRolesAllowsNobody() {
        assertThrows(AccessDeniedException.class, () -> Authorizer.require(ADMIN));
    }

    @Test
    void theDeniedMessageIsPlain() {
        AccessDeniedException e = assertThrows(AccessDeniedException.class,
                () -> Authorizer.require(CUSTOMER, Role.ADMIN));
        assertEquals("You do not have permission to do this", e.getMessage());
    }
}
