package ca.ucalgary.chinookair.business.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** How the price of a seat follows from the base price of its flight. */
public final class PricingPolicy {

    private static final BigDecimal BUSINESS_FACTOR = new BigDecimal("1.5");

    private PricingPolicy() {
    }

    /** Economy pays the base price, business pays 1.5 times as much, rounded half up to cents. */
    public static BigDecimal priceFor(BigDecimal base, SeatClass seatClass) {
        if (base == null || seatClass == null) {
            throw new IllegalArgumentException("A base price and a seat class are required");
        }
        BigDecimal price = seatClass == SeatClass.BUSINESS ? base.multiply(BUSINESS_FACTOR) : base;
        return price.setScale(2, RoundingMode.HALF_UP);
    }
}
