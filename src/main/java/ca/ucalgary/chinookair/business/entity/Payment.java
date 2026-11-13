package ca.ucalgary.chinookair.business.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * A simulated charge for a reservation. It keeps only the masked card, and the
 * status only moves the way the payment state diagram allows.
 */
public final class Payment {

    private static final Pattern MASKED = Pattern.compile("\\*{4} \\*{4} \\*{4} \\d{4}");

    private Long id;
    private final Long reservationId;
    private final BigDecimal amount;
    private final PaymentMethodType type;
    private PaymentStatus status;
    private final String maskedCard;
    private final LocalDateTime createdAt;
    private String message;

    /** A new payment, waiting for the authorization. */
    public Payment(Long reservationId, BigDecimal amount, PaymentMethodType type, String maskedCard,
                   LocalDateTime createdAt) {
        this(null, reservationId, amount, type, PaymentStatus.PENDING, maskedCard, createdAt, null);
    }

    private Payment(Long id, Long reservationId, BigDecimal amount, PaymentMethodType type, PaymentStatus status,
                    String maskedCard, LocalDateTime createdAt, String message) {
        if (reservationId == null) {
            throw new IllegalArgumentException("A payment belongs to a reservation");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("The amount must be greater than 0");
        }
        if (type == null || status == null || createdAt == null) {
            throw new IllegalArgumentException("A payment needs a type, a status and a creation time");
        }
        // the full card number must never get this far
        if (maskedCard == null || !MASKED.matcher(maskedCard).matches()) {
            throw new IllegalArgumentException("Only a masked card like **** **** **** 1111 can be stored");
        }
        this.id = id;
        this.reservationId = reservationId;
        this.amount = amount;
        this.type = type;
        this.status = status;
        this.maskedCard = maskedCard;
        this.createdAt = createdAt;
        this.message = message;
    }

    /** Rebuilds a saved payment. */
    public static Payment restore(Long id, Long reservationId, BigDecimal amount, PaymentMethodType type,
                                  PaymentStatus status, String maskedCard, LocalDateTime createdAt,
                                  String message) {
        return new Payment(id, reservationId, amount, type, status, maskedCard, createdAt, message);
    }

    /** PENDING to APPROVED. */
    public void approve() {
        require(PaymentStatus.PENDING, "approve");
        status = PaymentStatus.APPROVED;
        message = "Approved";
    }

    /** PENDING to DECLINED, keeping the reason. */
    public void decline(String reason) {
        require(PaymentStatus.PENDING, "decline");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required");
        }
        status = PaymentStatus.DECLINED;
        message = reason.trim();
    }

    /** APPROVED to REFUNDED. */
    public void refund() {
        require(PaymentStatus.APPROVED, "refund");
        status = PaymentStatus.REFUNDED;
        message = "Refunded";
    }

    private void require(PaymentStatus needed, String action) {
        if (status != needed) {
            throw new IllegalStateException("Cannot " + action + " a " + status.name().toLowerCase() + " payment");
        }
    }

    public Long id() {
        return id;
    }

    /** Called once by the data layer when the payment is saved for the first time. */
    public void assignId(long newId) {
        if (id != null) {
            throw new IllegalStateException("This payment already has an id");
        }
        id = newId;
    }

    public Long reservationId() {
        return reservationId;
    }

    public BigDecimal amount() {
        return amount;
    }

    public PaymentMethodType type() {
        return type;
    }

    public PaymentStatus status() {
        return status;
    }

    public String maskedCard() {
        return maskedCard;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    /** @return the approval text or the reason for a decline, null while pending */
    public String message() {
        return message;
    }

    @Override
    public String toString() {
        return "Payment[" + id + ", " + status + ", " + amount + ", " + maskedCard + "]";
    }
}
