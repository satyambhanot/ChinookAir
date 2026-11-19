package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Reservation;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/** The rows of the reservation list: one reservation per row, read only. */
class ReservationTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"Confirmation", "Flight", "Route", "Departs", "Seat", "Status", "Price"};

    private final transient List<Reservation> reservations = new ArrayList<>();

    /** Replaces all rows. */
    void setReservations(List<Reservation> newReservations) {
        reservations.clear();
        reservations.addAll(newReservations);
        fireTableDataChanged();
    }

    Reservation reservationAt(int row) {
        return reservations.get(row);
    }

    @Override
    public int getRowCount() {
        return reservations.size();
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
        Reservation reservation = reservations.get(row);
        Flight flight = reservation.flight();
        switch (column) {
            case 0:
                return reservation.confirmationNumber();
            case 1:
                return flight.flightNumber();
            case 2:
                return flight.route().origin().code() + " → " + flight.route().destination().code();
            case 3:
                return flight.departure();
            case 4:
                // no seat yet while the reservation is only created
                return reservation.seatNo() == null ? "" : reservation.seatNo();
            case 5:
                return Status.of(reservation.status());
            case 6:
                return reservation.price() == null ? "" : Format.money(reservation.price());
            default:
                throw new IndexOutOfBoundsException("No column " + column);
        }
    }

    /** The status column holds a {@link Status}, which the table draws as a badge. */
    @Override
    public Class<?> getColumnClass(int column) {
        switch (column) {
            case 3:
                return LocalDateTime.class;
            case 5:
                return Status.class;
            default:
                return Object.class;
        }
    }
}
