package ca.ucalgary.chinookair.business.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Administrator;
import ca.ucalgary.chinookair.business.entity.Aircraft;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.FlightAgent;
import ca.ucalgary.chinookair.data.DaoFactory;
import ca.ucalgary.chinookair.data.TestDatabase;
import ca.ucalgary.chinookair.data.TestWorld;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScheduleControllerTest {

    private static final String HASH = "pbkdf2$1$AAAA$BBBB";
    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private Connection db;
    private TestWorld world;
    private DaoFactory daos;
    private ScheduleController controller;
    private Administrator admin;
    private FlightAgent agent;
    private Customer customer;

    @BeforeEach
    void open() {
        db = TestDatabase.inMemory();
        world = new TestWorld(db);
        daos = DaoFactory.create(db);
        controller = new ScheduleController(daos.flightDao(), CLOCK);
        admin = (Administrator) daos.userDao().save(new Administrator(null, "admin", HASH));
        agent = (FlightAgent) daos.userDao().save(new FlightAgent(null, "agent1", HASH));
        customer = daos.customerDao().findById(1).orElseThrow();
    }

    @AfterEach
    void close() throws SQLException {
        db.close();
    }

    private Flight flight(String number, Aircraft aircraft, LocalDateTime departure) {
        return daos.flightDao().save(world.flight(number, world.yycYvr, world.ac, aircraft, departure));
    }

    private static LocalDateTime at(int day, int hour) {
        return LocalDateTime.of(2026, 12, day, hour, 0);
    }

    private static List<String> numbers(List<Flight> flights) {
        return flights.stream().map(Flight::flightNumber).toList();
    }

    // ---- schedule

    @Test
    void theScheduleHoldsTheFlightsOfTheDaysEarliestFirst() {
        flight("AC400", world.small, at(4, 8));
        flight("AC300", world.small, at(3, 23));
        flight("AC100", world.small, at(1, 8));
        flight("AC200", world.small, at(2, 12));
        List<Flight> found = controller.schedule(agent, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3));
        assertEquals(List.of("AC100", "AC200", "AC300"), numbers(found));
    }

    @Test
    void bothEndDaysAreIncludedAndOneDayWorks() {
        flight("AC100", world.small, at(1, 0));
        flight("AC200", world.small, at(2, 23));
        assertEquals(List.of("AC100"), numbers(controller.schedule(agent, LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 1))));
        assertEquals(List.of("AC200"), numbers(controller.schedule(agent, LocalDate.of(2026, 12, 2),
                LocalDate.of(2026, 12, 2))));
        assertTrue(controller.schedule(agent, LocalDate.of(2026, 12, 5), LocalDate.of(2026, 12, 6)).isEmpty());
    }

    @Test
    void aRemovedFlightIsNotInTheSchedule() {
        Flight gone = flight("AC100", world.small, at(1, 8));
        flight("AC200", world.small, at(2, 8));
        daos.flightDao().archive(gone.id());
        assertEquals(List.of("AC200"), numbers(controller.schedule(admin, LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 3))));
    }

    @Test
    void agentsAndAdministratorsMayLookButCustomersMayNot() {
        flight("AC100", world.small, at(2, 8));
        LocalDate from = LocalDate.of(2026, 12, 1);
        LocalDate to = LocalDate.of(2026, 12, 3);
        assertEquals(1, controller.schedule(agent, from, to).size());
        assertEquals(1, controller.schedule(admin, from, to).size());
        assertThrows(AccessDeniedException.class, () -> controller.schedule(customer, from, to));
        assertThrows(AccessDeniedException.class, () -> controller.schedule(null, from, to));
    }

    @Test
    void theDatesMustMakeSense() {
        assertThrows(IllegalArgumentException.class, () -> controller.schedule(agent, null, LocalDate.of(2026, 12, 3)));
        assertThrows(IllegalArgumentException.class, () -> controller.schedule(agent, LocalDate.of(2026, 12, 3), null));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> controller.schedule(agent, LocalDate.of(2026, 12, 3), LocalDate.of(2026, 12, 1)));
        assertTrue(e.getMessage().contains("cannot be before"), e.getMessage());
    }

    // ---- reschedule

    @Test
    void aFlightMovesToAFreeSlot() {
        Flight flight = flight("AC100", world.small, at(2, 8));
        Flight moved = controller.reschedule(admin, flight.id(), at(3, 14), at(3, 16));
        assertEquals(at(3, 14), moved.departure());
        Flight stored = daos.flightDao().findById(flight.id()).orElseThrow();
        assertEquals(at(3, 14), stored.departure());
        assertEquals(at(3, 16), stored.arrival());
        assertEquals("AC100", stored.flightNumber());
        assertEquals(flight.basePrice(), stored.basePrice());
    }

    @Test
    void aClashWithAnotherFlightOnTheSameAircraftIsRefused() {
        Flight first = flight("AC100", world.small, at(2, 8));
        flight("AC200", world.small, at(2, 12));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> controller.reschedule(admin, first.id(), at(2, 11), at(2, 13)));
        assertTrue(e.getMessage().contains("Aircraft is already assigned"), e.getMessage());
        assertEquals(at(2, 8), daos.flightDao().findById(first.id()).orElseThrow().departure());
    }

    @Test
    void touchingFlightsAndFlightsOfOtherAircraftAreFine() {
        Flight first = flight("AC100", world.small, at(2, 8));
        flight("AC200", world.small, at(2, 12));
        flight("AC300", world.big, at(2, 10));
        // 10:00-12:00 only touches AC200 at 12:00 and the other aircraft does not matter
        assertEquals(at(2, 10), controller.reschedule(admin, first.id(), at(2, 10), at(2, 12)).departure());
    }

    @Test
    void aFlightMayShiftWithinItsOwnOldSlot() {
        Flight flight = flight("AC100", world.small, at(2, 8));
        assertEquals(at(2, 9), controller.reschedule(admin, flight.id(), at(2, 9), at(2, 11)).departure());
    }

    @Test
    void aDepartedFlightCannotMove() {
        Flight past = flight("AC100", world.small, at(1, 8));
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> controller.reschedule(admin, past.id(), at(3, 8), at(3, 10)));
        assertTrue(e.getMessage().contains("already departed"), e.getMessage());
        assertEquals(at(1, 8), daos.flightDao().findById(past.id()).orElseThrow().departure());
    }

    @Test
    void theNewTimesMustBeValidAndInTheFuture() {
        Flight flight = flight("AC100", world.small, at(2, 8));
        assertThrows(IllegalArgumentException.class, () -> controller.reschedule(admin, flight.id(), at(3, 10),
                at(3, 8)));
        assertThrows(IllegalArgumentException.class, () -> controller.reschedule(admin, flight.id(), at(3, 10),
                at(3, 10)));
        assertThrows(IllegalArgumentException.class, () -> controller.reschedule(admin, flight.id(), null, null));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> controller.reschedule(admin, flight.id(), LocalDateTime.of(2026, 11, 30, 8, 0),
                        LocalDateTime.of(2026, 11, 30, 10, 0)));
        assertTrue(e.getMessage().contains("must be in the future"), e.getMessage());
    }

    @Test
    void onlyAnAdministratorMayRescheduleAndTheFlightMustBeThere() {
        Flight flight = flight("AC100", world.small, at(2, 8));
        assertThrows(AccessDeniedException.class, () -> controller.reschedule(agent, flight.id(), at(3, 8), at(3, 10)));
        assertThrows(AccessDeniedException.class,
                () -> controller.reschedule(customer, flight.id(), at(3, 8), at(3, 10)));
        assertThrows(AccessDeniedException.class, () -> controller.reschedule(null, flight.id(), at(3, 8), at(3, 10)));
        assertThrows(IllegalArgumentException.class, () -> controller.reschedule(admin, 999, at(3, 8), at(3, 10)));
        assertEquals(at(2, 8), daos.flightDao().findById(flight.id()).orElseThrow().departure());
    }

    @Test
    void aRemovedFlightCannotBeRescheduled() {
        Flight flight = flight("AC100", world.small, at(2, 8));
        daos.flightDao().archive(flight.id());
        assertThrows(IllegalStateException.class, () -> controller.reschedule(admin, flight.id(), at(3, 8), at(3, 10)));
    }
}
