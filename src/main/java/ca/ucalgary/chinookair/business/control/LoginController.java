package ca.ucalgary.chinookair.business.control;

import ca.ucalgary.chinookair.business.entity.User;
import ca.ucalgary.chinookair.data.UserDao;
import ca.ucalgary.chinookair.util.PasswordHasher;
import java.util.Optional;

/** Use case Login: checks a username and a password and returns the user. */
public class LoginController {

    private static final String INVALID = "Invalid username or password";

    // checked when the username is unknown, so a missing user takes as long as a wrong password
    private static final String DUMMY_HASH = PasswordHasher.hash("not-a-real-password");

    private final UserDao users;

    public LoginController(UserDao users) {
        this.users = users;
    }

    /**
     * @return the user, as the subclass of their role
     * @throws IllegalArgumentException if the username or the password is blank
     * @throws AuthenticationException  if the username is unknown or the password is wrong, with the same
     *                                  message for both so nobody can find out which usernames exist
     */
    public User authenticate(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        Optional<User> found = users.findByUsername(username.trim());
        boolean matches = PasswordHasher.verify(password, found.map(User::passwordHash).orElse(DUMMY_HASH));
        if (found.isEmpty() || !matches) {
            throw new AuthenticationException(INVALID);
        }
        return found.get();
    }
}
