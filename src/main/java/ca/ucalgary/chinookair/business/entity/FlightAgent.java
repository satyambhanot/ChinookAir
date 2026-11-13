package ca.ucalgary.chinookair.business.entity;

/** Works with customers and their reservations on their behalf. */
public class FlightAgent extends User {

    public FlightAgent(Long id, String username, String passwordHash) {
        super(id, username, passwordHash);
    }

    @Override
    public Role role() {
        return Role.AGENT;
    }
}
