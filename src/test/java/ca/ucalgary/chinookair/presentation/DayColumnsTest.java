package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.ucalgary.chinookair.AppContext;
import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Flight;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DayColumnsTest {

    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    static Path folder;

    private static Controllers controllers;

    @BeforeAll
    static void startTheProgramParts() {
        controllers = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
    }

    @AfterAll
    static void shutDown() {
        controllers.shutdown().run();
    }

    private static DayColumns twoDays() {
        List<Flight> flights = controllers.scheduleController().schedule(
                controllers.session().currentUser().orElseThrow(), LocalDate.of(2026, 12, 2),
                LocalDate.of(2026, 12, 3));
        ScheduleTableModel base = new ScheduleTableModel();
        base.setFlights(flights);
        return new DayColumns(base);
    }

    @Test
    void theDayIsWrittenOnceOnTheFirstFlightOfEachDay() {
        controllers.session().login(controllers.loginController().authenticate("agent1", "agent123"));
        DayColumns model = twoDays();
        assertEquals("Wed, Dec 2", model.getValueAt(0, 0));
        int rowsOfTheFirstDay = 0;
        while (rowsOfTheFirstDay < model.getRowCount() && (rowsOfTheFirstDay == 0
                || model.getValueAt(rowsOfTheFirstDay, 0).equals(""))) {
            rowsOfTheFirstDay++;
        }
        assertEquals("Thu, Dec 3", model.getValueAt(rowsOfTheFirstDay, 0));
        for (int row = 1; row < rowsOfTheFirstDay; row++) {
            assertEquals("", model.getValueAt(row, 0));
        }
    }

    @Test
    void theTimesAreJustTheTimeOfDay() {
        controllers.session().login(controllers.loginController().authenticate("agent1", "agent123"));
        DayColumns model = twoDays();
        assertEquals("Departs", model.getColumnName(3));
        assertEquals("Arrives", model.getColumnName(4));
        assertEquals("7:00 AM", model.getValueAt(0, 3));
        assertEquals("8:30 AM", model.getValueAt(0, 4));
        assertEquals(6, model.getColumnCount());
    }

    @Test
    void theOtherColumnsAreTheFlightTheRouteAndTheAircraft() {
        controllers.session().login(controllers.loginController().authenticate("agent1", "agent123"));
        DayColumns model = twoDays();
        assertEquals("AC101", model.getValueAt(0, 1));
        assertEquals("YYC \u2192 YVR", model.getValueAt(0, 2));
        assertEquals("Airbus A320", model.getValueAt(0, 5));
    }
}
