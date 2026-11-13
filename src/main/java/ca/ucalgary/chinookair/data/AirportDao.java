package ca.ucalgary.chinookair.data;

import ca.ucalgary.chinookair.business.entity.Airport;
import java.util.List;
import java.util.Optional;

/** Loads and saves airports. */
public interface AirportDao {

    /** All airports, sorted by code. */
    List<Airport> findAll();

    Optional<Airport> findByCode(String code);

    /** Adds a new airport. A second airport with the same code throws DataAccessException. */
    Airport save(Airport airport);
}
