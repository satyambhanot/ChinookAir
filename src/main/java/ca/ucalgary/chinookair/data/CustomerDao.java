package ca.ucalgary.chinookair.data;

import ca.ucalgary.chinookair.business.entity.Customer;
import java.util.List;
import java.util.Optional;

/** Loads and saves customers, who are users with a profile. */
public interface CustomerDao {

    /**
     * Adds a new customer: the login and the profile are saved together, all or nothing.
     *
     * @throws IllegalArgumentException if the customer already has an id
     * @throws DataAccessException      if the username or the email is already used; nothing is saved then
     */
    Customer save(Customer customer);

    /** Changes the name, email and phone of a saved customer. The username and password stay. */
    void update(Customer customer);

    Optional<Customer> findById(long customerId);

    /** Finds a customer by email, ignoring case. */
    Optional<Customer> findByEmail(String email);

    /** Customers whose name or email contains the text, ignoring case, sorted by name. Blank text finds everybody. */
    List<Customer> search(String text);
}
