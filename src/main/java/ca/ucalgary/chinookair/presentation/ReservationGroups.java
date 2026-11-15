package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.ReservationStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/** Sorts a customer's reservations into what is ahead, what is done and what was cancelled. */
final class ReservationGroups {

    /** The three sections of the list, in the order they are shown. */
    enum Group {
        UPCOMING("Upcoming"), PAST("Past"), CANCELLED("Cancelled");

        private final String title;

        Group(String title) {
            this.title = title;
        }

        String title() {
            return title;
        }
    }

    private ReservationGroups() {
    }

    /** The section a reservation belongs to, judged from the start of {@code today}. */
    static Group of(Reservation reservation, LocalDate today) {
        if (reservation.status() == ReservationStatus.CANCELLED) {
            return Group.CANCELLED;
        }
        boolean left = reservation.status() == ReservationStatus.COMPLETED
                || reservation.flight().departure().toLocalDate().isBefore(today);
        return left ? Group.PAST : Group.UPCOMING;
    }

    /**
     * Every section with its reservations: what is ahead with the soonest flight first, the others
     * with the latest flight first. A section with nothing in it is left out.
     */
    static Map<Group, List<Reservation>> split(List<Reservation> all, LocalDate today) {
        Map<Group, List<Reservation>> groups = new EnumMap<>(Group.class);
        for (Reservation reservation : all) {
            groups.computeIfAbsent(of(reservation, today), g -> new ArrayList<>()).add(reservation);
        }
        Comparator<Reservation> byFlight = Comparator.comparing(r -> r.flight().departure());
        groups.forEach((group, list) -> list.sort(group == Group.UPCOMING ? byFlight : byFlight.reversed()));
        return groups;
    }

    /** Whether the words typed are part of the confirmation number, flight number or airports. */
    static boolean matches(Reservation reservation, String typed) {
        String words = typed == null ? "" : typed.trim().toLowerCase(Locale.ROOT);
        if (words.isEmpty()) {
            return true;
        }
        String haystack = (reservation.confirmationNumber() + " " + reservation.flight().flightNumber() + " "
                + reservation.flight().route().origin().code() + " "
                + reservation.flight().route().destination().code()).toLowerCase(Locale.ROOT);
        return haystack.contains(words);
    }
}
