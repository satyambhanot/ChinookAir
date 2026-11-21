package ca.ucalgary.chinookair.business.control;

import ca.ucalgary.chinookair.business.entity.Airport;
import ca.ucalgary.chinookair.business.entity.Role;
import ca.ucalgary.chinookair.business.entity.Route;
import ca.ucalgary.chinookair.business.entity.User;
import ca.ucalgary.chinookair.data.AirportDao;
import ca.ucalgary.chinookair.data.RouteDao;
import java.util.List;

/** Use case Manage Routes: the pairs of airports that flights can be scheduled on. */
public class RouteController {

    private final RouteDao routes;
    private final AirportDao airports;

    public RouteController(RouteDao routes, AirportDao airports) {
        this.routes = routes;
        this.airports = airports;
    }

    /**
     * Adds the route from one airport to another. Only an administrator may. YYC to YVR and YVR to YYC
     * are two routes.
     *
     * @throws AccessDeniedException    if the actor is not an administrator
     * @throws IllegalArgumentException if an airport is not known, both are the same, or the route exists
     */
    public Route addRoute(User actor, String originCode, String destinationCode) {
        Authorizer.require(actor, Role.ADMIN);
        Airport origin = airport(originCode, "Origin");
        Airport destination = airport(destinationCode, "Destination");
        Route route = new Route(null, origin, destination);
        if (routes.findByAirports(origin.code(), destination.code()).isPresent()) {
            throw new IllegalArgumentException("Route already exists");
        }
        return routes.save(route);
    }

    /**
     * Removes a route that no flight uses. Only an administrator may.
     *
     * @throws AccessDeniedException    if the actor is not an administrator
     * @throws IllegalArgumentException if there is no such route
     * @throws IllegalStateException    if a flight is scheduled on the route
     */
    public void removeRoute(User actor, long routeId) {
        Authorizer.require(actor, Role.ADMIN);
        routes.findById(routeId).orElseThrow(() -> new IllegalArgumentException("Route not found"));
        if (routes.isUsed(routeId)) {
            throw new IllegalStateException("Cannot remove a route that flights are scheduled on");
        }
        routes.delete(routeId);
    }

    /** All routes, sorted by origin and then destination. */
    public List<Route> list() {
        return routes.findAll();
    }

    private Airport airport(String code, String what) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(what + " is required");
        }
        return airports.findByCode(code).orElseThrow(
                () -> new IllegalArgumentException("Airport not found: " + code.trim().toUpperCase()));
    }
}
