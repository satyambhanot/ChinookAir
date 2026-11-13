package ca.ucalgary.chinookair.business.entity;

/** The monthly offer shown on the first launch of a month. The id is null until it is saved. */
public record Promotion(Long id, String title, String description) {

    public Promotion {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Promotion title must not be blank");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Promotion description must not be blank");
        }
        title = title.trim();
        description = description.trim();
    }
}
