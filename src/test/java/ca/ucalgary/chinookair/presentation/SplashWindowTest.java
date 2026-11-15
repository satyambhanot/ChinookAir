package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Builds the window without showing it; skipped on a machine without a display. */
class SplashWindowTest {

    @BeforeEach
    void needsADisplay() {
        assumeFalse(GraphicsEnvironment.isHeadless());
    }

    @Test
    void itSaysTheProgramIsGettingReady() {
        SplashWindow splash = new SplashWindow();
        assertEquals("Getting ChinookAir ready…", splash.statusText());
    }

    @Test
    void theBarKeepsMovingBecauseNoPercentageIsKnown() {
        assertTrue(new SplashWindow().isMoving());
    }

    @Test
    void theStatusCanChange() {
        SplashWindow splash = new SplashWindow();
        splash.setStatus("Opening the flights");
        assertEquals("Opening the flights", splash.statusText());
    }

    @Test
    void theWindowIsBigEnoughForTheLogo() {
        SplashWindow splash = new SplashWindow();
        assertTrue(splash.getWidth() >= 380);
        assertTrue(splash.getHeight() > 200);
    }
}
