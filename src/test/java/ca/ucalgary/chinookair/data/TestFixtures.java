package ca.ucalgary.chinookair.data;

import ca.ucalgary.chinookair.business.entity.Aircraft;
import ca.ucalgary.chinookair.business.entity.Airline;
import ca.ucalgary.chinookair.business.entity.Airport;
import ca.ucalgary.chinookair.business.entity.Route;
import java.util.Map;

/** Short ways to make the small objects that DAO tests need. None of them is saved yet. */
public final class TestFixtures {

    private static final Map<String, String> AIRLINES = Map.of(
            "AC", "Air Canada", "WS", "WestJet", "PD", "Porter Airlines");
    private static final Map<String, String> CITIES = Map.of(
            "YYC", "Calgary", "YVR", "Vancouver", "YYZ", "Toronto", "YEG", "Edmonton", "YUL", "Montreal",
            "YOW", "Ottawa");

    private TestFixtures() {
    }

    public static Airline airline(String code) {
        return new Airline(code, AIRLINES.getOrDefault(code, "Airline " + code));
    }

    public static Airport airport(String code) {
        return new Airport(code, CITIES.getOrDefault(code, "City " + code));
    }

    public static Aircraft aircraft() {
        return new Aircraft(null, "A320", 30, 6);
    }

    public static Route route(String origin, String destination) {
        return new Route(null, airport(origin), airport(destination));
    }
}
