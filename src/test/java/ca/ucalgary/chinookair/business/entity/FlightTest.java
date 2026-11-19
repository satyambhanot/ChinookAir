package ca.ucalgary.chinookair.business.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class FlightTest {

    private static final Route ROUTE =
            new Route(1L, new Airport("YYC", "Calgary"), new Airport("YVR", "Vancouver"));
    private static final Airline AIRLINE = new Airline("AC", "Air Canada");
    private static final Aircraft AIRCRAFT = new Aircraft(1L, "A320", 30, 6);
    private static final LocalDateTime DEP = LocalDateTime.of(2026, 12, 1, 8, 0);
    private static final LocalDateTime ARR = LocalDateTime.of(2026, 12, 1, 10, 0);

    private static Flight flight(String number, LocalDateTime dep, LocalDateTime arr, String price) {
        return new Flight(null, number, ROUTE, AIRLINE, AIRCRAFT, dep, arr, new BigDecimal(price));
    }

    private static Clock clockAt(LocalDateTime time) {
        return Clock.fixed(time.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    }

    @Test
    void arrivalMustBeAfterDeparture() {
        assertThrows(IllegalArgumentException.class, () -> flight("AC123", ARR, DEP, "100.00"));
        assertThrows(IllegalArgumentException.class, () -> flight("AC123", DEP, DEP, "100.00"));
    }

    @Test
    void flightNumberIsStoredInUppercase() {
        assertEquals("AC123", flight("ac123", DEP, ARR, "100.00").flightNumber());
        assertEquals("AC1", flight(" ac1 ", DEP, ARR, "100.00").flightNumber());
    }

    @Test
    void flightNumberNeedsTwoLettersAndUpToFourDigits() {
        assertThrows(IllegalArgumentException.class, () -> flight("A1", DEP, ARR, "100.00"));
        assertThrows(IllegalArgumentException.class, () -> flight("AC12345", DEP, ARR, "100.00"));
        assertThrows(IllegalArgumentException.class, () -> flight("AC", DEP, ARR, "100.00"));
        assertThrows(IllegalArgumentException.class, () -> flight("123AC", DEP, ARR, "100.00"));
        assertThrows(IllegalArgumentException.class, () -> flight(null, DEP, ARR, "100.00"));
        assertEquals("AC1234", flight("AC1234", DEP, ARR, "100.00").flightNumber());
    }

    @Test
    void basePriceMustBePositiveWithAtMostTwoDecimals() {
        assertThrows(IllegalArgumentException.class, () -> flight("AC123", DEP, ARR, "0"));
        assertThrows(IllegalArgumentException.class, () -> flight("AC123", DEP, ARR, "-5.00"));
        assertThrows(IllegalArgumentException.class, () -> flight("AC123", DEP, ARR, "100.005"));
    }

    @Test
    void basePriceKeepsTwoDecimals() {
        assertEquals(new BigDecimal("100.00"), flight("AC123", DEP, ARR, "100").basePrice());
        assertEquals(new BigDecimal("99.90"), flight("AC123", DEP, ARR, "99.9").basePrice());
        assertEquals(new BigDecimal("100.00"), flight("AC123", DEP, ARR, "100.000").basePrice());
    }

    @Test
    void routeAirlineAndAircraftAreRequired() {
        BigDecimal price = new BigDecimal("100.00");
        assertThrows(IllegalArgumentException.class,
                () -> new Flight(null, "AC123", null, AIRLINE, AIRCRAFT, DEP, ARR, price));
        assertThrows(IllegalArgumentException.class,
                () -> new Flight(null, "AC123", ROUTE, null, AIRCRAFT, DEP, ARR, price));
        assertThrows(IllegalArgumentException.class,
                () -> new Flight(null, "AC123", ROUTE, AIRLINE, null, DEP, ARR, price));
    }

    @Test
    void hasDepartedIsTrueFromTheDepartureMinute() {
        Flight f = flight("AC123", DEP, ARR, "100.00");
        assertFalse(f.hasDeparted(clockAt(LocalDateTime.of(2026, 12, 1, 7, 59))));
        assertTrue(f.hasDeparted(clockAt(LocalDateTime.of(2026, 12, 1, 8, 0))));
        assertTrue(f.hasDeparted(clockAt(LocalDateTime.of(2026, 12, 2, 8, 0))));
    }

    @Test
    void overlapsWhenThePeriodsShareSomeTime() {
        Flight f = flight("AC123", DEP, ARR, "100.00");
        assertTrue(f.overlaps(LocalDateTime.of(2026, 12, 1, 9, 30), LocalDateTime.of(2026, 12, 1, 11, 0)));
        assertTrue(f.overlaps(LocalDateTime.of(2026, 12, 1, 6, 0), LocalDateTime.of(2026, 12, 1, 9, 0)));
        assertTrue(f.overlaps(LocalDateTime.of(2026, 12, 1, 8, 30), LocalDateTime.of(2026, 12, 1, 9, 0)));
        assertTrue(f.overlaps(LocalDateTime.of(2026, 12, 1, 6, 0), LocalDateTime.of(2026, 12, 1, 12, 0)));
    }

    @Test
    void touchingPeriodsDoNotOverlap() {
        Flight f = flight("AC123", DEP, ARR, "100.00");
        assertFalse(f.overlaps(LocalDateTime.of(2026, 12, 1, 10, 0), LocalDateTime.of(2026, 12, 1, 12, 0)));
        assertFalse(f.overlaps(LocalDateTime.of(2026, 12, 1, 6, 0), LocalDateTime.of(2026, 12, 1, 8, 0)));
    }

    @Test
    void overlapsNeedsAPeriodThatEndsAfterItStarts() {
        Flight f = flight("AC123", DEP, ARR, "100.00");
        assertThrows(IllegalArgumentException.class, () -> f.overlaps(ARR, DEP));
    }

    @Test
    void priceForUsesThePricingPolicy() {
        Flight f = flight("AC123", DEP, ARR, "199.99");
        assertEquals(new BigDecimal("199.99"), f.priceFor(SeatClass.ECONOMY));
        assertEquals(new BigDecimal("299.99"), f.priceFor(SeatClass.BUSINESS));
    }
}
