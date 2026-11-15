package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.entity.Flight;
import javax.swing.table.AbstractTableModel;

/**
 * The schedule as a day-by-day list: a first column with the day written once, on the first flight
 * of that day, and the times of the flight as a time of day only. It reads the flights from a
 * {@link ScheduleTableModel}, so the rows are the same rows.
 */
final class DayColumns extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"Day", "Flight", "Route", "Departs", "Arrives", "Aircraft"};

    private final transient ScheduleTableModel base;

    DayColumns(ScheduleTableModel base) {
        this.base = base;
        base.addTableModelListener(e -> fireTableDataChanged());
    }

    @Override
    public int getRowCount() {
        return base.getRowCount();
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
        Flight flight = base.flightAt(row);
        switch (column) {
            case 0:
                boolean first = row == 0 || !base.flightAt(row - 1).departure().toLocalDate()
                        .equals(flight.departure().toLocalDate());
                return first ? Format.date(flight.departure().toLocalDate()) : "";
            case 1:
                return base.getValueAt(row, 0);
            case 2:
                return base.getValueAt(row, 1);
            case 3:
                return Format.time(flight.departure());
            case 4:
                boolean nextDay = flight.arrival().toLocalDate().isAfter(flight.departure().toLocalDate());
                return Format.time(flight.arrival()) + (nextDay ? " +1" : "");
            case 5:
                return base.getValueAt(row, 4);
            default:
                throw new IndexOutOfBoundsException("No column " + column);
        }
    }
}
