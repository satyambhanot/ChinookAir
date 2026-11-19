package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.entity.Role;
import java.util.List;
import java.util.Optional;

/**
 * The steps of the two tasks that take several screens: a customer booking a flight, and an agent
 * changing a reservation. It says which step a screen is, or that the screen is not part of a task.
 */
final class StepperModel {

    /** The names of the steps and the one that is current (counting from 0). */
    record Flow(List<String> steps, int current) {
    }

    private static final List<String> BOOKING = List.of("Find a flight", "Choose a seat", "Pay", "Done");
    private static final List<String> CHANGE = List.of("Reservation", "New flight", "New seat");

    private StepperModel() {
    }

    static Optional<Flow> flowFor(Role role, ViewId view) {
        if (role == Role.CUSTOMER) {
            switch (view) {
                case FLIGHT_SEARCH:
                    return Optional.of(new Flow(BOOKING, 0));
                case SEAT_SELECTION:
                    return Optional.of(new Flow(BOOKING, 1));
                case PAYMENT:
                    return Optional.of(new Flow(BOOKING, 2));
                case CONFIRMATION:
                    return Optional.of(new Flow(BOOKING, 3));
                default:
                    return Optional.empty();
            }
        }
        if (role == Role.AGENT) {
            switch (view) {
                case MODIFY_RESERVATION:
                    return Optional.of(new Flow(CHANGE, 0));
                case FLIGHT_SEARCH:
                    return Optional.of(new Flow(CHANGE, 1));
                case SEAT_SELECTION:
                    return Optional.of(new Flow(CHANGE, 2));
                default:
                    return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
