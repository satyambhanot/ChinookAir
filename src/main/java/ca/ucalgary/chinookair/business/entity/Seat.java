package ca.ucalgary.chinookair.business.entity;

/** One numbered place on a flight, like 12A. */
public record Seat(String number, SeatClass seatClass, boolean available) {

    public Seat {
        if (number == null || number.isBlank()) {
            throw new IllegalArgumentException("Seat number must not be blank");
        }
        if (seatClass == null) {
            throw new IllegalArgumentException("Seat class is required");
        }
        number = number.trim().toUpperCase();
    }
}
