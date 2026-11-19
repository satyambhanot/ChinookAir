package ca.ucalgary.chinookair.business.entity;

/** Manages flights, routes, aircraft and schedules. */
public class Administrator extends User {

    public Administrator(Long id, String username, String passwordHash) {
        super(id, username, passwordHash);
    }

    @Override
    public Role role() {
        return Role.ADMIN;
    }
}
