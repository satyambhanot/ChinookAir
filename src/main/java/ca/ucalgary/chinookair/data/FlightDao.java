package ca.ucalgary.chinookair.data;

import ca.ucalgary.chinookair.business.entity.Flight;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Loads and saves flights. A removed flight that has reservations is archived, not
 * deleted: it is hidden from the searches but findById still returns it.
 */
public interface FlightDao {

    /**
     * Adds a new flight and returns it with its id. The route and the aircraft must already be saved.
     *
     * @throws IllegalArgumentException if the flight already has an id or its route or aircraft is not saved
     * @throws DataAccessException      if the flight number is already used
     */
    Flight save(Flight flight);

    /** Changes a saved flight. Its seats are not touched. */
    void update(Flight flight);

    /** Deletes a flight and its seats. Throws DataAccessException while a reservation still points at it. */
    void delete(long flightId);

    /** Hides a flight from search, schedules and aircraft checks. Its reservations stay as they are. */
    void archive(long flightId);

    /** Finds a flight, archived or not. */
    Optional<Flight> findById(long flightId);

    boolean existsByFlightNumber(String flightNumber);

    /**
     * Flights that depart on that calendar day on the route, earliest first. The airline is only a filter
     * when it is not null or blank. Archived flights are never returned.
     */
    List<Flight> search(String origin, String destination, LocalDate date, String airlineCodeOrNull);

    /** Flights that depart from {@code from} (included) until {@code to} (not included), earliest first. */
    List<Flight> findDepartingBetween(LocalDateTime from, LocalDateTime to);

    /** The flights that use this aircraft, earliest first. Archived flights do not count. */
    List<Flight> findByAircraft(long aircraftId);
}
