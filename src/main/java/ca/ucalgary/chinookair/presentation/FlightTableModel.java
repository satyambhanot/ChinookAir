package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.FlightOffer;
import ca.ucalgary.chinookair.business.entity.Flight;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/** The rows of the search results table: one flight offer per row, read only. */
class FlightTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String[] COLUMNS =
            {"Flight", "Airline", "Route", "Departs", "Arrives", "Seats left", "From"};

    private final transient List<FlightOffer> offers = new ArrayList<>();

    /** Replaces all rows. */
    void setOffers(List<FlightOffer> newOffers) {
        offers.clear();
        offers.addAll(newOffers);
        fireTableDataChanged();
    }

    /** The offer shown in a row. */
    FlightOffer offerAt(int row) {
        return offers.get(row);
    }

    @Override
    public int getRowCount() {
        return offers.size();
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
        return column == 5 ? Integer.class : String.class;
    }

    @Override
    public Object getValueAt(int row, int column) {
        FlightOffer offer = offers.get(row);
        Flight flight = offer.flight();
        switch (column) {
            case 0:
                return flight.flightNumber();
            case 1:
                return flight.airline().name();
            case 2:
                return flight.route().origin().code() + " → " + flight.route().destination().code();
            case 3:
                return flight.departure().format(WHEN);
            case 4:
                return flight.arrival().format(WHEN);
            case 5:
                return offer.seatsLeft();
            case 6:
                return "$" + offer.fromPrice().toPlainString();
            default:
                throw new IndexOutOfBoundsException("No column " + column);
        }
    }
}
