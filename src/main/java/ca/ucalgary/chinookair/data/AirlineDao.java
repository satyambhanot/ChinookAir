package ca.ucalgary.chinookair.data;

import ca.ucalgary.chinookair.business.entity.Airline;
import java.util.List;
import java.util.Optional;

/** Loads and saves airlines. */
public interface AirlineDao {

    /** All airlines, sorted by code. */
    List<Airline> findAll();

    Optional<Airline> findByCode(String code);

    /** Adds a new airline. A second airline with the same code throws DataAccessException. */
    Airline save(Airline airline);
}
