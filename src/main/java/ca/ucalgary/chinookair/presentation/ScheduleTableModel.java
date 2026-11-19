package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.entity.Flight;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/** The rows of the flight schedule: one flight per row, read only. */
class ScheduleTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"Flight", "Route", "Departs", "Arrives", "Aircraft"};

    private final transient List<Flight> flights = new ArrayList<>();

    /** Replaces all rows. */
    void setFlights(List<Flight> newFlights) {
        flights.clear();
        flights.addAll(newFlights);
        fireTableDataChanged();
    }

    Flight flightAt(int row) {
        return flights.get(row);
    }

    @Override
    public int getRowCount() {
        return flights.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    /** The two time columns hold real times, which the table writes with {@link Format} and sorts as times. */
    @Override
    public Class<?> getColumnClass(int column) {
        return column == 2 || column == 3 ? LocalDateTime.class : Object.class;
    }

    @Override
    public Object getValueAt(int row, int column) {
        Flight flight = flights.get(row);
        switch (column) {
            case 0:
                return flight.flightNumber();
            case 1:
                return flight.route().origin().code() + " → " + flight.route().destination().code();
            case 2:
                return flight.departure();
            case 3:
                return flight.arrival();
            case 4:
                return flight.aircraft().model();
            default:
                throw new IndexOutOfBoundsException("No column " + column);
        }
    }
}
