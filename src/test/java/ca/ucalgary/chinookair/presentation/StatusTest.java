package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.PaymentStatus;
import ca.ucalgary.chinookair.business.entity.ReservationStatus;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;

class StatusTest {

    @Test
    void everyReservationStatusHasPlainWordsAndTheyDiffer() {
        Set<String> words = new HashSet<>();
        for (ReservationStatus status : ReservationStatus.values()) {
            String text = Status.of(status).text();
            assertFalse(text.isBlank(), status.name());
            assertFalse(text.contains("_"), "no enum name on screen: " + text);
            words.add(text);
        }
        assertEquals(ReservationStatus.values().length, words.size());
    }

    @Test
    void everyPaymentStatusHasPlainWordsAndTheyDiffer() {
        Set<String> words = new HashSet<>();
        for (PaymentStatus status : PaymentStatus.values()) {
            words.add(Status.of(status).text());
        }
        assertEquals(PaymentStatus.values().length, words.size());
    }

    @Test
    void theMeaningOfEachStatusDecidesItsKind() {
        assertEquals(Status.Kind.WARNING, Status.of(ReservationStatus.PENDING_PAYMENT).kind());
        assertEquals(Status.Kind.SUCCESS, Status.of(ReservationStatus.CONFIRMED).kind());
        assertEquals(Status.Kind.NEUTRAL, Status.of(ReservationStatus.CANCELLED).kind());
        assertEquals("Awaiting payment", Status.of(ReservationStatus.PENDING_PAYMENT).text());
        assertEquals(Status.Kind.SUCCESS, Status.of(PaymentStatus.APPROVED).kind());
        assertEquals("Paid", Status.of(PaymentStatus.APPROVED).text());
        assertEquals(Status.Kind.DANGER, Status.of(PaymentStatus.DECLINED).kind());
        assertEquals(Status.Kind.INFO, Status.of(PaymentStatus.REFUNDED).kind());
    }

    @Test
    void aStatusPrintsAsItsWords() {
        assertEquals("Awaiting payment", Status.of(ReservationStatus.PENDING_PAYMENT).toString());
    }

    @Test
    void aBadgeShowsTheWordsInTheColourOfItsKind() {
        Badge badge = new Badge(Status.of(PaymentStatus.DECLINED));
        assertEquals("Declined", badge.getText());
        assertEquals(Palette.DANGER_TEXT, badge.getForeground());
        assertEquals(Status.Kind.DANGER, badge.kind());
        assertTrue(badge.getIcon().getIconWidth() > 0);
    }

    @Test
    void theCellRendererPutsABadgeOnTheRowBackground() {
        JTable table = new JTable(new DefaultTableModel(1, 1));
        Badge.CellRenderer renderer = new Badge.CellRenderer();
        Component cell = renderer.getTableCellRendererComponent(table, Status.of(ReservationStatus.CONFIRMED),
                false, false, 0, 0);
        assertEquals(table.getBackground(), cell.getBackground());
        assertEquals("Confirmed", ((Badge) ((Container) cell).getComponent(0)).getText());

        Component selected = renderer.getTableCellRendererComponent(table, Status.of(ReservationStatus.CONFIRMED),
                true, true, 0, 0);
        assertEquals(table.getSelectionBackground(), selected.getBackground());
        assertSame(cell, selected);
    }

    @Test
    void aCellThatIsNotAStatusStillShowsItsText() {
        JTable table = new JTable(new DefaultTableModel(1, 1));
        Component cell = new Badge.CellRenderer().getTableCellRendererComponent(table, "n/a", false, false, 0, 0);
        assertEquals("n/a", ((JLabel) ((Container) cell).getComponent(0)).getText());
    }

    @Test
    void everyKindKeepsItsTextReadableOnItsFill() {
        for (Status.Kind kind : Status.Kind.values()) {
            Color text = kind.text();
            Color fill = kind.fill();
            assertTrue(PaletteTest.contrast(text, fill) >= 4.5, kind.name());
        }
    }
}
