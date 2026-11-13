package ca.ucalgary.chinookair.business.entity;

/** Factory: picks the payment method for the card type, so no caller needs an if on the type. */
public final class PaymentMethods {

    private PaymentMethods() {
    }

    public static PaymentMethod forType(PaymentMethodType type) {
        if (type == null) {
            throw new IllegalArgumentException("A card type is required");
        }
        switch (type) {
            case CREDIT:
                return new CreditCardPayment();
            case DEBIT:
                return new DebitCardPayment();
            default:
                throw new IllegalArgumentException("Unknown card type " + type);
        }
    }
}
