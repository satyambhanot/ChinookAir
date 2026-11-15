package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.ucalgary.chinookair.business.entity.Role;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class NavModelTest {

    private static List<String> labels(Role role) {
        return NavModel.forRole(role).tabs().stream().map(NavModel.Tab::label).toList();
    }

    private static String selected(Role role, ViewId view) {
        return NavModel.forRole(role).selected(view).map(NavModel.Tab::label).orElse(null);
    }

    @Test
    void eachRoleHasItsOwnTabs() {
        assertEquals(List.of("Find flights", "My reservations", "My profile"), labels(Role.CUSTOMER));
        assertEquals(List.of("Customers", "Payment records", "Schedule"), labels(Role.AGENT));
        assertEquals(List.of("Flights", "Routes", "Aircraft", "Schedule"), labels(Role.ADMIN));
    }

    @Test
    void everyTabOpensAScreenThatBelongsToIt() {
        for (Role role : Role.values()) {
            NavModel model = NavModel.forRole(role);
            for (NavModel.Tab tab : model.tabs()) {
                assertEquals(Optional.of(tab), model.selected(tab.opens()), role + " " + tab.label());
            }
        }
    }

    @Test
    void theStepsOfABookingStayInTheFindFlightsTab() {
        for (ViewId view : List.of(ViewId.FLIGHT_SEARCH, ViewId.SEAT_SELECTION, ViewId.PAYMENT, ViewId.CONFIRMATION)) {
            assertEquals("Find flights", selected(Role.CUSTOMER, view), view.name());
        }
    }

    @Test
    void anAgentWorkingOnAReservationStaysInTheCustomersTab() {
        for (ViewId view : List.of(ViewId.MY_RESERVATIONS, ViewId.MODIFY_RESERVATION, ViewId.FLIGHT_SEARCH,
                ViewId.SEAT_SELECTION)) {
            assertEquals("Customers", selected(Role.AGENT, view), view.name());
        }
    }

    @Test
    void theSameScreenCanBelongToDifferentTabsForDifferentRoles() {
        assertEquals("My reservations", selected(Role.CUSTOMER, ViewId.MY_RESERVATIONS));
        assertEquals("Customers", selected(Role.AGENT, ViewId.MY_RESERVATIONS));
        assertEquals("Schedule", selected(Role.AGENT, ViewId.SCHEDULE));
        assertEquals("Schedule", selected(Role.ADMIN, ViewId.SCHEDULE));
    }

    @Test
    void homeAndTheScreensBeforeTheLoginHaveNoTab() {
        for (Role role : Role.values()) {
            for (ViewId view : List.of(ViewId.LOGIN, ViewId.REGISTER, ViewId.dashboardFor(role))) {
                assertTrue(NavModel.forRole(role).selected(view).isEmpty(), role + " " + view);
            }
        }
    }

    @Test
    void aScreenOfAnotherRoleHasNoTab() {
        assertEquals(null, selected(Role.CUSTOMER, ViewId.FLIGHT_MANAGEMENT));
        assertEquals(null, selected(Role.ADMIN, ViewId.PROFILE));
    }

    @Test
    void theTabListCannotBeChanged() {
        assertThrows(UnsupportedOperationException.class, () -> NavModel.forRole(Role.ADMIN).tabs().clear());
    }
}
