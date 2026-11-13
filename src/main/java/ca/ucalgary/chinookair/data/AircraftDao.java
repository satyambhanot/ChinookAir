package ca.ucalgary.chinookair.data;

import ca.ucalgary.chinookair.business.entity.Aircraft;
import java.util.List;
import java.util.Optional;

/** Loads and saves aircraft. */
public interface AircraftDao {

    /** All aircraft, oldest first. */
    List<Aircraft> findAll();

    Optional<Aircraft> findById(long id);

    /**
     * Adds a new aircraft and returns it with the id the database gave it.
     *
     * @throws IllegalArgumentException if the aircraft already has an id
     */
    Aircraft save(Aircraft aircraft);
}
