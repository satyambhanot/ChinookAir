package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.entity.Flight;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A trip set out like a boarding pass: a navy band with where the flight leaves from and lands, when,
 * and how long it takes, and under it who flies, on which flight, in which seat and what was paid.
 */
final class ItineraryCard extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient List<SummaryCard.Row> rows = new ArrayList<>();

    ItineraryCard(Flight flight, String passenger, String seat, String amountLabel, String amount) {
        super(new BorderLayout());
        SurfaceBorder.apply(this, 0, 0, 0, 0);
        rows.add(new SummaryCard.Row("Passenger", passenger, false));
        rows.add(new SummaryCard.Row("Flight", flight.flightNumber() + " (" + flight.airline().name() + ")", false));
        rows.add(new SummaryCard.Row("Route", flight.route().origin().code() + " → "
                + flight.route().destination().code(), false));
        rows.add(new SummaryCard.Row("Departs", Format.dateTimeWithYear(flight.departure()), false));
        rows.add(new SummaryCard.Row("Seat", seat, false));
        rows.add(new SummaryCard.Row(amountLabel, amount, true));
        add(band(flight), BorderLayout.NORTH);
        add(details(flight, passenger, seat, amountLabel, amount), BorderLayout.CENTER);
        getAccessibleContext().setAccessibleName("Your trip");
    }

    private static JPanel band(Flight flight) {
        JPanel band = new Band();
        band.setLayout(new GridBagLayout());
        band.setBorder(javax.swing.BorderFactory.createEmptyBorder(Space.L, Space.XL, Space.L, Space.XL));
        GridBagConstraints at = new GridBagConstraints();
        at.anchor = GridBagConstraints.WEST;
        at.gridy = 0;
        at.gridx = 0;
        at.gridwidth = 3;
        at.insets = new Insets(0, 0, Space.S, 0);
        band.add(light(Format.date(flight.departure().toLocalDate()) + ", "
                + flight.departure().getYear(), Type.LABEL, false), at);
        at.gridwidth = 1;
        at.insets = new Insets(0, 0, 0, 0);
        at.gridy = 1;
        at.gridx = 0;
        band.add(light(flight.route().origin().code(), Type.DISPLAY, true), at);
        at.gridx = 1;
        at.weightx = 1;
        at.anchor = GridBagConstraints.CENTER;
        at.insets = new Insets(0, Space.XL, 0, Space.XL);
        FlightLine line = new FlightLine(true);
        line.setText(Format.duration(flight.departure(), flight.arrival()));
        band.add(line, at);
        at.weightx = 0;
        at.gridx = 2;
        at.anchor = GridBagConstraints.EAST;
        at.insets = new Insets(0, 0, 0, 0);
        band.add(light(flight.route().destination().code(), Type.DISPLAY, true), at);
        at.gridy = 2;
        at.gridx = 0;
        at.anchor = GridBagConstraints.WEST;
        band.add(light(Format.time(flight.departure()) + " · " + flight.route().origin().city(),
                Type.LABEL, false), at);
        at.gridx = 2;
        at.anchor = GridBagConstraints.EAST;
        boolean nextDay = flight.arrival().toLocalDate().isAfter(flight.departure().toLocalDate());
        band.add(light(Format.time(flight.arrival()) + (nextDay ? " +1" : "") + " · "
                + flight.route().destination().city(), Type.LABEL, false), at);
        return band;
    }

    private static JLabel light(String text, float size, boolean bold) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(bold ? Color.WHITE : Palette.SKY_100);
        return label;
    }

    private static JPanel details(Flight flight, String passenger, String seat, String amountLabel, String amount) {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        grid.setBorder(javax.swing.BorderFactory.createEmptyBorder(Space.L, Space.XL, Space.XL, Space.XL));
        String[][] cells = {{"Passenger", passenger},
            {"Flight", flight.flightNumber() + " · " + flight.airline().name()},
            {"Seat", seat}, {amountLabel, amount}};
        GridBagConstraints at = new GridBagConstraints();
        at.anchor = GridBagConstraints.NORTHWEST;
        at.weightx = 1;
        at.fill = GridBagConstraints.HORIZONTAL;
        for (int i = 0; i < cells.length; i++) {
            at.gridx = i;
            at.gridy = 0;
            at.insets = new Insets(0, 0, Space.XS, Space.L);
            grid.add(Theme.hint(cells[i][0]), at);
            JLabel value = new JLabel(cells[i][1]);
            value.setFont(value.getFont().deriveFont(Font.BOLD, i == cells.length - 1 ? Type.SECTION : Type.BODY));
            value.setForeground(Theme.TEXT);
            at.gridy = 1;
            grid.add(value, at);
        }
        return grid;
    }

    /** The lines of the card as label and value, for tests. */
    List<SummaryCard.Row> rows() {
        return List.copyOf(rows);
    }

    /** The navy band, with its top corners rounded like the card's. */
    private static final class Band extends JPanel {

        private static final long serialVersionUID = 1L;

        Band() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setClip(0, 0, getWidth(), getHeight());
            g2.setColor(Palette.NAVY_900);
            int arc = Theme.ARC;
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight() + arc, arc, arc));
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
