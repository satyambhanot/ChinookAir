package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.ucalgary.chinookair.business.entity.Payment;
import ca.ucalgary.chinookair.business.entity.PaymentMethodType;
import ca.ucalgary.chinookair.business.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentTableModelTest {

    private static Payment payment(String amount, PaymentStatus status) {
        return Payment.restore(1L, 12L, new BigDecimal(amount), PaymentMethodType.CREDIT, status,
                "**** **** **** 1111", LocalDateTime.of(2026, 11, 20, 9, 5), "Approved");
    }

    @Test
    void anApprovedPaymentShowsAllSixValues() {
        PaymentTableModel model = new PaymentTableModel();
        model.setPayments(List.of(payment("300.00", PaymentStatus.APPROVED)));
        assertEquals(1, model.getRowCount());
        assertEquals(6, model.getColumnCount());
        assertEquals("2026-11-20 09:05", model.getValueAt(0, 0));
        assertEquals("#12", model.getValueAt(0, 1));
        assertEquals("$300.00", model.getValueAt(0, 2));
        assertEquals("CREDIT", model.getValueAt(0, 3));
        assertEquals("APPROVED", model.getValueAt(0, 4));
        assertEquals("**** **** **** 1111", model.getValueAt(0, 5));
        assertEquals("Amount", model.getColumnName(2));
    }

    @Test
    void rowsKeepTheOrderTheyWereGiven() {
        PaymentTableModel model = new PaymentTableModel();
        model.setPayments(List.of(payment("300.00", PaymentStatus.APPROVED), payment("50.00", PaymentStatus.DECLINED)));
        assertEquals("APPROVED", model.getValueAt(0, 4));
        assertEquals("DECLINED", model.getValueAt(1, 4));
    }

    @Test
    void anUnknownColumnIsRejected() {
        PaymentTableModel model = new PaymentTableModel();
        model.setPayments(List.of(payment("300.00", PaymentStatus.APPROVED)));
        assertThrows(IndexOutOfBoundsException.class, () -> model.getValueAt(0, 6));
    }
}
