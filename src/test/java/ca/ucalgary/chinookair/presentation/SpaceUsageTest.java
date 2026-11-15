package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Padding and insets come from {@link Space}, so two screens that mean the same gap use the same one. */
class SpaceUsageTest {

    private static final Path SOURCES = Path.of("src/main/java/ca/ucalgary/chinookair/presentation");
    private static final Pattern CALL =
            Pattern.compile("(createEmptyBorder|new Insets)\\(([^()]*(?:\\([^()]*\\)[^()]*)*)\\)");

    /** A number a screen may write: nothing, or a hairline of up to three pixels. */
    private static boolean allowed(String argument) {
        return argument.matches("[0-3]") || argument.contains("Space.");
    }

    private static List<String> split(String arguments) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (char c : arguments.toCharArray()) {
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            }
            if (c == ',' && depth == 0) {
                parts.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString().trim());
        return parts;
    }

    static List<String> problems(String source, String file) {
        List<String> problems = new ArrayList<>();
        Matcher call = CALL.matcher(source);
        while (call.find()) {
            if (call.group(2).isBlank()) {
                continue;
            }
            for (String argument : split(call.group(2))) {
                if (!allowed(argument)) {
                    problems.add(file + ": " + call.group(0));
                    break;
                }
            }
        }
        return problems;
    }

    @Test
    void noScreenWritesItsOwnPaddingNumbers() throws IOException {
        List<String> problems = new ArrayList<>();
        try (Stream<Path> files = Files.list(SOURCES)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String name = file.getFileName().toString();
                if (name.equals("Space.java") || name.equals("SurfaceBorder.java")) {
                    continue;
                }
                problems.addAll(problems(Files.readString(file), name));
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void theCheckItselfCatchesANumberThatIsNotOnTheScale() {
        assertEquals(1, problems("x.setBorder(BorderFactory.createEmptyBorder(20, 22, 16, 22));", "A").size());
        assertEquals(1, problems("c.insets = new Insets(0, 0, 0, 13);", "A").size());
        assertEquals(0, problems("x.setBorder(BorderFactory.createEmptyBorder(0, Space.L, 3, Space.XL));", "A").size());
        assertEquals(0, problems("c.insets = new Insets(row.total() ? Space.M : 3, 0, 3, Space.L);", "A").size());
    }
}
