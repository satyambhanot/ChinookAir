package ca.ucalgary.chinookair.business.entity;

/**
 * Builds the right kind of User for a role, so the code that logs in or
 * registers someone does not have to name the subclasses.
 */
public final class UserFactory {

    private UserFactory() {
    }

    /**
     * Name, email and phone are only used for a customer; agents and
     * administrators have no extra details yet.
     */
    public static User create(Role role, Long id, String username, String passwordHash,
                              String name, String email, String phone) {
        if (role == null) {
            throw new IllegalArgumentException("A role is required");
        }
        switch (role) {
            case CUSTOMER:
                return new Customer(id, username, passwordHash, name, email, phone);
            case AGENT:
                return new FlightAgent(id, username, passwordHash);
            case ADMIN:
                return new Administrator(id, username, passwordHash);
            default:
                throw new IllegalArgumentException("Unknown role " + role);
        }
    }
}
