package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.entity.Role;
import java.util.List;

/** What a flight agent can do for customers: manage them and their reservations and payments, and see schedules. */
class AgentDashboard extends Dashboard {

    private static final long serialVersionUID = 1L;

    AgentDashboard(Controllers controllers, Navigator navigator) {
        super(controllers, navigator, Role.AGENT, "Flight agent", List.of(
                new Task("Customers", "Find a customer, add one, change bookings",
                        "user", 'C', ViewId.CUSTOMER_MANAGEMENT),
                new Task("Payment records", "Check what was paid and refunded", "card", 'P', ViewId.PAYMENT_RECORDS),
                new Task("Schedule", "See the flights day by day", "calendar", 'S', ViewId.SCHEDULE)));
    }
}
