package ca.ucalgary.chinookair.business.control;

import ca.ucalgary.chinookair.business.entity.User;
import java.util.Optional;

/**
 * Who is logged in to this running program. One instance is created at start-up and passed
 * to whoever needs it; it is not a static global, so tests can have as many as they like.
 */
public class SessionContext {

    private User currentUser;

    public void login(User user) {
        if (user == null) {
            throw new IllegalArgumentException("A user is required");
        }
        currentUser = user;
    }

    public void logout() {
        currentUser = null;
    }

    /** The logged-in user, or empty if nobody is. */
    public Optional<User> currentUser() {
        return Optional.ofNullable(currentUser);
    }
}
