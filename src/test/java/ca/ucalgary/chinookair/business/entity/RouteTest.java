package ca.ucalgary.chinookair.business.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RouteTest {

    private static final Airport YYC = new Airport("YYC", "Calgary");
    private static final Airport YVR = new Airport("YVR", "Vancouver");

    @Test
    void originAndDestinationMustDiffer() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new Route(null, YYC, new Airport("yyc", "Calgary")));
        assertEquals("Origin and destination must be different", e.getMessage());
    }

    @Test
    void bothAirportsAreRequired() {
        assertThrows(IllegalArgumentException.class, () -> new Route(null, null, YVR));
        assertThrows(IllegalArgumentException.class, () -> new Route(null, YYC, null));
    }

    @Test
    void aRouteKeepsItsAirportsAndHasNoIdBeforeSaving() {
        Route route = new Route(null, YYC, YVR);
        assertNull(route.id());
        assertEquals(YYC, route.origin());
        assertEquals(YVR, route.destination());
    }

    @Test
    void theReverseRouteIsADifferentRoute() {
        assertNotEquals(new Route(null, YYC, YVR), new Route(null, YVR, YYC));
    }
}
