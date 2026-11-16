package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Dimension;
import org.junit.jupiter.api.Test;

/** The window must fit on the screen it opens on, also a small screen or one drawn at 125 % or 150 %. */
class MainFrameSizeTest {

    @Test
    void aBigScreenGetsTheUsualWindowAndTheUsualSmallest() {
        Dimension room = new Dimension(1900, 1000);
        assertEquals(new Dimension(960, 640), MainFrame.smallestWindow(room));
        assertEquals(new Dimension(1000, 700), MainFrame.startingWindow(room));
    }

    @Test
    void aLaptopAt125PercentFitsOnTheScreen() {
        // 1366 x 768 pixels, less the task bar, drawn at 125 %
        Dimension room = new Dimension(1093, 580);
        assertEquals(new Dimension(960, 580), MainFrame.smallestWindow(room));
        assertEquals(new Dimension(1000, 580), MainFrame.startingWindow(room));
    }

    @Test
    void aLaptopAt150PercentFitsOnTheScreen() {
        Dimension room = new Dimension(910, 485);
        assertEquals(new Dimension(910, 485), MainFrame.smallestWindow(room));
        assertEquals(new Dimension(910, 485), MainFrame.startingWindow(room));
    }

    @Test
    void aTinyScreenNeverShrinksTheWindowBelowTheFloor() {
        Dimension room = new Dimension(500, 300);
        assertEquals(new Dimension(760, 440), MainFrame.smallestWindow(room));
        assertEquals(new Dimension(760, 440), MainFrame.startingWindow(room));
    }
}
