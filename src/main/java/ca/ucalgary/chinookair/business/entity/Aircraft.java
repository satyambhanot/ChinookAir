package ca.ucalgary.chinookair.business.entity;

/** A plane model with a fixed seat layout: some rows with the same number of seats in each. */
public record Aircraft(Long id, String model, int rows, int seatsPerRow) {

    public static final int MAX_ROWS = 60;
    public static final int MAX_SEATS_PER_ROW = 6;

    public Aircraft {
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Aircraft model must not be blank");
        }
        if (rows < 1 || rows > MAX_ROWS) {
            throw new IllegalArgumentException("Rows must be between 1 and " + MAX_ROWS);
        }
        if (seatsPerRow < 1 || seatsPerRow > MAX_SEATS_PER_ROW) {
            throw new IllegalArgumentException("Seats per row must be between 1 and " + MAX_SEATS_PER_ROW);
        }
        model = model.trim();
    }

    public int seatCount() {
        return rows * seatsPerRow;
    }
}
