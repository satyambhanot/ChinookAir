package ca.ucalgary.chinookair.business.control;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * What the administrator fills in on the flight form. The controller turns the ids and the code
 * into the real route, airline and aircraft and checks everything.
 */
public record FlightDetails(String flightNumber, long routeId, String airlineCode, long aircraftId,
                            LocalDateTime departure, LocalDateTime arrival, BigDecimal basePrice) {
}
