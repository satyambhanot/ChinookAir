package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.entity.Airline;
import ca.ucalgary.chinookair.business.entity.Airport;
import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

/** Shows "YYC - Calgary" for an airport, "AC - Air Canada" for an airline, "Any" for none. */
final class CodeRenderer extends DefaultListCellRenderer {

    private static final long serialVersionUID = 1L;

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                                                  boolean focus) {
        String text;
        if (value instanceof Airport) {
            Airport airport = (Airport) value;
            text = airport.code() + " - " + airport.city();
        } else if (value instanceof Airline) {
            Airline airline = (Airline) value;
            text = airline.code() + " - " + airline.name();
        } else {
            text = "Any";
        }
        return super.getListCellRendererComponent(list, text, index, selected, focus);
    }
}
