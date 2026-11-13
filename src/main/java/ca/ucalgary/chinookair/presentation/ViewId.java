package ca.ucalgary.chinookair.presentation;

/** The screens of the program. The navigator shows one of them at a time. */
public enum ViewId {
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

    /** The name in words, like "Flight search". */
    public String title() {
        String words = name().replace('_', ' ').toLowerCase();
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }
}
