package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.entity.Role;

/** The screens of the program. The navigator shows one of them at a time. */
enum ViewId {
    LOGIN,
    REGISTER,
    CUSTOMER_DASHBOARD,
    AGENT_DASHBOARD,
    ADMIN_DASHBOARD,
    FLIGHT_SEARCH,
    SEAT_SELECTION,
    PAYMENT,
    CONFIRMATION,
    MY_RESERVATIONS,
    MODIFY_RESERVATION,
    CUSTOMER_MANAGEMENT,
    PROFILE,
    PAYMENT_RECORDS,
    FLIGHT_MANAGEMENT,
    ROUTE_MANAGEMENT,
    AIRCRAFT_MANAGEMENT,
    SCHEDULE;

    /** The dashboard that belongs to a role. */
    public static ViewId dashboardFor(Role role) {
        switch (role) {
            case CUSTOMER:
                return CUSTOMER_DASHBOARD;
            case AGENT:
                return AGENT_DASHBOARD;
            case ADMIN:
                return ADMIN_DASHBOARD;
            default:
                throw new IllegalArgumentException("Unknown role " + role);
        }
    }

    /**
     * The screen a person lands on after logging in: the customer's Home, the agent's customer list
     * and the administrator's flights, because those are where each of them starts working.
     */
    public static ViewId landingFor(Role role) {
        switch (role) {
            case CUSTOMER:
                return CUSTOMER_DASHBOARD;
            case AGENT:
                return CUSTOMER_MANAGEMENT;
            case ADMIN:
                return FLIGHT_MANAGEMENT;
            default:
                throw new IllegalArgumentException("Unknown role " + role);
        }
    }

    /** The name the top bar shows: short and in the words a user would use. */
    public String label() {
        switch (this) {
            case LOGIN:
                return "Log in";
            case REGISTER:
                return "Create account";
            case CUSTOMER_DASHBOARD:
            case AGENT_DASHBOARD:
            case ADMIN_DASHBOARD:
                return "Home";
            case FLIGHT_SEARCH:
                return "Find flights";
            case SEAT_SELECTION:
                return "Choose seats";
            case PAYMENT:
                return "Payment";
            case CONFIRMATION:
                return "Confirmation";
            case MY_RESERVATIONS:
                return "Reservations";
            case MODIFY_RESERVATION:
                return "Modify reservation";
            case CUSTOMER_MANAGEMENT:
                return "Customers";
            case PROFILE:
                return "My profile";
            case PAYMENT_RECORDS:
                return "Payment records";
            case FLIGHT_MANAGEMENT:
                return "Flights";
            case ROUTE_MANAGEMENT:
                return "Routes";
            case AIRCRAFT_MANAGEMENT:
                return "Aircraft";
            case SCHEDULE:
                return "Schedule";
            default:
                return title();
        }
    }

    /** The name in words, like "Flight search". */
    public String title() {
        String words = name().replace('_', ' ').toLowerCase();
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }
}
