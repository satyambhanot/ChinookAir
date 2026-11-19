package ca.ucalgary.chinookair.business.control;

import ca.ucalgary.chinookair.business.entity.Promotion;
import ca.ucalgary.chinookair.data.AppStateDao;
import ca.ucalgary.chinookair.data.PromotionDao;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Use case View Monthly Promotion. On the first start in a calendar month it picks one
 * promotion at random and tells the listeners; for the rest of that month it stays quiet.
 */
public class PromotionController {

    /** The key under which the month of the last promotion is remembered. */
    static final String LAST_MONTH_KEY = "last_promo_month";

    private final PromotionDao promotions;
    private final AppStateDao state;
    private final Clock clock;
    private final IntUnaryOperator chooseIndex;
    private final List<PromotionListener> listeners = new ArrayList<>();

    /**
     * @param chooseIndex gets the number of promotions and returns an index from 0 up to one less;
     *                    the program passes a random one, tests pass a fixed one
     */
    public PromotionController(PromotionDao promotions, AppStateDao state, Clock clock,
                               IntUnaryOperator chooseIndex) {
        this.promotions = promotions;
        this.state = state;
        this.clock = clock;
        this.chooseIndex = chooseIndex;
    }

    public void addListener(PromotionListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("A listener is required");
        }
        listeners.add(listener);
    }

    /**
     * Shows this month's promotion if it was not shown yet. The month is saved as soon as the promotion
     * is picked and before the listeners hear about it, so a crash while showing it cannot cause a second
     * showing. Without any promotion nothing happens and the month is not saved.
     */
    public void checkOnStartup() {
        String thisMonth = YearMonth.now(clock).toString();
        if (state.get(LAST_MONTH_KEY).filter(thisMonth::equals).isPresent()) {
            return;
        }
        List<Promotion> all = promotions.findAll();
        if (all.isEmpty()) {
            return;
        }
        Promotion picked = all.get(chooseIndex.applyAsInt(all.size()));
        state.put(LAST_MONTH_KEY, thisMonth);
        for (PromotionListener listener : listeners) {
            listener.onPromotion(picked);
        }
    }
}
