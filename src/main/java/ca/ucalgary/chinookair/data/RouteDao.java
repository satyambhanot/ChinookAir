package ca.ucalgary.chinookair.data;

import ca.ucalgary.chinookair.business.entity.Route;
import java.util.List;
import java.util.Optional;

/** Loads and saves routes. Both airports of a route must already be saved. */
public interface RouteDao {

    /** All routes, sorted by origin and then destination. */
    List<Route> findAll();

    Optional<Route> findById(long id);

    /** The route from one airport code to another. YYC to YVR is not the same route as YVR to YYC. */
    Optional<Route> findByAirports(String originCode, String destinationCode);

    /**
     * Adds a new route and returns it with the id the database gave it.
     *
     * @throws IllegalArgumentException if the route already has an id
     * @throws DataAccessException      if an airport is not saved or the route exists
     */
    Route save(Route route);
}
