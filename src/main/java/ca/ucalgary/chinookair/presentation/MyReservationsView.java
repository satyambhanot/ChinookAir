package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.ReservationStatus;
import ca.ucalgary.chinookair.business.entity.Role;
import ca.ucalgary.chinookair.business.entity.User;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The reservations of a customer as cards: the logged-in customer, or the customer an agent chose. They
 * are in three sections (upcoming, past, cancelled) and can be found by typing. A reservation can be
 * cancelled while its flight has not left, and one that waits for payment can be paid, both from its card.
 */
class MyReservationsView extends JPanel implements Screen {

    private static final long serialVersionUID = 1L;
    private static final Color ERROR = Theme.DANGER;

    private final transient Controllers controllers;
    private final transient Navigator navigator;
    private final transient BookingSession booking;
    private transient Predicate<String> confirmer = this::askYesNo;

    final Breadcrumb breadcrumb = new Breadcrumb();
    final SearchField filterField = new SearchField("Find by confirmation number or flight", 28);
    private final JLabel heading = new JLabel("My reservations");
    private final JPanel list = new JPanel();
    private final JLabel messageLabel = new JLabel(" ");
    private final transient List<ReservationCard> cards = new ArrayList<>();
    private final transient List<String> sectionTitles = new ArrayList<>();
    private transient List<Reservation> history = List.of();
    private transient Customer owner;
    private EmptyState emptyState;

    MyReservationsView(Controllers controllers, Navigator navigator, BookingSession booking) {
        super(new BorderLayout(0, Space.M));
        this.controllers = controllers;
        this.navigator = navigator;
        this.booking = booking;
        build();
    }

