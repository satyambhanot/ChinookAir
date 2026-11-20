package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Aircraft;
import ca.ucalgary.chinookair.business.entity.Airline;
import ca.ucalgary.chinookair.business.entity.Airport;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Route;
import ca.ucalgary.chinookair.business.entity.Seat;
import ca.ucalgary.chinookair.business.entity.SeatLayout;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SeatMapPanelTest {

    private static final Aircraft AIRCRAFT = new Aircraft(1L, "Small", 4, 4);
    private static final Flight FLIGHT = new Flight(1L, "AC123",
            new Route(1L, new Airport("YYC", "Calgary"), new Airport("YVR", "Vancouver")),
            new Airline("AC", "Air Canada"), AIRCRAFT, LocalDateTime.of(2026, 12, 2, 9, 0),
            LocalDateTime.of(2026, 12, 2, 10, 30), new BigDecimal("200.00"));

    private final List<Seat> picked = new ArrayList<>();
    private SeatMapPanel map;

    @BeforeAll
    static void theme() {
        Theme.install();
    }

    @BeforeEach
    void drawMapWithSeat2BTaken() {
        List<Seat> free = SeatLayout.generate(AIRCRAFT).stream().filter(s -> !s.number().equals("2B")).toList();
        map = new SeatMapPanel();
        map.show(FLIGHT, SeatGridModel.of(AIRCRAFT, free), picked::add);
    }

    @Test
    void everySeatOfTheAircraftHasAButton() {
        for (Seat seat : SeatLayout.generate(AIRCRAFT)) {
            assertNotNull(map.seatButtonFor(seat.number()), seat.number());
        }
    }

    @Test
    void aFreeSeatTellsTheOwnerWhenItIsPicked() {
        map.seatButtonFor("3C").doClick(0);
        assertEquals(1, picked.size());
        assertEquals("3C", picked.get(0).number());
    }

    @Test
    void aTakenSeatIsDisabledAndSaysSo() {
        JToggleButton taken = map.seatButtonFor("2B");
        assertFalse(taken.isEnabled());
        assertEquals("Seat 2B, taken", taken.getAccessibleContext().getAccessibleName());
        assertTrue(picked.isEmpty());
    }

    @Test
    void aFreeSeatSaysItsClassAndPrice() {
        String name = map.seatButtonFor("1A").getAccessibleContext().getAccessibleName();
        assertTrue(name.startsWith("Seat 1A, Business, "), name);
        assertTrue(name.contains("$"), name);
    }

    @Test
    void theArrowKeysAreBoundOnFreeSeatsButNotOnTakenOnes() {
        for (String key : new String[] {"UP", "DOWN", "LEFT", "RIGHT"}) {
            KeyStroke stroke = KeyStroke.getKeyStroke(key);
            assertEquals("seat-" + key, map.seatButtonFor("3C").getInputMap(JComponent.WHEN_FOCUSED).get(stroke));
            assertNull(map.seatButtonFor("2B").getInputMap(JComponent.WHEN_FOCUSED).get(stroke));
        }
    }

    @Test
    void anArrowKeyAtTheEdgeOfTheMapDoesNothingWrong() {
        map.seatButtonFor("1A").getActionMap().get("seat-UP")
                .actionPerformed(new java.awt.event.ActionEvent(map, 0, "seat-UP"));
        map.seatButtonFor("4D").getActionMap().get("seat-RIGHT")
                .actionPerformed(new java.awt.event.ActionEvent(map, 0, "seat-RIGHT"));
    }

    @Test
    void drawingAgainReplacesTheEarlierSeats() {
        int before = map.getComponentCount();
        map.show(FLIGHT, SeatGridModel.of(AIRCRAFT, SeatLayout.generate(AIRCRAFT)), picked::add);
        assertEquals(before, map.getComponentCount());
        assertTrue(map.seatButtonFor("2B").isEnabled());
    }

    @Test
    void clearTakesEverythingOffTheScreen() {
        map.clear();
        assertEquals(0, map.getComponentCount());
    }
}
