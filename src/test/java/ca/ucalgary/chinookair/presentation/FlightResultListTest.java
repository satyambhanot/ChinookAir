package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.AppContext;
import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.control.FlightOffer;
import ca.ucalgary.chinookair.business.control.SearchCriteria;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlightResultListTest {

    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    static Path folder;

    private static Controllers controllers;
    private static List<FlightOffer> offers;

    private FlightResultList list;

    @BeforeAll
    static void startTheProgramParts() {
        Theme.install();
        controllers = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
        // flights of different times and lengths from one day, with made-up prices so the order is known
        List<FlightOffer> found = new ArrayList<>();
        for (int day = 2; day <= 3; day++) {
            for (String[] route : new String[][] {{"YYC", "YVR"}, {"YYC", "YYZ"}, {"YYC", "YEG"}, {"YVR", "YYC"}}) {
                found.addAll(controllers.flightSearchController()
                        .search(new SearchCriteria(route[0], route[1], LocalDate.of(2026, 12, day), null)));
            }
        }
        offers = new ArrayList<>();
        int price = 300;
        for (FlightOffer offer : found) {
            offers.add(new FlightOffer(offer.flight(), offer.seatsLeft(), new BigDecimal(price)));
            price -= 50;
        }
    }

    @AfterAll
    static void shutDown() {
        controllers.shutdown().run();
    }

    @BeforeEach
    void anEmptyList() {
        list = new FlightResultList("Nothing yet");
    }

    @Test
    void theTestDataHasFlightsOfDifferentTimesPricesAndLengths() {
        assertTrue(offers.size() >= 4, "found " + offers.size());
        assertTrue(offers.stream().map(o -> FlightResultList.minutes(o.flight())).distinct().count() >= 3);
    }

    @Test
    void theEarliestComesFirstByDefault() {
        list.setOffers(offers);
        List<FlightOffer> shown = list.offers();
        for (int i = 1; i < shown.size(); i++) {
            assertTrue(!shown.get(i - 1).flight().departure().isAfter(shown.get(i).flight().departure()));
        }
    }

    @Test
    void cheapestPutsThePriceInOrder() {
        list.setOffers(offers);
        list.sortBy(FlightResultList.Sort.CHEAPEST);
        List<FlightOffer> shown = list.offers();
        for (int i = 1; i < shown.size(); i++) {
            assertTrue(shown.get(i - 1).fromPrice().compareTo(shown.get(i).fromPrice()) <= 0);
        }
    }

    @Test
    void shortestPutsTheLengthInOrder() {
        list.setOffers(offers);
        list.sortBy(FlightResultList.Sort.SHORTEST);
        List<FlightOffer> shown = list.offers();
        for (int i = 1; i < shown.size(); i++) {
            assertTrue(FlightResultList.minutes(shown.get(i - 1).flight())
                    <= FlightResultList.minutes(shown.get(i).flight()));
        }
    }

    @Test
    void sortingKeepsTheChosenFlightChosen() {
        list.setOffers(offers);
        list.setSelectedIndex(0);
        FlightOffer chosen = list.selectedOffer();
        list.sortBy(FlightResultList.Sort.CHEAPEST);
        assertEquals(chosen, list.selectedOffer());
        list.sortBy(FlightResultList.Sort.SHORTEST);
        assertEquals(chosen, list.selectedOffer());
    }

    @Test
    void newResultsReplaceTheOldAndChooseNothing() {
        list.setOffers(offers);
        list.setSelectedIndex(1);
        list.setOffers(offers.subList(0, 2));
        assertEquals(2, list.offers().size());
        assertNull(list.selectedOffer());
    }

    @Test
    void theSortOrderIsKeptWhenNewResultsComeIn() {
        list.setOffers(offers);
        list.sortBy(FlightResultList.Sort.CHEAPEST);
        list.setOffers(offers);
        assertEquals(FlightResultList.Sort.CHEAPEST, list.sort());
        assertEquals(offers.stream().map(FlightOffer::fromPrice).min(BigDecimal::compareTo).orElseThrow(),
                list.offers().get(0).fromPrice());
    }

    @Test
    void anEmptyListSaysWhatToDoAndNothingIsChosen() {
        assertEquals("Nothing yet", list.emptyText());
        list.setEmptyText("No flights match this search.");
        assertEquals("No flights match this search.", list.emptyText());
        assertNull(list.selectedOffer());
    }

    @Test
    void aCardNamesTheFlightForAScreenReader() {
        list.setOffers(offers);
        java.awt.Component card = list.getCellRenderer().getListCellRendererComponent(list,
                list.offers().get(0), 0, false, false);
        String name = ((javax.swing.JComponent) card).getAccessibleContext().getAccessibleName();
        assertTrue(name.contains(list.offers().get(0).flight().flightNumber()), name);
        assertTrue(name.contains("seats left"), name);
    }

    @Test
    void theRouteLineOfACardHasRoomBetweenTheTwoTimes() {
        list.setOffers(offers);
        javax.swing.JComponent card = (javax.swing.JComponent) list.getCellRenderer()
                .getListCellRendererComponent(list, list.offers().get(0), 0, false, false);
        card.setSize(1000, FlightResultList.CELL_HEIGHT);
        layoutAll(card);
        FlightLine line = find(card, FlightLine.class);
        assertTrue(line.getWidth() >= 100, "the line is " + line.getWidth() + " px wide");
        assertTrue(line.text().length() > 0, "it says how long the flight takes");
    }

    private static void layoutAll(java.awt.Container container) {
        container.doLayout();
        for (java.awt.Component child : container.getComponents()) {
            if (child instanceof java.awt.Container) {
                layoutAll((java.awt.Container) child);
            }
        }
    }

    private static <T extends java.awt.Component> T find(java.awt.Container root, Class<T> type) {
        for (java.awt.Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof java.awt.Container) {
                T inside = findOrNull((java.awt.Container) child, type);
                if (inside != null) {
                    return inside;
                }
            }
        }
        throw new AssertionError("no " + type.getSimpleName());
    }

    private static <T extends java.awt.Component> T findOrNull(java.awt.Container root, Class<T> type) {
        try {
            return find(root, type);
        } catch (AssertionError e) {
            return null;
        }
    }
}
