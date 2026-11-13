package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import ca.ucalgary.chinookair.AppContext;
import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Promotion;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Opens the real window, so it is skipped on a machine without a display (like a CI server). */
class MainFrameSmokeTest {

    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 12, 1, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    Path folder;

    private Controllers controllers;
    private MainFrame frame;

    @BeforeEach
    void open() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        controllers = AppContext.create("jdbc:sqlite::memory:", CLOCK, folder);
        frame = new MainFrame(controllers);
    }

    @AfterEach
    void close() {
        if (frame != null) {
            frame.dispose();
        }
        if (controllers != null) {
            controllers.shutdown().run();
        }
    }

    @Test
    void theFrameHasItsTitleAndSizes() {
        assertEquals("ChinookAir", frame.getTitle());
        assertEquals(new Dimension(1000, 700), frame.getSize());
        assertEquals(new Dimension(800, 600), frame.getMinimumSize());
    }

    @Test
    void itStartsOnTheLoginScreenAndCanShowEveryScreen() {
        assertEquals(ViewId.LOGIN, frame.currentView());
        for (ViewId view : ViewId.values()) {
            frame.show(view);
            // a dashboard needs a login, so without one it sends you back to the login screen
            boolean dashboard = view.name().endsWith("_DASHBOARD");
            assertEquals(dashboard ? ViewId.LOGIN : view, frame.currentView(), view.name());
        }
        frame.show(ViewId.LOGIN);
        assertEquals(ViewId.LOGIN, frame.currentView());
    }

    // ---- the monthly promotion

    private List<Promotion> collectPromotions() {
        List<Promotion> shown = new ArrayList<>();
        frame.setPromotionDisplay(shown::add);
        return shown;
    }

    @Test
    void theFirstStartOfTheMonthShowsOnePromotionAndASecondCheckShowsNone() {
        List<Promotion> shown = collectPromotions();
        frame.showStartupPromotion();
        assertEquals(1, shown.size());
        frame.showStartupPromotion();
        assertEquals(1, shown.size());
    }

    @Test
    void openingTheWindowAsksForThePromotionOnce() throws Exception {
        List<Promotion> shown = collectPromotions();
        EventQueue.invokeAndWait(() -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_OPENED)));
        assertEquals(1, shown.size());
        EventQueue.invokeAndWait(() -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_OPENED)));
        assertEquals(1, shown.size());
    }

    @Test
    void thePromotionDialogIsModalAndHasATitle() {
        PromotionDialog dialog = new PromotionDialog(frame, new Promotion(1L, "Winter Escape", "12% off"));
        try {
            assertTrue(dialog.isModal());
            assertEquals("ChinookAir promotion", dialog.getTitle());
            assertFalse(dialog.isVisible());
        } finally {
            dialog.dispose();
        }
    }

    @Test
    void aScreenIsRequired() {
        assertThrows(IllegalArgumentException.class, () -> frame.show(null));
    }

    @Test
    void aBuiltScreenReplacesTheComingSoonPage() {
        JLabel built = new JLabel("Real login");
        frame.register(ViewId.LOGIN, built);
        frame.show(ViewId.LOGIN);
        assertNotNull(built.getParent());
    }

    @Test
    void closingTheWindowClosesTheDatabase() throws Exception {
        EventQueue.invokeAndWait(() -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)));
        assertThrows(RuntimeException.class, () -> controllers.loginController().authenticate("admin", "admin123"));
    }
}
