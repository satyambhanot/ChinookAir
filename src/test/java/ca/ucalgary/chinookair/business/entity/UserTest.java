package ca.ucalgary.chinookair.business.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class UserTest {

    private static final String HASH = "pbkdf2$1$AAAA$BBBB";

    private static Customer customer(String username, String name, String email, String phone) {
        return new Customer(null, username, HASH, name, email, phone);
    }

    @Test
    void usernameIsTrimmedAndLowercased() {
        Customer c = customer(" JSmith ", "Jo Smith", "jo@x.ca", null);
        assertEquals("jsmith", c.username());
    }

    @Test
    void idIsNullBeforeSavingAndKeptAfter() {
        assertNull(customer("jsmith", "Jo Smith", "jo@x.ca", null).id());
        assertEquals(7L, new Customer(7L, "jsmith", HASH, "Jo Smith", "jo@x.ca", null).id());
    }

    @Test
    void usernameRulesAreChecked() {
        assertThrows(IllegalArgumentException.class, () -> customer("ab", "Jo", "jo@x.ca", null));
        assertThrows(IllegalArgumentException.class, () -> customer("a b", "Jo", "jo@x.ca", null));
        assertThrows(IllegalArgumentException.class, () -> customer("a".repeat(31), "Jo", "jo@x.ca", null));
        assertThrows(IllegalArgumentException.class, () -> customer("jo!", "Jo", "jo@x.ca", null));
        assertThrows(IllegalArgumentException.class, () -> customer(null, "Jo", "jo@x.ca", null));
        assertEquals("jo.smith_1", customer("Jo.Smith_1", "Jo", "jo@x.ca", null).username());
        assertEquals(30, customer("a".repeat(30), "Jo", "jo@x.ca", null).username().length());
    }

    @Test
    void emailMustHaveADomainWithADot() {
        assertThrows(IllegalArgumentException.class, () -> customer("jsmith", "Jo", "a@b", null));
        assertThrows(IllegalArgumentException.class, () -> customer("jsmith", "Jo", "no-at-sign.ca", null));
        assertThrows(IllegalArgumentException.class, () -> customer("jsmith", "Jo", "a b@c.ca", null));
        assertThrows(IllegalArgumentException.class, () -> customer("jsmith", "Jo", null, null));
        assertEquals("a@b.ca", customer("jsmith", "Jo", "a@b.ca", null).email());
    }

    @Test
    void nameMustNotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> customer("jsmith", "  ", "jo@x.ca", null));
        assertThrows(IllegalArgumentException.class, () -> customer("jsmith", null, "jo@x.ca", null));
        assertEquals("Jo Smith", customer("jsmith", " Jo Smith ", "jo@x.ca", null).name());
    }

    @Test
    void phoneIsOptional() {
        assertNull(customer("jsmith", "Jo", "jo@x.ca", null).phone());
        assertNull(customer("jsmith", "Jo", "jo@x.ca", "  ").phone());
        assertEquals("403-555-0101", customer("jsmith", "Jo", "jo@x.ca", " 403-555-0101 ").phone());
    }

    @Test
    void passwordHashIsRequired() {
        assertThrows(IllegalArgumentException.class,
                () -> new Customer(null, "jsmith", null, "Jo", "jo@x.ca", null));
        assertThrows(IllegalArgumentException.class, () -> new FlightAgent(null, "agent1", " "));
    }

    @Test
    void toStringDoesNotShowTheHash() {
        assertFalse(customer("jsmith", "Jo", "jo@x.ca", null).toString().contains(HASH));
    }

    @Test
    void eachRoleHasItsSubclass() {
        assertEquals(Role.CUSTOMER, customer("jsmith", "Jo", "jo@x.ca", null).role());
        assertEquals(Role.AGENT, new FlightAgent(null, "agent1", HASH).role());
        assertEquals(Role.ADMIN, new Administrator(null, "admin", HASH).role());
    }

    @Test
    void factoryReturnsTheSubclassForEachRole() {
        User admin = UserFactory.create(Role.ADMIN, null, "admin", HASH, null, null, null);
        assertInstanceOf(Administrator.class, admin);
        assertEquals(Role.ADMIN, admin.role());

        User agent = UserFactory.create(Role.AGENT, 2L, "agent1", HASH, null, null, null);
        assertInstanceOf(FlightAgent.class, agent);
        assertEquals(2L, agent.id());

        User customer = UserFactory.create(Role.CUSTOMER, null, "jsmith", HASH, "Jo Smith", "jo@x.ca", null);
        assertInstanceOf(Customer.class, customer);
        assertEquals("Jo Smith", ((Customer) customer).name());
    }

    @Test
    void factoryChecksTheCustomerDetails() {
        assertThrows(IllegalArgumentException.class,
                () -> UserFactory.create(Role.CUSTOMER, null, "jsmith", HASH, "Jo", "bad", null));
        assertThrows(IllegalArgumentException.class,
                () -> UserFactory.create(null, null, "jsmith", HASH, "Jo", "jo@x.ca", null));
    }
}
