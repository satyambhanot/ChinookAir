package ca.ucalgary.chinookair.business.control;

import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.data.FlightDao;

/** The rule that an aircraft cannot be on two overlapping flights, shared by adding and rescheduling. */
final class AircraftConflicts {

    private AircraftConflicts() {
    }

    /**
     * Refuses the flight if its aircraft is already on another flight at that time. Flights that only
     * touch (one lands as the next leaves) are fine, a removed flight does not count, and the flight is
     * never in conflict with its own old self.
     *
     * @throws IllegalArgumentException if there is a clash
     */
    static void requireNone(FlightDao flights, Flight candidate) {
        for (Flight other : flights.findByAircraft(candidate.aircraft().id())) {
            boolean itself = candidate.id() != null && candidate.id().equals(other.id());
            if (!itself && other.overlaps(candidate.departure(), candidate.arrival())) {
                throw new IllegalArgumentException("Aircraft is already assigned to another flight at that time");
            }
        }
    }
}
