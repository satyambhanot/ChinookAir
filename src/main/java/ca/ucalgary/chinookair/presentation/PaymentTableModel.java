package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.entity.Payment;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/** The rows of the payment records: one payment per row, read only. */
class PaymentTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String[] COLUMNS = {"Date", "Reservation", "Amount", "Method", "Status", "Card"};

    private final transient List<Payment> payments = new ArrayList<>();

    /** Replaces all rows. */
    void setPayments(List<Payment> newPayments) {
        payments.clear();
        payments.addAll(newPayments);
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return payments.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int row, int column) {
        Payment payment = payments.get(row);
        switch (column) {
            case 0:
                return payment.createdAt().format(WHEN);
            case 1:
                return "#" + payment.reservationId();
            case 2:
                return "$" + payment.amount().toPlainString();
            case 3:
                return payment.type().name();
            case 4:
                return payment.status().name();
            case 5:
                return payment.maskedCard();
            default:
                throw new IndexOutOfBoundsException("No column " + column);
        }
    }
}
