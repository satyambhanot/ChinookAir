package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.FlightOffer;
import ca.ucalgary.chinookair.business.entity.Flight;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

/**
 * The flights found, one card each: when it leaves and lands, how long it takes, which airline and
 * plane, how many seats are left and the lowest price. Arrow keys move between the cards and a
 * click chooses one. The cards can be put in order by time, price or length.
 */
class FlightResultList extends JList<FlightOffer> {

    private static final long serialVersionUID = 1L;
    static final int CELL_HEIGHT = 112;
    private static final int FEW_SEATS = 10;

    /** How the cards are put in order. */
    enum Sort {
        EARLIEST("Earliest"), CHEAPEST("Cheapest"), SHORTEST("Shortest");

        private final String label;

        Sort(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }

        Comparator<FlightOffer> order() {
            Comparator<FlightOffer> byTime = Comparator.comparing(o -> o.flight().departure());
            switch (this) {
                case CHEAPEST:
                    return Comparator.comparing(FlightOffer::fromPrice).thenComparing(byTime);
                case SHORTEST:
                    return Comparator.comparing((FlightOffer o) -> minutes(o.flight())).thenComparing(byTime);
                default:
                    return byTime.thenComparing(FlightOffer::fromPrice);
            }
        }
    }

    private final DefaultListModel<FlightOffer> listModel = new DefaultListModel<>();
    private transient List<FlightOffer> found = List.of();
    private Sort sort = Sort.EARLIEST;
    private String emptyText;

    FlightResultList(String emptyText) {
        super();
        setModel(listModel);
        this.emptyText = emptyText;
        setOpaque(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setFixedCellHeight(CELL_HEIGHT);
        setCellRenderer(new Card());
        getAccessibleContext().setAccessibleName("Flights found");
    }

    static long minutes(Flight flight) {
        return Duration.between(flight.departure(), flight.arrival()).toMinutes();
    }

    /** Shows these flights in the chosen order, with nothing chosen. */
    void setOffers(List<FlightOffer> offers) {
        this.found = List.copyOf(offers);
        show(null);
    }

    /** Puts the cards in this order and keeps the chosen flight chosen. */
    void sortBy(Sort newSort) {
        this.sort = newSort;
        show(selectedOffer());
    }

    Sort sort() {
        return sort;
    }

    private void show(FlightOffer keep) {
        List<FlightOffer> ordered = new ArrayList<>(found);
        ordered.sort(sort.order());
        listModel.clear();
        ordered.forEach(listModel::addElement);
        if (keep != null) {
            for (int i = 0; i < ordered.size(); i++) {
                if (ordered.get(i).flight().id().equals(keep.flight().id())) {
                    setSelectedIndex(i);
                }
            }
        }
        revalidate();
        repaint();
    }

    /** The flight that is chosen, or null. */
    FlightOffer selectedOffer() {
        return getSelectedValue();
    }

    /** The flights in the order they are shown. */
    List<FlightOffer> offers() {
        List<FlightOffer> shown = new ArrayList<>();
        for (int i = 0; i < listModel.size(); i++) {
            shown.add(listModel.get(i));
        }
        return shown;
    }

    void setEmptyText(String text) {
        this.emptyText = text;
        repaint();
    }

    String emptyText() {
        return emptyText;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (listModel.isEmpty() && emptyText != null && !emptyText.isEmpty()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Theme.MUTED);
            g2.setFont(getFont().deriveFont(Type.BODY));
            g2.drawString(emptyText, Space.S, Space.XL);
            g2.dispose();
        }
    }

    /** One flight as a card. The same panel is filled in again for every row. */
    private static final class Card extends JPanel implements ListCellRenderer<FlightOffer> {

        private static final long serialVersionUID = 1L;

