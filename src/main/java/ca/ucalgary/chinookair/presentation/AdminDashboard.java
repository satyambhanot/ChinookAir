package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Role;
import java.util.List;

/** What an administrator can do: manage flights, routes, aircraft and schedules. */
class AdminDashboard extends Dashboard {

    private static final long serialVersionUID = 1L;

    AdminDashboard(Controllers controllers, Navigator navigator) {
        super(controllers, navigator, Role.ADMIN, "Administrator", List.of(
                new Task("Flights", "Add, change or remove flights", "plane", 'F', ViewId.FLIGHT_MANAGEMENT),
                new Task("Routes", "Choose which airports are connected", "swap", 'R', ViewId.ROUTE_MANAGEMENT),
                new Task("Aircraft", "Manage the planes and their seats", "grid", 'A', ViewId.AIRCRAFT_MANAGEMENT),
                new Task("Schedule", "See the flights and move a departure", "calendar", 'S', ViewId.SCHEDULE)));
    }
}
