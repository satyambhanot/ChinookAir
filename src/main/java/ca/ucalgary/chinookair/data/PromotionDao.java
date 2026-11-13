package ca.ucalgary.chinookair.data;

import ca.ucalgary.chinookair.business.entity.Promotion;
import java.util.List;
import java.util.Optional;

/** Loads and saves the monthly promotions. */
public interface PromotionDao {

    /** All promotions in the order they were saved. */
    List<Promotion> findAll();

    Optional<Promotion> findById(long promotionId);

    /**
     * Adds a new promotion and returns it with its id.
     *
     * @throws IllegalArgumentException if it already has an id
     */
    Promotion save(Promotion promotion);
}
