package ca.ucalgary.chinookair.business.entity;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/** One scheduled trip of an aircraft on a route. The id is null until the flight is saved. */
public record Flight(Long id, String flightNumber, Route route, Airline airline, Aircraft aircraft,
                     LocalDateTime departure, LocalDateTime arrival, BigDecimal basePrice) {

    private static final Pattern FLIGHT_NUMBER = Pattern.compile("^[A-Z]{2}\\d{1,4}$");

    public Flight {
        if (flightNumber == null || !FLIGHT_NUMBER.matcher(flightNumber.trim().toUpperCase()).matches()) {
            throw new IllegalArgumentException("Flight number must be 2 letters and 1 to 4 digits, like AC123");
        }
        if (route == null || airline == null || aircraft == null) {
            throw new IllegalArgumentException("A flight needs a route, an airline and an aircraft");
        }
        if (departure == null || arrival == null || !arrival.isAfter(departure)) {
            throw new IllegalArgumentException("Arrival must be after departure");
        }
        if (basePrice == null || basePrice.signum() <= 0) {
            throw new IllegalArgumentException("Base price must be greater than 0");
        }
        if (basePrice.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Base price can have at most 2 decimals");
        }
        flightNumber = flightNumber.trim().toUpperCase();
        basePrice = basePrice.setScale(2);
    }

    /** True once the clock has reached the departure time. */
    public boolean hasDeparted(Clock clock) {
        return !LocalDateTime.now(clock).isBefore(departure);
    }

    /** True if this flight is in the air at some moment of the period. Touching ends do not overlap. */
    public boolean overlaps(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new IllegalArgumentException("The period must end after it starts");
        }
        return departure.isBefore(end) && start.isBefore(arrival);
    }

    /** What a seat of the given class costs on this flight. */
    public BigDecimal priceFor(SeatClass seatClass) {
        return PricingPolicy.priceFor(basePrice, seatClass);
    }
}
