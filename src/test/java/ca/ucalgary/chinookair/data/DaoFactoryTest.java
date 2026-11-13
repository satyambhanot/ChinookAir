package ca.ucalgary.chinookair.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Administrator;
import ca.ucalgary.chinookair.business.entity.Aircraft;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.Route;
import ca.ucalgary.chinookair.business.entity.SeatLayout;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DaoFactoryTest {

    private Connection db;
    private DaoFactory factory;

    @BeforeEach
    void open() {
        db = TestDatabase.inMemory();
        factory = DaoFactory.create(db);
    }

    @AfterEach
    void close() throws SQLException {
        db.close();
    }

    @Test
    void theFlightDaoWorks() {
        assertTrue(factory.flightDao().findById(1).isEmpty());
        assertFalse(factory.flightDao().existsByFlightNumber("AC123"));
    }

    @Test
    void everyAccessorGivesADao() {
        assertNotNull(factory.airlineDao());
        assertNotNull(factory.airportDao());
        assertNotNull(factory.aircraftDao());
        assertNotNull(factory.routeDao());
        assertNotNull(factory.flightDao());
        assertNotNull(factory.seatDao());
        assertNotNull(factory.userDao());
        assertNotNull(factory.customerDao());
        assertNotNull(factory.reservationDao());
        assertNotNull(factory.paymentDao());
        assertNotNull(factory.promotionDao());
        assertNotNull(factory.appStateDao());
    }

    @Test
    void eachDaoIsBuiltOnceAndShared() {
        assertSame(factory.flightDao(), factory.flightDao());
        assertSame(factory.reservationDao(), factory.reservationDao());
    }

    @Test
    void aConnectionIsRequired() {
        assertThrows(IllegalArgumentException.class, () -> DaoFactory.create(null));
    }

    @Test
    void theDaosWorkTogetherOnOneConnection() {
        factory.airportDao().save(TestFixtures.airport("YYC"));
        factory.airportDao().save(TestFixtures.airport("YVR"));
        factory.airlineDao().save(TestFixtures.airline("AC"));
        Aircraft aircraft = factory.aircraftDao().save(TestFixtures.aircraft());
        Route route = factory.routeDao().save(TestFixtures.route("YYC", "YVR"));
        Flight flight = factory.flightDao().save(new Flight(null, "AC123", route, TestFixtures.airline("AC"),
                aircraft, LocalDateTime.of(2026, 12, 1, 8, 0), LocalDateTime.of(2026, 12, 1, 10, 0),
                new BigDecimal("200.00")));
        Customer customer = factory.customerDao().save(
                new Customer(null, "jsmith", "pbkdf2$1$AAAA$BBBB", "Jo Smith", "j@x.ca", null));
        factory.seatDao().createSeats(flight.id(), SeatLayout.generate(aircraft));

        Reservation reservation = new Reservation("CA12AB34", customer, flight, LocalDateTime.of(2026, 11, 20, 9, 0));
        reservation.assignSeat("1A", new BigDecimal("300.00"));
        factory.reservationDao().save(reservation);

        Reservation found = factory.reservationDao().findByConfirmationNumber("CA12AB34").orElseThrow();
        assertEquals("Jo Smith", found.customer().name());
        assertEquals("AC123", found.flight().flightNumber());
        assertEquals(179, factory.seatDao().countAvailable(flight.id()));
        assertTrue(factory.userDao().save(new Administrator(null, "admin", "pbkdf2$1$AAAA$BBBB")).id() > 0);
    }
}
