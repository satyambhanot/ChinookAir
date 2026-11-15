package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Role;
import java.util.List;
import org.junit.jupiter.api.Test;

class StepperModelTest {

    @Test
    void aBookingHasFourStepsInOrder() {
        StepperModel.Flow search = StepperModel.flowFor(Role.CUSTOMER, ViewId.FLIGHT_SEARCH).orElseThrow();
        assertEquals(List.of("Find a flight", "Choose a seat", "Pay", "Done"), search.steps());
        assertEquals(0, search.current());
        assertEquals(1, StepperModel.flowFor(Role.CUSTOMER, ViewId.SEAT_SELECTION).orElseThrow().current());
        assertEquals(2, StepperModel.flowFor(Role.CUSTOMER, ViewId.PAYMENT).orElseThrow().current());
        assertEquals(3, StepperModel.flowFor(Role.CUSTOMER, ViewId.CONFIRMATION).orElseThrow().current());
    }

    @Test
    void anAgentChangingAReservationHasThreeSteps() {
        StepperModel.Flow first = StepperModel.flowFor(Role.AGENT, ViewId.MODIFY_RESERVATION).orElseThrow();
        assertEquals(List.of("Reservation", "New flight", "New seat"), first.steps());
        assertEquals(0, first.current());
        assertEquals(1, StepperModel.flowFor(Role.AGENT, ViewId.FLIGHT_SEARCH).orElseThrow().current());
        assertEquals(2, StepperModel.flowFor(Role.AGENT, ViewId.SEAT_SELECTION).orElseThrow().current());
    }

    @Test
    void screensOutsideATaskHaveNoSteps() {
        for (ViewId view : List.of(ViewId.LOGIN, ViewId.CUSTOMER_DASHBOARD, ViewId.MY_RESERVATIONS, ViewId.PROFILE,
                ViewId.PAYMENT_RECORDS, ViewId.SCHEDULE)) {
            assertTrue(StepperModel.flowFor(Role.CUSTOMER, view).isEmpty(), view.name());
        }
        assertTrue(StepperModel.flowFor(Role.AGENT, ViewId.CUSTOMER_MANAGEMENT).isEmpty());
        assertTrue(StepperModel.flowFor(Role.AGENT, ViewId.PAYMENT).isEmpty());
    }

    @Test
    void anAdminHasNoSteps() {
        for (ViewId view : ViewId.values()) {
            assertTrue(StepperModel.flowFor(Role.ADMIN, view).isEmpty(), view.name());
        }
    }

    @Test
    void theCurrentStepIsAlwaysOneOfTheSteps() {
        for (Role role : Role.values()) {
            for (ViewId view : ViewId.values()) {
                StepperModel.flowFor(role, view).ifPresent(flow -> assertTrue(
                        flow.current() >= 0 && flow.current() < flow.steps().size(), role + " " + view));
            }
        }
    }
}
