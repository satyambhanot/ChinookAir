package ca.ucalgary.chinookair.business.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PricingPolicyTest {

    @Test
    void businessCostsOneAndAHalfTimesTheBase() {
        assertEquals(new BigDecimal("300.00"), PricingPolicy.priceFor(new BigDecimal("200.00"), SeatClass.BUSINESS));
    }

    @Test
    void businessRoundsHalfUpToCents() {
        // 199.99 * 1.5 = 299.985
        assertEquals(new BigDecimal("299.99"), PricingPolicy.priceFor(new BigDecimal("199.99"), SeatClass.BUSINESS));
    }

    @Test
    void economyStaysAtTheBase() {
        assertEquals(new BigDecimal("199.99"), PricingPolicy.priceFor(new BigDecimal("199.99"), SeatClass.ECONOMY));
    }

    @Test
    void resultAlwaysHasTwoDecimals() {
        assertEquals(2, PricingPolicy.priceFor(new BigDecimal("200"), SeatClass.ECONOMY).scale());
        assertEquals(new BigDecimal("150.00"), PricingPolicy.priceFor(new BigDecimal("100"), SeatClass.BUSINESS));
    }

    @Test
    void inputIsRequired() {
        assertThrows(IllegalArgumentException.class, () -> PricingPolicy.priceFor(null, SeatClass.ECONOMY));
        assertThrows(IllegalArgumentException.class, () -> PricingPolicy.priceFor(BigDecimal.TEN, null));
    }
}
