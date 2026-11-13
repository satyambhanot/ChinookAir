package ca.ucalgary.chinookair.business.entity;

/** An origin and a destination that flights are scheduled on. The id is null until the route is saved. */
public record Route(Long id, Airport origin, Airport destination) {

    public Route {
        if (origin == null || destination == null) {
            throw new IllegalArgumentException("A route needs an origin and a destination");
        }
        if (origin.equals(destination)) {
            throw new IllegalArgumentException("Origin and destination must be different");
        }
    }
}
