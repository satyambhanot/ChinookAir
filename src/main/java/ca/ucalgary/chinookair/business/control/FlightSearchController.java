package ca.ucalgary.chinookair.business.control;

import ca.ucalgary.chinookair.business.entity.Airline;
import ca.ucalgary.chinookair.business.entity.Airport;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.SeatClass;
import ca.ucalgary.chinookair.data.AirlineDao;
import ca.ucalgary.chinookair.data.AirportDao;
import ca.ucalgary.chinookair.data.FlightDao;
import ca.ucalgary.chinookair.data.SeatDao;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Use cases Find Available Flights and View Flight Details. */
public class FlightSearchController {

    private final FlightDao flights;
    private final SeatDao seats;
    private final AirportDao airports;
    private final AirlineDao airlines;
    private final Clock clock;

    public FlightSearchController(FlightDao flights, SeatDao seats, AirportDao airports, AirlineDao airlines,
                                  Clock clock) {
        this.flights = flights;
        this.seats = seats;
        this.airports = airports;
        this.airlines = airlines;
        this.clock = clock;
    }

    /** Today by the program's clock, the day the search screen starts with. */
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /** Every airport, sorted by code, for the origin and destination lists of the search screen. */
    public List<Airport> airports() {
        return airports.findAll();
    }

    /** Every airline, sorted by code, for the airline list of the search screen. */
    public List<Airline> airlines() {
        return airlines.findAll();
    }

    /**
     * The flights that match, earliest first. Flights that already left today are not shown.
     *
     * @throws IllegalArgumentException with a plain message if the input is not valid
     */
    public List<FlightOffer> search(SearchCriteria criteria) {
        if (criteria == null) {
            throw new IllegalArgumentException("Search criteria are required");
        }
        String origin = airportCode(criteria.origin(), "Origin");
        String destination = airportCode(criteria.destination(), "Destination");
        if (origin.equals(destination)) {
            throw new IllegalArgumentException("Origin and destination must be different");
        }
        LocalDate date = criteria.date();
        if (date == null) {
            throw new IllegalArgumentException("Date is required");
        }
        if (date.isBefore(LocalDate.now(clock))) {
            throw new IllegalArgumentException("Date cannot be in the past");
        }
        String airline = airlineCode(criteria.airlineCode());

        List<FlightOffer> offers = new ArrayList<>();
        for (Flight flight : flights.search(origin, destination, date, airline)) {
            if (!flight.hasDeparted(clock)) {
                offers.add(offerFor(flight));
            }
        }
        return offers;
    }

    /**
     * One flight with its seats left and price.
     *
     * @throws IllegalArgumentException if there is no such flight
     */
    public FlightOffer details(long flightId) {
        Flight flight = flights.findById(flightId)
                .orElseThrow(() -> new IllegalArgumentException("That flight does not exist"));
        return offerFor(flight);
    }

    private FlightOffer offerFor(Flight flight) {
        // the lowest price is the economy price
        return new FlightOffer(flight, seats.countAvailable(flight.id()), flight.priceFor(SeatClass.ECONOMY));
    }

    private static String airportCode(String input, String what) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException(what + " is required");
        }
        String code = input.trim();
        if (!code.matches("[A-Za-z]{3}")) {
            throw new IllegalArgumentException(what + " must be 3 letters, like YYC");
        }
        return code.toUpperCase();
    }

    /** @return the airline code in uppercase, or null for any airline */
    private static String airlineCode(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String code = input.trim();
        if (!code.matches("[A-Za-z]{2}")) {
            throw new IllegalArgumentException("Airline must be 2 letters, like AC");
        }
        return code.toUpperCase();
    }
}
