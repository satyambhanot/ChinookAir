package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Role;
import java.util.List;

/** What a customer can do: find flights, look at their reservations and edit their profile. */
class CustomerDashboard extends Dashboard {

    private static final long serialVersionUID = 1L;

    CustomerDashboard(Controllers controllers, Navigator navigator) {
        super(controllers, navigator, Role.CUSTOMER, "Customer", List.of(
                new Task("Search flights", "Find a flight, pick your seat and pay",
                        "search", 'S', ViewId.FLIGHT_SEARCH),
                new Task("My reservations", "See, pay for or cancel your bookings",
                        "ticket", 'M', ViewId.MY_RESERVATIONS),
                new Task("My profile", "Keep your contact details up to date", "user", 'P', ViewId.PROFILE)));
    }
}
