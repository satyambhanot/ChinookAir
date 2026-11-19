package ca.ucalgary.chinookair.business.control;

import ca.ucalgary.chinookair.business.entity.Promotion;

/**
 * Observer: whoever wants to show the monthly promotion registers one of these with the
 * PromotionController. The controller never needs to know that the listener is a Swing window.
 */
@FunctionalInterface
public interface PromotionListener {

    void onPromotion(Promotion promotion);
}
