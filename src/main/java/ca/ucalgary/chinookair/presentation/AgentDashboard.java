package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Role;
import java.util.List;

/** What a flight agent can do for customers: manage them and their reservations and payments, and see schedules. */
class AgentDashboard extends Dashboard {

    private static final long serialVersionUID = 1L;

    AgentDashboard(Controllers controllers, Navigator navigator) {
        super(controllers, navigator, Role.AGENT, "Flight agent", List.of(
                new Task("Customers", 'C', ViewId.CUSTOMER_MANAGEMENT),
                new Task("Reservations", 'R', ViewId.MY_RESERVATIONS),
                new Task("Payment records", 'P', ViewId.PAYMENT_RECORDS),
                new Task("Schedule", 'S', ViewId.SCHEDULE)));
    }
}
