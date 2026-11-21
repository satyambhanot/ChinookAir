package ca.ucalgary.chinookair;

import ca.ucalgary.chinookair.business.control.Controllers;
import ca.ucalgary.chinookair.business.control.FlightOffer;
import ca.ucalgary.chinookair.business.control.SearchCriteria;
import ca.ucalgary.chinookair.business.entity.Role;
import ca.ucalgary.chinookair.business.entity.User;
import java.io.PrintStream;
import java.time.Clock;
import java.util.List;

/**
 * The check behind {@code java -jar ChinookAir.jar --selftest}. It needs no screen and never touches the
 * user's database: it opens a throw-away in-memory database, seeds it, logs in as the demo customer and
 * searches for a flight. That proves the jar has its SQLite driver, its schema and its data.
 */
final class SelfTest {

    /** The command line argument that runs the check instead of opening the window. */
    static final String ARGUMENT = "--selftest";

    private SelfTest() {
    }

    /**
     * Runs the check and reports the result on the stream.
     *
     * @return true and prints {@code SELFTEST OK} if it passed, false and prints the reason otherwise
     */
    static boolean run(PrintStream out) {
        Controllers controllers = null;
        try {
            Clock clock = Clock.systemDefaultZone();
            controllers = AppContext.create("jdbc:sqlite::memory:", clock);
            User user = controllers.loginController().authenticate("customer1", "customer123");
            if (user.role() != Role.CUSTOMER) {
                return fail(out, "the demo customer did not get the customer role");
            }
            List<FlightOffer> offers = controllers.flightSearchController().search(
                    new SearchCriteria("YYC", "YVR", controllers.flightSearchController().today().plusDays(1), null));
            if (offers.isEmpty()) {
                return fail(out, "the demo data has no flight from YYC to YVR tomorrow");
            }
            out.println("SELFTEST OK");
            return true;
        } catch (RuntimeException e) {
            return fail(out, e.toString());
        } finally {
            if (controllers != null) {
                controllers.shutdown().run();
            }
        }
    }

    private static boolean fail(PrintStream out, String reason) {
        out.println("SELFTEST FAILED: " + reason);
        return false;
    }
}
