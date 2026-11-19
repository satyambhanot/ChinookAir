package ca.ucalgary.chinookair.business.entity;

import java.math.BigDecimal;

/** Credit cards are approved unless the card number ends in 0000. */
class CreditCardPayment implements PaymentMethod {

    @Override
    public AuthorizationResult authorize(PaymentDetails details, BigDecimal amount) {
        if ("0000".equals(details.last4())) {
            return new AuthorizationResult(false, "Card declined");
        }
        return new AuthorizationResult(true, "Approved");
    }
}
