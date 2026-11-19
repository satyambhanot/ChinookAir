package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.AppContext;
import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Flight;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ItineraryCardTest {

    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    static Path folder;

    private static Controllers controllers;
    private static Flight flight;

    @BeforeAll
    static void startTheProgramParts() {
        Theme.install();
        controllers = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
        flight = controllers.flightSearchController().search(new ca.ucalgary.chinookair.business.control
                .SearchCriteria("YYC", "YVR", LocalDate.of(2026, 12, 2), null)).get(0).flight();
    }

    @AfterAll
    static void shutDown() {
        controllers.shutdown().run();
    }

    private static ItineraryCard card() {
        ItineraryCard card = new ItineraryCard(flight, "Casey Customer", "5C (Economy)", "Amount paid", "$184.00");
        card.setSize(700, 260);
        layout(card);
        return card;
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container) {
                layout((Container) child);
            }
        }
    }

    private static void texts(Container root, List<String> out) {
        for (Component c : root.getComponents()) {
            if (c instanceof JLabel) {
                out.add(((JLabel) c).getText());
            }
            if (c instanceof Container) {
                texts((Container) c, out);
            }
        }
    }

    @Test
    void itListsTheLinesOfTheTrip() {
        List<String> labels = card().rows().stream().map(SummaryCard.Row::label).toList();
        assertEquals(List.of("Passenger", "Flight", "Route", "Departs", "Seat", "Amount paid"), labels);
        assertEquals("YYC → YVR", card().rows().get(2).value());
        assertTrue(card().rows().get(3).value().contains("2026"));
    }

    @Test
    void theBandShowsBothAirportsTheirTimesCitiesAndTheDuration() {
        List<String> shown = new ArrayList<>();
        texts(card(), shown);
        assertTrue(shown.contains("YYC") && shown.contains("YVR"), shown.toString());
        assertTrue(shown.stream().anyMatch(t -> t.contains("7:00 AM") && t.contains("Calgary")), shown.toString());
        assertTrue(shown.stream().anyMatch(t -> t.contains("8:30 AM") && t.contains("Vancouver")), shown.toString());
        assertTrue(shown.contains("Passenger") && shown.contains("Casey Customer"), shown.toString());
        assertTrue(shown.contains("$184.00"));
    }

    @Test
    void theTopIsNavyAndTheBodyIsWhiteWithRoundedCorners() {
        ItineraryCard card = card();
        BufferedImage image = new BufferedImage(card.getWidth(), card.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Palette.PAGE);
        g.fillRect(0, 0, card.getWidth(), card.getHeight());
        card.paint(g);
        g.dispose();
        assertEquals(Palette.NAVY_900.getRGB(), image.getRGB(card.getWidth() / 2, 6));
        assertEquals(Palette.SURFACE.getRGB(), image.getRGB(card.getWidth() / 2, card.getHeight() - 20));
        assertEquals(Palette.PAGE.getRGB(), image.getRGB(0, 0), "the corner is rounded off");
    }
}
