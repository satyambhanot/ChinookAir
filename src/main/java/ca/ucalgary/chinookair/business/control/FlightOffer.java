package ca.ucalgary.chinookair.business.control;

import ca.ucalgary.chinookair.business.entity.Flight;
import java.math.BigDecimal;

/** One line of the search results: a flight, how many seats are left, and the lowest price. */
public record FlightOffer(Flight flight, int seatsLeft, BigDecimal fromPrice) {
}
