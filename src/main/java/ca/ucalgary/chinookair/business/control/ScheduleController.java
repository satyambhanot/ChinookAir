package ca.ucalgary.chinookair.business.control;

import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Role;
import ca.ucalgary.chinookair.business.entity.User;
import ca.ucalgary.chinookair.data.FlightDao;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Use cases View Flight Schedules and Update Schedules. */
public class ScheduleController {

    private final FlightDao flights;
    private final Clock clock;

    public ScheduleController(FlightDao flights, Clock clock) {
        this.flights = flights;
        this.clock = clock;
    }

    /**
     * The flights that depart from the first day to the last day, both days included, earliest first.
     * Removed flights are not shown. An agent or an administrator may look.
     *
     * @throws AccessDeniedException    if the actor is neither
     * @throws IllegalArgumentException if a date is missing or the last day is before the first
     */
    public List<Flight> schedule(User actor, LocalDate from, LocalDate to) {
        Authorizer.require(actor, Role.AGENT, Role.ADMIN);
        if (from == null || to == null) {
            throw new IllegalArgumentException("A first and a last day are required");
        }
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("The last day cannot be before the first day");
        }
        return flights.findDepartingBetween(from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }

    /**
     * Moves a flight to new times. Only an administrator may. A flight that already departed cannot move,
     * the new departure must be in the future, and the aircraft must be free at the new time.
     *
     * @throws AccessDeniedException    if the actor is not an administrator
     * @throws IllegalArgumentException if the flight does not exist, the times are not valid or the aircraft
     *                                  is busy
     * @throws IllegalStateException    if the flight already departed or was removed
     */
    public Flight reschedule(User actor, long flightId, LocalDateTime departure, LocalDateTime arrival) {
        Authorizer.require(actor, Role.ADMIN);
        Flight current = flights.findById(flightId)
                .orElseThrow(() -> new IllegalArgumentException("Flight not found"));
        if (flights.isArchived(flightId)) {
            throw new IllegalStateException("That flight was removed");
        }
        if (current.hasDeparted(clock)) {
            throw new IllegalStateException("That flight has already departed");
        }
        // building the flight checks that the arrival is after the departure
        Flight moved = new Flight(current.id(), current.flightNumber(), current.route(), current.airline(),
                current.aircraft(), departure, arrival, current.basePrice());
        if (!moved.departure().isAfter(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException("Departure must be in the future");
        }
        AircraftConflicts.requireNone(flights, moved);
        flights.update(moved);
        return moved;
    }
}
