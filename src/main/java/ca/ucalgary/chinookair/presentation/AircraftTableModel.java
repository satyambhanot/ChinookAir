package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.entity.Aircraft;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/** The rows of the aircraft list: one aircraft per row, with the number of seats worked out. */
class AircraftTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"Model", "Rows", "Seats per row", "Seats"};

    private final transient List<Aircraft> aircraft = new ArrayList<>();

    /** Replaces all rows. */
    void setAircraft(List<Aircraft> newAircraft) {
        aircraft.clear();
        aircraft.addAll(newAircraft);
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return aircraft.size();
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
    public Class<?> getColumnClass(int column) {
        return column == 0 ? String.class : Integer.class;
    }

    @Override
    public Object getValueAt(int row, int column) {
        Aircraft plane = aircraft.get(row);
        switch (column) {
            case 0:
                return plane.model();
            case 1:
                return plane.rows();
            case 2:
                return plane.seatsPerRow();
            case 3:
                return plane.rows() * plane.seatsPerRow();
            default:
                throw new IndexOutOfBoundsException("No column " + column);
        }
    }
}
