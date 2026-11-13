package ca.ucalgary.chinookair.business.control;

import ca.ucalgary.chinookair.business.entity.Aircraft;
import ca.ucalgary.chinookair.business.entity.Role;
import ca.ucalgary.chinookair.business.entity.User;
import ca.ucalgary.chinookair.data.AircraftDao;
import java.util.List;

/** Use case Manage Aircraft: the planes and their seat layouts. */
public class AircraftController {

    private final AircraftDao aircraft;

    public AircraftController(AircraftDao aircraft) {
        this.aircraft = aircraft;
    }

    /**
     * Adds an aircraft model with its seat layout. Only an administrator may.
     *
     * @throws AccessDeniedException    if the actor is not an administrator
     * @throws IllegalArgumentException if the model is blank or the rows or the seats per row are out of range
     */
    public Aircraft addAircraft(User actor, String model, int rows, int seatsPerRow) {
        Authorizer.require(actor, Role.ADMIN);
        return aircraft.save(new Aircraft(null, model, rows, seatsPerRow));
    }

    /** All aircraft, oldest first. */
    public List<Aircraft> list() {
        return aircraft.findAll();
    }
}
