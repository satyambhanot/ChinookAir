package ca.ucalgary.chinookair.business.control;

import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Role;
import ca.ucalgary.chinookair.business.entity.User;
import ca.ucalgary.chinookair.data.CustomerDao;
import ca.ucalgary.chinookair.data.UserDao;
import ca.ucalgary.chinookair.util.PasswordHasher;
import java.util.List;
import java.util.Objects;

/**
 * Use cases Register Customer and Manage Customer Profile. The checks and the messages for what a
 * person typed live here, so that a duplicate gets a plain sentence and not a database error.
 */
public class CustomerController {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final CustomerDao customers;
    private final UserDao users;

    public CustomerController(CustomerDao customers, UserDao users) {
        this.customers = customers;
        this.users = users;
    }

    /**
     * Creates a customer with a login. The password is stored as a hash.
     *
     * @param phone optional, blank means none
     * @throws IllegalArgumentException with a plain message if something is not valid or the username
     *                                  or the email is already used
     */
    public Customer register(String username, String password, String name, String email, String phone) {
        checkPassword(password);
        // building the customer checks the username, the name and the email; the hash is only a stand-in here
        Customer checked = new Customer(null, username, "unchecked", name, email, phone);
        if (users.findByUsername(checked.username()).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (customers.findByEmail(checked.email()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }
        // the database constraints are the backstop if someone else registers the same name right now
        return customers.save(new Customer(null, checked.username(), PasswordHasher.hash(password), checked.name(),
                checked.email(), checked.phone()));
    }

    /**
     * The customer themself or an agent may look at a customer.
     *
     * @throws AccessDeniedException    if the actor may not
     * @throws IllegalArgumentException if there is no such customer
     */
    public Customer view(User actor, long customerId) {
        requireSelfOrAgent(actor, customerId);
        return customer(customerId);
    }

    /**
     * Changes the name, email and phone. The username and the password stay. The customer themself or
     * an agent may do this.
     *
     * @throws AccessDeniedException    if the actor may not
     * @throws IllegalArgumentException if a value is not valid or the email belongs to another customer
     */
    public Customer update(User actor, long customerId, String name, String email, String phone) {
        requireSelfOrAgent(actor, customerId);
        Customer current = customer(customerId);
        Customer changed = new Customer(current.id(), current.username(), current.passwordHash(), name, email, phone);
        customers.findByEmail(changed.email()).ifPresent(other -> {
            if (!Objects.equals(other.id(), current.id())) {
                throw new IllegalArgumentException("Email already registered");
            }
        });
        customers.update(changed);
        return changed;
    }

    /**
     * Customers whose name or email contains the text. Only an agent may search.
     *
     * @throws AccessDeniedException if the actor is not an agent
     */
    public List<Customer> search(User actor, String text) {
        Authorizer.require(actor, Role.AGENT);
        return customers.search(text);
    }

    // ---- helpers

    private static void checkPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }
    }

    private static void requireSelfOrAgent(User actor, long customerId) {
        Authorizer.require(actor, Role.CUSTOMER, Role.AGENT);
        if (actor.role() == Role.CUSTOMER && !Objects.equals(actor.id(), customerId)) {
            throw new AccessDeniedException("You can only see and change your own details");
        }
    }

    private Customer customer(long customerId) {
        return customers.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("That customer does not exist"));
    }
}
