package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class EmptyStateTest {

    @Test
    void itSaysWhatIsMissingAndWhatToDo() {
        EmptyState state = new EmptyState("No reservations yet", "Find a flight to book your first trip.");
        assertEquals("No reservations yet", state.title());
        assertEquals("Find a flight to book your first trip.", state.hint());
        assertNull(state.action());
    }

    @Test
    void theActionButtonRunsItsTask() {
        EmptyState state = new EmptyState("Empty", "Nothing here");
        AtomicInteger runs = new AtomicInteger();
        state.setAction("Find flights", runs::incrementAndGet);
        assertNotNull(state.action());
        assertEquals("Find flights", state.action().getText());
        state.action().doClick(0);
        assertEquals(1, runs.get());
    }

    @Test
    void settingANewActionReplacesTheOldOneAndNullRemovesIt() {
        EmptyState state = new EmptyState("Empty", "Nothing here");
        state.setAction("One", () -> { });
        state.setAction("Two", () -> { });
        assertEquals("Two", state.action().getText());
        int parts = state.getComponentCount();
        state.setAction(null, null);
        assertNull(state.action());
        assertEquals(parts - 2, state.getComponentCount());
    }
}
