package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.ReservationStatus;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Which of a customer's reservations the Home screen puts forward. */
final class HomeTrips {

    private HomeTrips() {
    }

    /**
     * The trip to look forward to: the confirmed reservation whose flight leaves soonest, counting
     * flights from the start of today on.
     */
    static Optional<Reservation> next(List<Reservation> all, LocalDate today) {
        return all.stream()
                .filter(r -> r.status() == ReservationStatus.CONFIRMED)
                .filter(r -> !r.flight().departure().toLocalDate().isBefore(today))
                .min(Comparator.comparing(r -> r.flight().departure()));
    }

    /** The reservations made most recently, newest first, at most {@code count} of them. */
    static List<Reservation> recent(List<Reservation> all, int count) {
        return all.stream()
                .sorted(Comparator.comparing(Reservation::createdAt).reversed())
                .limit(count)
                .toList();
    }

    /** What the greeting calls the customer: the first name, or the username when there is no name. */
    static String firstName(String name, String username) {
        if (name == null || name.isBlank()) {
            return username;
        }
        return name.trim().split("\\s+")[0];
    }
}
