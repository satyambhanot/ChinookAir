package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.entity.Role;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The tabs in the top bar for one role, and which tab a screen belongs to. A screen that is a step of
 * a task, like choosing a seat, belongs to the tab where the task started, so that tab stays marked
 * while the user is inside the task. It holds no Swing, so the map of screens to tabs is easy to test.
 */
final class NavModel {

    /** One tab: its words, the screen it opens, and the screens that count as being inside it. */
    record Tab(String label, ViewId opens, List<ViewId> contains) {
    }

    private final List<Tab> tabs;
    private final Map<ViewId, Tab> owner = new EnumMap<>(ViewId.class);

    private NavModel(List<Tab> tabs) {
        this.tabs = List.copyOf(tabs);
        for (Tab tab : tabs) {
            owner.put(tab.opens(), tab);
            for (ViewId view : tab.contains()) {
                owner.put(view, tab);
            }
        }
    }

    static NavModel forRole(Role role) {
        List<Tab> tabs = new ArrayList<>();
        switch (role) {
            case CUSTOMER:
                tabs.add(new Tab("Find flights", ViewId.FLIGHT_SEARCH,
                        List.of(ViewId.SEAT_SELECTION, ViewId.PAYMENT, ViewId.CONFIRMATION)));
                tabs.add(new Tab("My reservations", ViewId.MY_RESERVATIONS, List.of()));
                tabs.add(new Tab("My profile", ViewId.PROFILE, List.of()));
                break;
            case AGENT:
                // an agent reaches the reservations, the flight search and the seats through a customer
                tabs.add(new Tab("Customers", ViewId.CUSTOMER_MANAGEMENT, List.of(ViewId.MY_RESERVATIONS,
                        ViewId.MODIFY_RESERVATION, ViewId.FLIGHT_SEARCH, ViewId.SEAT_SELECTION, ViewId.PAYMENT,
                        ViewId.CONFIRMATION)));
                tabs.add(new Tab("Payment records", ViewId.PAYMENT_RECORDS, List.of()));
                tabs.add(new Tab("Schedule", ViewId.SCHEDULE, List.of()));
                break;
            case ADMIN:
                tabs.add(new Tab("Flights", ViewId.FLIGHT_MANAGEMENT, List.of()));
                tabs.add(new Tab("Routes", ViewId.ROUTE_MANAGEMENT, List.of()));
                tabs.add(new Tab("Aircraft", ViewId.AIRCRAFT_MANAGEMENT, List.of()));
                tabs.add(new Tab("Schedule", ViewId.SCHEDULE, List.of()));
                break;
            default:
                throw new IllegalArgumentException("Unknown role " + role);
        }
        return new NavModel(tabs);
    }

    List<Tab> tabs() {
        return tabs;
    }

    /** The tab a screen belongs to; empty for Home and the screens before the login. */
    Optional<Tab> selected(ViewId view) {
        return Optional.ofNullable(owner.get(view));
    }
}
