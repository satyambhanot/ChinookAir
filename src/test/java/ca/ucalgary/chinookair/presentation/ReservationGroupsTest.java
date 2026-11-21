package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.AppContext;
import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.control.SearchCriteria;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Reservation;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReservationGroupsTest {

    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 12, 1, 0, 0);

    @TempDir
    static Path folder;

    private static Controllers controllers;
    private static Customer customer;

    @BeforeAll
    static void startTheProgramParts() {
        controllers = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
        customer = controllers.customerController().register("grp", "password1", "Grace Group", "g@x.ca", null);
    }

    @AfterAll
    static void shutDown() {
        controllers.shutdown().run();
    }

    private static Flight flightOn(int day) {
        return controllers.flightSearchController()
                .search(new SearchCriteria("YYC", "YVR", LocalDate.of(2026, 12, day), null)).get(0).flight();
    }

    private static Reservation reservation(String number, int day) {
        return new Reservation(number, customer, flightOn(day), LocalDateTime.of(2026, 11, 20, 10, 0));
    }

    @Test
    void aFlightThatHasNotLeftIsUpcomingAndOneThatHasIsPast() {
        assertEquals(ReservationGroups.Group.UPCOMING, ReservationGroups.of(reservation("A", 5), NOW));
        assertEquals(ReservationGroups.Group.UPCOMING, ReservationGroups.of(reservation("A", 2), NOW));
        assertEquals(ReservationGroups.Group.PAST,
                ReservationGroups.of(reservation("A", 2), NOW.plusDays(2)));
    }

    @Test
    void aCancelledReservationIsCancelledWhateverTheDate() {
        Reservation r = reservation("C", 5);
        r.cancel();
        assertEquals(ReservationGroups.Group.CANCELLED, ReservationGroups.of(r, NOW));
        assertEquals(ReservationGroups.Group.CANCELLED, ReservationGroups.of(r, NOW.plusDays(30)));
    }

    @Test
    void theSectionsAreInOrderAndAheadComesSoonestFirst() {
        Reservation late = reservation("LATE", 9);
        Reservation soon = reservation("SOON", 3);
        Reservation gone = reservation("GONE", 2);
        Reservation off = reservation("OFF", 6);
        off.cancel();
        Map<ReservationGroups.Group, List<Reservation>> groups =
                ReservationGroups.split(List.of(late, soon, gone, off), NOW.plusDays(2));
        assertEquals(List.of(ReservationGroups.Group.UPCOMING, ReservationGroups.Group.PAST,
                ReservationGroups.Group.CANCELLED), List.copyOf(groups.keySet()));
        assertEquals(List.of("SOON", "LATE"), groups.get(ReservationGroups.Group.UPCOMING).stream()
                .map(Reservation::confirmationNumber).toList());
        assertEquals(List.of("GONE"), groups.get(ReservationGroups.Group.PAST).stream()
                .map(Reservation::confirmationNumber).toList());
    }

    @Test
    void aSectionWithNothingInItIsLeftOut() {
        Map<ReservationGroups.Group, List<Reservation>> groups =
                ReservationGroups.split(List.of(reservation("ONLY", 5)), NOW);
        assertEquals(1, groups.size());
        assertFalse(groups.containsKey(ReservationGroups.Group.CANCELLED));
    }

    @Test
    void typingMatchesTheNumberTheFlightAndTheAirportsWhateverTheCapitals() {
        Reservation r = reservation("ABC123", 5);
        assertTrue(ReservationGroups.matches(r, "abc"));
        assertTrue(ReservationGroups.matches(r, " AC10 "));
        assertTrue(ReservationGroups.matches(r, "yvr"));
        assertTrue(ReservationGroups.matches(r, ""));
        assertTrue(ReservationGroups.matches(r, null));
        assertFalse(ReservationGroups.matches(r, "zzz"));
    }
}
