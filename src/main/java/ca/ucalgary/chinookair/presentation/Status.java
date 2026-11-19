package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.entity.PaymentStatus;
import ca.ucalgary.chinookair.business.entity.ReservationStatus;
import java.awt.Color;

/**
 * What a status is called on screen and how it looks: plain words and one of five kinds of badge.
 * The words always show, so the colour is never the only thing that tells the status apart.
 */
record Status(String text, Kind kind) {

    /** How a badge is coloured. */
    enum Kind {
        SUCCESS(Palette.SUCCESS_TEXT, Palette.SUCCESS_TINT),
        WARNING(Palette.WARNING_TEXT, Palette.WARNING_TINT),
        DANGER(Palette.DANGER_TEXT, Palette.DANGER_TINT),
        INFO(Palette.NAVY_900, Palette.SKY_100),
        NEUTRAL(Palette.TEXT_MUTED, Palette.TABLE_HEADER);

        private final Color text;
        private final Color fill;

        Kind(Color text, Color fill) {
            this.text = text;
            this.fill = fill;
        }

        Color text() {
            return text;
        }

        Color fill() {
            return fill;
        }
    }

    static Status of(ReservationStatus status) {
        switch (status) {
            case CREATED:
                return new Status("New", Kind.NEUTRAL);
            case PENDING_PAYMENT:
                return new Status("Awaiting payment", Kind.WARNING);
            case CONFIRMED:
                return new Status("Confirmed", Kind.SUCCESS);
            case CANCELLED:
                return new Status("Cancelled", Kind.NEUTRAL);
            case COMPLETED:
                return new Status("Completed", Kind.INFO);
            default:
                throw new IllegalArgumentException("Unknown status " + status);
        }
    }

    static Status of(PaymentStatus status) {
        switch (status) {
            case PENDING:
                return new Status("Pending", Kind.WARNING);
            case APPROVED:
                return new Status("Paid", Kind.SUCCESS);
            case DECLINED:
                return new Status("Declined", Kind.DANGER);
            case REFUNDED:
                return new Status("Refunded", Kind.INFO);
            default:
                throw new IllegalArgumentException("Unknown status " + status);
        }
    }

    /** The words only, so a table that sorts or copies the cell gets readable text. */
    @Override
    public String toString() {
        return text;
    }
}
