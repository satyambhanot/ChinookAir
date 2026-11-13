package ca.ucalgary.chinookair.business.control;

/**
 * Every controller of the program, plus the session of the person using it. The screens receive
 * this one object and never build a controller or a DAO themselves. {@code shutdown} closes the
 * database when the window closes.
 */
public record Controllers(
        LoginController loginController,
        CustomerController customerController,
        FlightSearchController flightSearchController,
        BookingController bookingController,
        PaymentController paymentController,
        ReservationController reservationController,
        FlightManagementController flightManagementController,
        RouteController routeController,
        AircraftController aircraftController,
        ScheduleController scheduleController,
        PromotionController promotionController,
        SessionContext session,
        Runnable shutdown) {
}
