package ca.ucalgary.chinookair.business.entity;

import java.math.BigDecimal;

/** Debit cards follow the credit rule and also have a limit per payment. */
class DebitCardPayment implements PaymentMethod {

    static final BigDecimal LIMIT = new BigDecimal("5000.00");

    @Override
    public AuthorizationResult authorize(PaymentDetails details, BigDecimal amount) {
        if ("0000".equals(details.last4())) {
            return new AuthorizationResult(false, "Card declined");
        }
        if (amount.compareTo(LIMIT) > 0) {
            return new AuthorizationResult(false, "Amount is over the debit limit of " + LIMIT);
        }
        return new AuthorizationResult(true, "Approved");
    }
}
