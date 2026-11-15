package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.swing.JLabel;
import javax.swing.table.AbstractTableModel;
import org.junit.jupiter.api.Test;

/** Dates and money are written by {@link Format} only, and a time column sorts as time, not as text. */
class FormatUsageTest {

    private static final Path SOURCES = Path.of("src/main/java/ca/ucalgary/chinookair/presentation");

    @Test
    void noScreenBuildsItsOwnDateFormatOrDollarSign() throws IOException {
        List<String> problems = new ArrayList<>();
        try (Stream<Path> files = Files.list(SOURCES)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String name = file.getFileName().toString();
                if (name.equals("Format.java") || name.equals("InputParser.java")) {
                    continue;
                }
                String text = Files.readString(file);
                if (text.contains("DateTimeFormatter.ofPattern")) {
                    problems.add(name + " makes its own date format");
                }
                if (text.contains("\"$\" +")) {
                    problems.add(name + " writes its own dollar sign");
                }
            }
        }
        assertTrue(problems.isEmpty(), problems.toString());
    }

    /** One column of times, in an order that is wrong when the written form is sorted as text. */
    private static final class Times extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final transient List<LocalDateTime> rows = List.of(
                LocalDateTime.of(2026, 12, 2, 7, 0),     // Wednesday
                LocalDateTime.of(2026, 12, 4, 18, 30),   // Friday
                LocalDateTime.of(2026, 12, 7, 9, 15));   // Monday

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return LocalDateTime.class;
        }

        @Override
        public Object getValueAt(int row, int column) {
            return rows.get(row);
        }
    }

    private static String cell(HintTable table, int row) {
        Component c = table.prepareRenderer(table.getCellRenderer(row, 0), row, 0);
        return ((JLabel) c).getText();
    }

    @Test
    void aTimeColumnIsWrittenForPeopleAndSortsByTheTimeItself() {
        HintTable table = new HintTable(new Times(), "none");
        table.setAutoCreateRowSorter(true);
        assertEquals("Wed, Dec 2 · 7:00 AM", cell(table, 0));
        table.getRowSorter().toggleSortOrder(0);
        table.getRowSorter().toggleSortOrder(0);   // descending
        assertEquals("Mon, Dec 7 · 9:15 AM", cell(table, 0));
        assertEquals("Fri, Dec 4 · 6:30 PM", cell(table, 1));
        assertEquals("Wed, Dec 2 · 7:00 AM", cell(table, 2));
    }

    @Test
    void theYearVersionIsAvailableForRecords() {
        HintTable table = new HintTable(new Times(), "none");
        table.getColumnModel().getColumn(0).setCellRenderer(HintTable.timeWithYearRenderer());
        assertEquals("Wed, Dec 2, 2026 · 7:00 AM", cell(table, 0));
    }
}