    private void build() {
        setBorder(BorderFactory.createEmptyBorder(Space.XL, Space.PAGE, Space.XL, Space.PAGE));
        Theme.heading(heading);
        JPanel titles = new JPanel(new BorderLayout(0, Space.S));
        titles.setOpaque(false);
        titles.add(breadcrumb, BorderLayout.NORTH);
        titles.add(heading, BorderLayout.CENTER);
        JPanel top = new JPanel(new BorderLayout(0, Space.M));
        top.setOpaque(false);
        top.add(titles, BorderLayout.NORTH);
        top.add(filterField, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        add(ScrollableForm.around(list), BorderLayout.CENTER);
        add(messageLabel, BorderLayout.SOUTH);
        filterField.onChange(text -> render());
    }

    // ---- showing

    private void load() {
        User user = controllers.session().currentUser().orElseThrow();
        history = controllers.reservationController().history(user, owner.id());
        render();
    }

    /** Builds the sections again from what was loaded and what is typed in the search box. */
    private void render() {
        list.removeAll();
        cards.clear();
        sectionTitles.clear();
        LocalDate today = controllers.flightSearchController().today();
        List<Reservation> shown = history.stream()
                .filter(r -> ReservationGroups.matches(r, filterField.getText())).toList();
        if (shown.isEmpty()) {
            showEmpty(history.isEmpty());
        } else {
            for (Map.Entry<ReservationGroups.Group, List<Reservation>> section
                    : ReservationGroups.split(shown, today).entrySet()) {
                list.add(sectionTitle(section.getKey().title() + " (" + section.getValue().size() + ")"));
                for (Reservation reservation : section.getValue()) {
                    ReservationCard card = new ReservationCard(reservation,
                            controllers.reservationController().canCancel(reservation),
                            () -> pay(reservation), () -> cancel(reservation));
                    card.setAlignmentX(LEFT_ALIGNMENT);
                    card.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
                    cards.add(card);
                    list.add(card);
                    list.add(Box.createVerticalStrut(Space.S));
                }
            }
        }
        list.revalidate();
        list.repaint();
    }

    private void showEmpty(boolean nothingAtAll) {
        boolean customer = controllers.session().currentUser().map(u -> u.role() == Role.CUSTOMER).orElse(false);
        if (nothingAtAll) {
            emptyState = new EmptyState("No reservations yet", "Find a flight and your booking shows up here.");
            if (customer) {
                emptyState.setAction("Find flights", () -> navigator.show(ViewId.FLIGHT_SEARCH));
            }
        } else {
            emptyState = new EmptyState("No reservation matches", "Check the confirmation number or the flight.");
        }
        list.add(emptyState);
    }

    private JLabel sectionTitle(String text) {
        sectionTitles.add(text);
        JLabel title = new JLabel(text);
        title.setFont(title.getFont().deriveFont(Font.BOLD, Type.SECTION));
        title.setForeground(Theme.TEXT);
        title.setBorder(BorderFactory.createEmptyBorder(Space.S, 0, Space.S, 0));
        title.setAlignmentX(LEFT_ALIGNMENT);
        return title;
    }

    /** A customer is on a tab; an agent came from a customer's row and gets the way back. */
    private void showPath(User user) {
        Optional<Customer> chosen = booking.customer();
        if (user.role() == Role.AGENT && chosen.isPresent()) {
            heading.setText("Reservations of " + chosen.get().name());
            breadcrumb.show(List.of(
                    Breadcrumb.Crumb.link("Customers", () -> navigator.show(ViewId.CUSTOMER_MANAGEMENT)),
                    Breadcrumb.Crumb.text(chosen.get().name()), Breadcrumb.Crumb.text("Reservations")));
        } else {
            heading.setText("My reservations");
            breadcrumb.show(List.of());
        }
    }

    // ---- actions

    private void cancel(Reservation reservation) {
        User user = controllers.session().currentUser().orElse(null);
        if (user == null) {
            return;
        }
        String question = "Cancel reservation " + reservation.confirmationNumber() + "?";
        if (reservation.status() == ReservationStatus.CONFIRMED) {
            question += " You will be refunded $" + reservation.price().toPlainString() + ".";
        }
        if (!confirmer.test(question)) {
            return;
        }
        try {
            controllers.reservationController().cancel(user, reservation.id());
            messageLabel.setText(" ");
            load();
        } catch (RuntimeException e) {
            showError(ErrorMessages.forException(e));
        }
    }

    private void pay(Reservation reservation) {
        // a reservation paid from this list has no flight search behind it
        Optional<Customer> chosen = booking.customer();
        booking.clear();
        chosen.ifPresent(booking::bookFor);
        booking.setReservation(reservation);
        navigator.show(ViewId.PAYMENT);
    }

    private boolean askYesNo(String question) {
        return ConfirmDialog.ask(this, "Cancel this reservation?", question, "Cancel reservation", true);
    }

    /** The customer whose reservations are shown: the one an agent chose, or else the person logged in. */
    private Customer ownerOf(User user) {
        Optional<Customer> chosen = booking.customer();
        if (chosen.isPresent()) {
            return chosen.get();
        }
        if (user instanceof Customer) {
            return (Customer) user;
        }
        throw new IllegalStateException("Choose a customer first");
    }

    private void showError(String text) {
        messageLabel.setForeground(ERROR);
        messageLabel.setText(text);
    }

    @Override
    public JComponent initialFocus() {
        return filterField;
    }

    @Override
    public void onShow() {
        Optional<User> user = controllers.session().currentUser();
        if (user.isEmpty()) {
            navigator.show(ViewId.LOGIN);
            return;
        }
        showPath(user.get());
        filterField.setText("");
        history = List.of();
        render();
        try {
            owner = ownerOf(user.get());
            load();
        } catch (RuntimeException e) {
            showError(ErrorMessages.forException(e));
        }
    }

    // ---- for the tests

    String message() {
        return messageLabel.getText().trim();
    }

    /** Replaces the Yes/No question, so a test can answer it without a dialog. */
    void setConfirmer(Predicate<String> newConfirmer) {
        this.confirmer = newConfirmer;
    }

    /** The cards in the order they are shown. */
    List<ReservationCard> cards() {
        return List.copyOf(cards);
    }

    /** The card of a reservation, found by its confirmation number. */
    ReservationCard cardFor(String confirmationNumber) {
        return cards.stream().filter(c -> c.reservation().confirmationNumber().equals(confirmationNumber))
                .findFirst().orElseThrow();
    }

    /** The headings of the sections, like "Upcoming (2)". */
    List<String> sectionTitles() {
        return List.copyOf(sectionTitles);
    }

    /** What the list says when there is nothing to show, or null when it shows cards. */
    EmptyState emptyState() {
        return cards.isEmpty() ? emptyState : null;
    }
}
