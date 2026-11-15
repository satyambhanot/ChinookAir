package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.FlightSearchController;
import ca.ucalgary.chinookair.business.control.SearchCriteria;
import ca.ucalgary.chinookair.business.entity.Airport;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Where a search starts when there is nothing to remember: the first route that has a flight in the
 * coming days, so the first search is not an empty one.
 */
final class StartingRoute {

    /** How many days from today are looked through. */
    static final int LOOKAHEAD_DAYS = 7;

    /** The airports and the day to start on. */
    record Start(Airport from, Airport to, LocalDate date) {
    }

    private StartingRoute() {
    }

    static Optional<Start> find(FlightSearchController search, List<Airport> airports, LocalDate today) {
        for (int day = 0; day < LOOKAHEAD_DAYS; day++) {
            for (Airport from : airports) {
                for (Airport to : airports) {
                    if (!from.equals(to) && !search
                            .search(new SearchCriteria(from.code(), to.code(), today.plusDays(day), null)).isEmpty()) {
                        return Optional.of(new Start(from, to, today.plusDays(day)));
                    }
                }
            }
        }
        return Optional.empty();
    }
}
