package ca.ucalgary.chinookair.business.entity;

import java.util.regex.Pattern;

/**
 * Someone who can log in. The three roles extend this class.
 * It only stores the password hash, it never looks inside it.
 */
public abstract class User {

    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9._]{3,30}");

    private final Long id;
    private final String username;
    private final String passwordHash;

    /**
     * @param id           null until the user is saved
     * @param username     3 to 30 letters, digits, dots or underscores; kept trimmed and lowercase
     * @param passwordHash the stored hash, never the plain password
     */
    protected User(Long id, String username, String passwordHash) {
        if (username == null || !USERNAME.matcher(username.trim()).matches()) {
            throw new IllegalArgumentException(
                    "Username must be 3 to 30 letters, digits, dots or underscores");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("A password hash is required");
        }
        this.id = id;
        this.username = username.trim().toLowerCase();
        this.passwordHash = passwordHash;
    }

    public abstract Role role();

    public Long id() {
        return id;
    }

    public String username() {
        return username;
    }

    public String passwordHash() {
        return passwordHash;
    }

    // the hash stays out of the text so it can't end up in a log by accident
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + username + "]";
    }
}
