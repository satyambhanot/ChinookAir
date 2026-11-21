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

    /**
     * Removes an aircraft that no flight uses. Only an administrator may.
     *
     * @throws AccessDeniedException    if the actor is not an administrator
     * @throws IllegalArgumentException if there is no such aircraft
     * @throws IllegalStateException    if a flight uses the aircraft
     */
    public void removeAircraft(User actor, long aircraftId) {
        Authorizer.require(actor, Role.ADMIN);
        aircraft.findById(aircraftId).orElseThrow(() -> new IllegalArgumentException("Aircraft not found"));
        if (aircraft.isUsed(aircraftId)) {
            throw new IllegalStateException("Cannot remove an aircraft that flights use");
        }
        aircraft.delete(aircraftId);
    }

    /** All aircraft, oldest first. */
    public List<Aircraft> list() {
        return aircraft.findAll();
    }
}