        private final JLabel leaves = bold(Type.SECTION);
        private final JLabel lands = bold(Type.SECTION);
        private final JLabel from = Theme.hint("");
        private final JLabel to = Theme.hint("");
        private final JLabel what = Theme.hint("");
        private final FlightLine line = new FlightLine();
        private final JLabel price = bold(Type.PAGE_HEADING);
        private final JLabel seats = Theme.hint("");

        Card() {
            super(new BorderLayout(Space.XL, 0));
            setOpaque(false);
            JPanel route = new JPanel(new GridBagLayout());
            route.setOpaque(false);
            GridBagConstraints at = new GridBagConstraints();
            at.anchor = GridBagConstraints.WEST;
            at.gridy = 0;
            at.gridx = 0;
            route.add(leaves, at);
            at.gridx = 1;
            at.insets = new Insets(0, Space.M, 0, Space.M);
            route.add(line, at);
            at.gridx = 2;
            at.insets = new Insets(0, 0, 0, 0);
            route.add(lands, at);
            at.gridy = 1;
            at.gridx = 0;
            route.add(from, at);
            at.gridx = 2;
            route.add(to, at);
            at.gridx = 0;
            at.gridy = 2;
            at.gridwidth = 3;
            at.insets = new Insets(Space.S, 0, 0, 0);
            route.add(what, at);
            JPanel left = new JPanel(new BorderLayout());
            left.setOpaque(false);
            left.add(route, BorderLayout.WEST);

            JPanel right = new JPanel(new GridBagLayout());
            right.setOpaque(false);
            GridBagConstraints r = new GridBagConstraints();
            r.gridx = 0;
            r.anchor = GridBagConstraints.EAST;
            r.gridy = 0;
            right.add(Theme.hint("from"), r);
            r.gridy = 1;
            right.add(price, r);
            r.gridy = 2;
            right.add(seats, r);
            add(left, BorderLayout.CENTER);
            add(right, BorderLayout.EAST);
        }

        private static JLabel bold(float size) {
            JLabel label = new JLabel(" ");
            label.setFont(label.getFont().deriveFont(Font.BOLD, size));
            label.setForeground(Theme.TEXT);
            return label;
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends FlightOffer> list, FlightOffer offer,
                int index, boolean selected, boolean focus) {
            Flight flight = offer.flight();
            leaves.setText(Format.time(flight.departure()));
            boolean nextDay = flight.arrival().toLocalDate().isAfter(flight.departure().toLocalDate());
            lands.setText(Format.time(flight.arrival()) + (nextDay ? " +1" : ""));
            from.setText(flight.route().origin().code() + " · " + flight.route().origin().city());
            to.setText(flight.route().destination().code() + " · " + flight.route().destination().city());
            line.setText(Format.duration(flight.departure(), flight.arrival()));
            what.setText(flight.airline().name() + " · " + flight.flightNumber() + " · "
                    + flight.aircraft().model());
            price.setText(Format.money(offer.fromPrice()));
            seats.setText(offer.seatsLeft() <= FEW_SEATS ? "Only " + offer.seatsLeft() + " seats left"
                    : offer.seatsLeft() + " seats left");
            seats.setForeground(offer.seatsLeft() <= FEW_SEATS ? Theme.WARNING : Theme.MUTED);
            SurfaceBorder.apply(this, selected, Space.L, Space.XL, Space.L, Space.XL);
            getAccessibleContext().setAccessibleName(accessibleText(offer));
            return this;
        }

        private static String accessibleText(FlightOffer offer) {
            Flight flight = offer.flight();
            return flight.flightNumber() + ", " + flight.route().origin().code() + " to "
                    + flight.route().destination().code() + ", leaves " + Format.time(flight.departure())
                    + ", from " + Format.money(offer.fromPrice()) + ", " + offer.seatsLeft() + " seats left";
        }

        /** The cell is only as wide as the list and always {@link #CELL_HEIGHT} tall. */
        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            return new Dimension(size.width, CELL_HEIGHT);
        }

        @Override
        public Color getBackground() {
            return Palette.PAGE;
        }
    }
}
