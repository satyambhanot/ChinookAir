package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.swing.JComponent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The Tab key must reach every control of every screen, so the program can be used without a mouse.
 */
class KeyboardTraversalTest {

    @TempDir
    static Path folder;

    private static EveryScreen every;

    @BeforeAll
    static void buildEveryScreen() {
        every = EveryScreen.build(folder);
    }

    @AfterAll
    static void shutDown() {
        every.shutDown();
    }

    private static final Path SOURCES = Path.of("src/main/java/ca/ucalgary/chinookair/presentation");

    /**
     * Swing's own Tab order follows the layout of the screen, so every focusable control is reached, and
     * the order reads left to right, top to bottom. It stays that way while no screen takes a control out
     * of it or sets an order of its own; the one exception is the calendar button inside a date box, which
     * the Alt + Down key replaces (see {@link AccessibilityTest}).
     */
    @Test
    void noScreenChangesTheTabOrderOrTakesAControlOutOfIt() throws IOException {
        List<String> problems = new ArrayList<>();
        try (Stream<Path> files = Files.list(SOURCES)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String name = file.getFileName().toString();
                String text = Files.readString(file);
                for (String change : List.of("setFocusTraversalPolicy", "setFocusTraversalKeys",
                        "setFocusCycleRoot", "setFocusable(false)")) {
                    int uses = text.split(java.util.regex.Pattern.quote(change), -1).length - 1;
                    int allowed = name.equals("DatePickerField.java") && change.equals("setFocusable(false)") ? 1 : 0;
                    if (uses > allowed) {
                        problems.add(name + " uses " + change);
                    }
                }
            }
        }
        assertTrue(problems.isEmpty(), "the Tab order is changed: " + problems);
    }

    @Test
    void everyScreenStartsItsKeyboardInAControlThatCanTakeIt() {
        List<String> bad = new ArrayList<>();
        for (Map.Entry<String, JComponent> screen : every.screens.entrySet()) {
            if (!(screen.getValue() instanceof Screen)) {
                continue;
            }
            JComponent first = ((Screen) screen.getValue()).initialFocus();
            if (first != null && !(first.isFocusable() && first.isEnabled())) {
                bad.add(screen.getKey());
            }
        }
        assertTrue(bad.isEmpty(), "the keyboard cannot start in: " + bad);
    }
}
