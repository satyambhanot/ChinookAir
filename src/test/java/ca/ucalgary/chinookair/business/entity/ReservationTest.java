package ca.ucalgary.chinookair.business.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class ReservationTest {

    private static final Customer CUSTOMER = new Customer(1L, "jsmith", "pbkdf2$1$AA$BB", "Jo Smith", "jo@x.ca", null);
    private static final Route ROUTE = new Route(1L, new Airport("YYC", "Calgary"), new Airport("YVR", "Vancouver"));
    private static final Airline AIRLINE = new Airline("AC", "Air Canada");
    private static final Aircraft AIRCRAFT = new Aircraft(1L, "A320", 30, 6);
    private static final LocalDateTime DEPARTURE = LocalDateTime.of(2026, 12, 1, 8, 0);
    private static final Flight FLIGHT = flight("AC123", DEPARTURE);
    private static final Flight OTHER_FLIGHT = flight("AC456", DEPARTURE.plusDays(1));
    private static final BigDecimal PRICE = new BigDecimal("200.00");
    private static final Clock BEFORE = clockAt(DEPARTURE.minusMinutes(1));
    private static final Clock AFTER = clockAt(DEPARTURE);

    private enum Action { ASSIGN_SEAT, CONFIRM_PAYMENT, CHANGE_FLIGHT_SAME_PRICE, CHANGE_FLIGHT_NEW_PRICE, CANCEL,
        COMPLETE }

    private static Flight flight(String number, LocalDateTime departure) {
        return new Flight(null, number, ROUTE, AIRLINE, AIRCRAFT, departure, departure.plusHours(2),
                new BigDecimal("200.00"));
    }

    private static Clock clockAt(LocalDateTime time) {
        return Clock.fixed(time.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    }

    /** A reservation that is already in the given status, built the way the data layer builds one. */
    private static Reservation in(ReservationStatus status) {
        boolean hasSeat = status != ReservationStatus.CREATED;
        return Reservation.restore(null, "CA12AB34", CUSTOMER, FLIGHT, hasSeat ? "12A" : null,
                hasSeat ? PRICE : null, LocalDateTime.of(2026, 11, 20, 9, 0), status);
    }

    private static void perform(Reservation r, Action action) {
        switch (action) {
            case ASSIGN_SEAT:
                r.assignSeat("12A", PRICE);
                break;
            case CONFIRM_PAYMENT:
                r.confirmPayment();
                break;
            case CHANGE_FLIGHT_SAME_PRICE:
                r.changeFlight(OTHER_FLIGHT, "3C", PRICE);
                break;
            case CHANGE_FLIGHT_NEW_PRICE:
                r.changeFlight(OTHER_FLIGHT, "3C", new BigDecimal("250.00"));
                break;
            case CANCEL:
                r.cancel();
                break;
            case COMPLETE:
                r.complete(AFTER);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    /** Every transition the state diagram allows: status, action, status afterwards. */
    static Stream<Arguments> legalTransitions() {
        return Stream.of(
                Arguments.of(ReservationStatus.CREATED, Action.ASSIGN_SEAT, ReservationStatus.PENDING_PAYMENT),
                Arguments.of(ReservationStatus.PENDING_PAYMENT, Action.CONFIRM_PAYMENT, ReservationStatus.CONFIRMED),
                Arguments.of(ReservationStatus.CREATED, Action.CANCEL, ReservationStatus.CANCELLED),
                Arguments.of(ReservationStatus.PENDING_PAYMENT, Action.CANCEL, ReservationStatus.CANCELLED),
                Arguments.of(ReservationStatus.CONFIRMED, Action.CANCEL, ReservationStatus.CANCELLED),
                Arguments.of(ReservationStatus.CONFIRMED, Action.CHANGE_FLIGHT_SAME_PRICE,
                        ReservationStatus.CONFIRMED),
                Arguments.of(ReservationStatus.CONFIRMED, Action.CHANGE_FLIGHT_NEW_PRICE,
                        ReservationStatus.PENDING_PAYMENT),
                Arguments.of(ReservationStatus.CONFIRMED, Action.COMPLETE, ReservationStatus.COMPLETED));
    }

    @ParameterizedTest(name = "{0} + {1} gives {2}")
    @MethodSource("legalTransitions")
    void legalTransitionsReachTheNextState(ReservationStatus from, Action action, ReservationStatus to) {
        Reservation r = in(from);
        perform(r, action);
        assertEquals(to, r.status());
    }

    @ParameterizedTest(name = "{0} + {1} is refused")
    @MethodSource("illegalTransitions")
    void everyOtherActionIsRefusedAndChangesNothing(ReservationStatus from, Action action) {
        Reservation r = in(from);
        String seatBefore = r.seatNo();
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> perform(r, action));
        assertTrue(e.getMessage().startsWith("Cannot "), e.getMessage());
        assertEquals(from, r.status());
        assertEquals(seatBefore, r.seatNo());
    }

    /** The status and action pairs that the state diagram does not allow. */
    static Stream<Arguments> illegalTransitions() {
        return Stream.of(ReservationStatus.values())
                .flatMap(s -> Stream.of(Action.values()).map(a -> Arguments.of(s, a)))
                .filter(args -> legalTransitions().noneMatch(
                        l -> l.get()[0] == args.get()[0] && l.get()[1] == args.get()[1]));
    }

    @Test
    void refusalMessageNamesTheActionAndTheStatus() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> in(ReservationStatus.CANCELLED)
                .confirmPayment());
        assertEquals("Cannot confirm the payment of a cancelled reservation", e.getMessage());
        e = assertThrows(IllegalStateException.class, () -> in(ReservationStatus.COMPLETED).cancel());
        assertEquals("Cannot cancel a completed reservation", e.getMessage());
        e = assertThrows(IllegalStateException.class, () -> in(ReservationStatus.PENDING_PAYMENT)
                .changeFlight(OTHER_FLIGHT, "3C", PRICE));
        assertEquals("Cannot change the flight of a pending payment reservation", e.getMessage());
    }

    @Test
    void completeBeforeDepartureIsRefused() {
        Reservation r = in(ReservationStatus.CONFIRMED);
        assertThrows(IllegalStateException.class, () -> r.complete(BEFORE));
        assertEquals(ReservationStatus.CONFIRMED, r.status());
        r.complete(AFTER);
        assertEquals(ReservationStatus.COMPLETED, r.status());
    }

    @Test
    void assignSeatKeepsTheSeatAndThePrice() {
        Reservation r = in(ReservationStatus.CREATED);
        assertNull(r.seatNo());
        assertNull(r.price());
        r.assignSeat("12a", new BigDecimal("200"));
        assertEquals("12A", r.seatNo());
        assertEquals(new BigDecimal("200.00"), r.price());
    }

    @Test
    void assignSeatChecksItsInput() {
        Reservation r = in(ReservationStatus.CREATED);
        assertThrows(IllegalArgumentException.class, () -> r.assignSeat(" ", PRICE));
        assertThrows(IllegalArgumentException.class, () -> r.assignSeat("12A", null));
        assertThrows(IllegalArgumentException.class, () -> r.assignSeat("12A", BigDecimal.ZERO));
        assertEquals(ReservationStatus.CREATED, r.status());
        assertNull(r.seatNo());
    }

    @Test
    void changeFlightMovesTheReservationAndKeepsTheStatusWhenThePriceIsTheSame() {
        Reservation r = in(ReservationStatus.CONFIRMED);
        r.changeFlight(OTHER_FLIGHT, "3C", new BigDecimal("200.0"));
        assertSame(OTHER_FLIGHT, r.flight());
        assertEquals("3C", r.seatNo());
        assertEquals(ReservationStatus.CONFIRMED, r.status());
    }

    @Test
    void changeFlightToADifferentPriceWaitsForPayment() {
        Reservation r = in(ReservationStatus.CONFIRMED);
        r.changeFlight(OTHER_FLIGHT, "3C", new BigDecimal("250.00"));
        assertSame(OTHER_FLIGHT, r.flight());
        assertEquals(new BigDecimal("250.00"), r.price());
        assertEquals(ReservationStatus.PENDING_PAYMENT, r.status());
    }

    @Test
    void aBadChangeFlightLeavesTheReservationAsItWas() {
        Reservation r = in(ReservationStatus.CONFIRMED);
        assertThrows(IllegalArgumentException.class, () -> r.changeFlight(null, "3C", PRICE));
        assertThrows(IllegalArgumentException.class, () -> r.changeFlight(OTHER_FLIGHT, "", PRICE));
        assertThrows(IllegalArgumentException.class, () -> r.changeFlight(OTHER_FLIGHT, "3C", null));
        assertSame(FLIGHT, r.flight());
        assertEquals("12A", r.seatNo());
        assertEquals(ReservationStatus.CONFIRMED, r.status());
    }

    @Test
    void aNewReservationStartsCreatedWithoutAnIdOrASeat() {
        Reservation r = new Reservation("ca12ab34", CUSTOMER, FLIGHT, LocalDateTime.of(2026, 11, 20, 9, 0));
        assertEquals(ReservationStatus.CREATED, r.status());
        assertNull(r.id());
        assertNull(r.seatNo());
        assertEquals("ca12ab34", r.confirmationNumber());
        assertSame(CUSTOMER, r.customer());
        assertSame(FLIGHT, r.flight());
    }

    @Test
    void theIdCanBeAssignedOnlyOnce() {
        Reservation r = in(ReservationStatus.CREATED);
        r.assignId(5L);
        assertEquals(5L, r.id());
        assertThrows(IllegalStateException.class, () -> r.assignId(6L));
    }

    @ParameterizedTest
    @EnumSource(ReservationStatus.class)
    void restoreGivesTheStateOfTheStatus(ReservationStatus status) {
        assertEquals(status, in(status).status());
    }

    @Test
    void restoreNeedsASeatAndAPriceForAHeldReservation() {
        for (ReservationStatus status : new ReservationStatus[] {ReservationStatus.PENDING_PAYMENT,
            ReservationStatus.CONFIRMED, ReservationStatus.COMPLETED}) {
            assertThrows(IllegalArgumentException.class, () -> Reservation.restore(null, "CA12AB34", CUSTOMER,
                    FLIGHT, null, null, LocalDateTime.of(2026, 11, 20, 9, 0), status));
        }
    }

    @Test
    void onlyHeldReservationsAreActive() {
        assertTrue(in(ReservationStatus.CREATED).isActive());
        assertTrue(in(ReservationStatus.PENDING_PAYMENT).isActive());
        assertTrue(in(ReservationStatus.CONFIRMED).isActive());
        assertFalse(in(ReservationStatus.CANCELLED).isActive());
        assertFalse(in(ReservationStatus.COMPLETED).isActive());
    }

    @Test
    void aReservationNeedsItsBasicData() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 20, 9, 0);
        assertThrows(IllegalArgumentException.class, () -> new Reservation(" ", CUSTOMER, FLIGHT, now));
        assertThrows(IllegalArgumentException.class, () -> new Reservation("CA12AB34", null, FLIGHT, now));
        assertThrows(IllegalArgumentException.class, () -> new Reservation("CA12AB34", CUSTOMER, null, now));
        assertThrows(IllegalArgumentException.class, () -> new Reservation("CA12AB34", CUSTOMER, FLIGHT, null));
    }
}
