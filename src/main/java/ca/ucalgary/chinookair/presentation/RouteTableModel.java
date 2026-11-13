package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.entity.Route;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/** The rows of the route list: one route per row, read only. */
class RouteTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"Origin", "Destination"};

    private final transient List<Route> routes = new ArrayList<>();

    /** Replaces all rows. */
    void setRoutes(List<Route> newRoutes) {
        routes.clear();
        routes.addAll(newRoutes);
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return routes.size();
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
        Route route = routes.get(row);
        switch (column) {
            case 0:
                return route.origin().code() + " - " + route.origin().city();
            case 1:
                return route.destination().code() + " - " + route.destination().city();
            default:
                throw new IndexOutOfBoundsException("No column " + column);
        }
    }
}
