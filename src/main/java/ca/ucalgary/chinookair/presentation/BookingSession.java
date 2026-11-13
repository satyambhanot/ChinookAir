package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.FlightOffer;
import java.util.Optional;

/**
 * What the customer has picked so far while booking. The search screen puts the chosen flight here
 * and the seat screen takes it from here, so the screens do not need to know each other.
 */
final class BookingSession {

    private FlightOffer offer;

    void select(FlightOffer chosen) {
        if (chosen == null) {
            throw new IllegalArgumentException("A flight is required");
        }
        offer = chosen;
    }

    Optional<FlightOffer> selectedOffer() {
        return Optional.ofNullable(offer);
    }

    void clear() {
        offer = null;
    }
}
