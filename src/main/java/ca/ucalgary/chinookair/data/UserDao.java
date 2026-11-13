package ca.ucalgary.chinookair.data;

import ca.ucalgary.chinookair.business.entity.User;
import java.util.Optional;

/** Loads and saves users of every role. What comes back is the right subclass for the role. */
public interface UserDao {

    /** Finds a user by username, ignoring case. */
    Optional<User> findByUsername(String username);

    Optional<User> findById(long userId);

    /**
     * Adds a new user and returns it with its id. A customer is saved together with its profile,
     * all or nothing.
     *
     * @throws IllegalArgumentException if the user already has an id
     * @throws DataAccessException      if the username (or a customer's email) is already used
     */
    User save(User user);
}
