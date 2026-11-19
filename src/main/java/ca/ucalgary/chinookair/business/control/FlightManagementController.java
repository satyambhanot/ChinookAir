package ca.ucalgary.chinookair.business.control;

import ca.ucalgary.chinookair.business.entity.Aircraft;
import ca.ucalgary.chinookair.business.entity.Airline;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Role;
import ca.ucalgary.chinookair.business.entity.Route;
import ca.ucalgary.chinookair.business.entity.SeatLayout;
import ca.ucalgary.chinookair.business.entity.User;
import ca.ucalgary.chinookair.data.AircraftDao;
import ca.ucalgary.chinookair.data.AirlineDao;
import ca.ucalgary.chinookair.data.FlightDao;
import ca.ucalgary.chinookair.data.ReservationDao;
import ca.ucalgary.chinookair.data.RouteDao;
import ca.ucalgary.chinookair.data.SeatDao;
import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Use case Manage Flights, for administrators only. It keeps the flights that customers can book
 * correct: unique flight numbers, no aircraft in two places at once, and nothing breaks for people
 * who already have a reservation.
 */
public class FlightManagementController {

    private final FlightDao flights;
    private final SeatDao seats;
    private final RouteDao routes;
    private final AirlineDao airlines;
    private final AircraftDao aircraft;
    private final ReservationDao reservations;
    private final Clock clock;

    public FlightManagementController(FlightDao flights, SeatDao seats, RouteDao routes, AirlineDao airlines,
                                      AircraftDao aircraft, ReservationDao reservations, Clock clock) {
        this.flights = flights;
        this.seats = seats;
        this.routes = routes;
        this.airlines = airlines;
        this.aircraft = aircraft;
        this.reservations = reservations;
        this.clock = clock;
    }

    /**
     * Adds a flight and creates its seats from the seat layout of its aircraft.
     *
     * @throws AccessDeniedException    if the actor is not an administrator
     * @throws IllegalArgumentException with a plain message if a detail is not valid or clashes with
     *                                  another flight
     */
    public Flight addFlight(User actor, FlightDetails details) {
        Authorizer.require(actor, Role.ADMIN);
        Flight flight = checked(null, details);
        Flight saved = flights.save(flight);
        try {
            seats.createSeats(saved.id(), SeatLayout.generate(saved.aircraft()));
        } catch (RuntimeException e) {
            // a flight without seats is no use to anybody, so take it back out
            flights.delete(saved.id());
            throw e;
        }
        return saved;
    }

    /**
     * Changes a flight. The route or the aircraft cannot change while there are active reservations, and
     * the aircraft only while the flight has no reservations at all, because the seats follow the aircraft.
     *
     * @throws AccessDeniedException    if the actor is not an administrator
     * @throws IllegalArgumentException if a detail is not valid or clashes with another flight
     * @throws IllegalStateException    if the flight was removed or the change would break a reservation
     */
    public Flight updateFlight(User actor, long flightId, FlightDetails details) {
        Authorizer.require(actor, Role.ADMIN);
        Flight current = flights.findById(flightId)
                .orElseThrow(() -> new IllegalArgumentException("Flight not found"));
        if (flights.isArchived(flightId)) {
            throw new IllegalStateException("That flight was removed");
        }
        Flight changed = checked(current, details);
        boolean routeChanged = !current.route().id().equals(changed.route().id());
        boolean aircraftChanged = !current.aircraft().id().equals(changed.aircraft().id());
        if (routeChanged || aircraftChanged) {
            if (reservations.countActiveByFlight(flightId) > 0) {
                throw new IllegalStateException(
                        "Cannot change the route or aircraft of a flight with active reservations");
            }
            if (aircraftChanged && !reservations.findByFlight(flightId).isEmpty()) {
                throw new IllegalStateException("Cannot change the aircraft of a flight that already has reservations");
            }
        }
        flights.update(changed);
        if (aircraftChanged) {
            seats.replaceSeats(flightId, SeatLayout.generate(changed.aircraft()));
        }
        return changed;
    }

    /**
     * Takes a flight away. It is blocked while there are active reservations. A flight that never had a
     * reservation is deleted; one with old (cancelled or completed) reservations is archived, so it
     * disappears from searches and schedules but those reservations stay in the history.
     *
     * @throws AccessDeniedException if the actor is not an administrator
     * @throws IllegalStateException if there are active reservations
     */
    public void removeFlight(User actor, long flightId) {
        Authorizer.require(actor, Role.ADMIN);
        flights.findById(flightId).orElseThrow(() -> new IllegalArgumentException("Flight not found"));
        if (reservations.countActiveByFlight(flightId) > 0) {
            throw new IllegalStateException("Cannot remove a flight with active reservations");
        }
        if (reservations.findByFlight(flightId).isEmpty()) {
            flights.delete(flightId);
        } else {
            flights.archive(flightId);
        }
    }

    // ---- the checks that add and update share

    /** Builds the flight from the details, or says in plain words what is wrong. Self is null for a new flight. */
    private Flight checked(Flight self, FlightDetails d) {
        if (d == null) {
            throw new IllegalArgumentException("Flight details are required");
        }
        Route route = routes.findById(d.routeId()).orElseThrow(() -> new IllegalArgumentException("Route not found"));
        Airline airline = airlines.findByCode(d.airlineCode())
                .orElseThrow(() -> new IllegalArgumentException("Airline not found"));
        Aircraft plane = aircraft.findById(d.aircraftId())
                .orElseThrow(() -> new IllegalArgumentException("Aircraft not found"));
        // building the flight checks the number's shape, the times' order and the price
        Flight flight = new Flight(self == null ? null : self.id(), d.flightNumber(), route, airline, plane,
                d.departure(), d.arrival(), d.basePrice());

        boolean numberChanged = self == null || !self.flightNumber().equals(flight.flightNumber());
        if (numberChanged && flights.existsByFlightNumber(flight.flightNumber())) {
            throw new IllegalArgumentException("Flight number already exists");
        }
        if (!flight.departure().isAfter(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException("Departure must be in the future");
        }
        AircraftConflicts.requireNone(flights, flight);
        return flight;
    }
}
