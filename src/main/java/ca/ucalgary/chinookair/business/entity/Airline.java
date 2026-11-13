package ca.ucalgary.chinookair.business.entity;

/** The company that operates flights. The code is the two-letter airline code, like AC. */
public record Airline(String code, String name) {

    public Airline {
        if (code == null || !code.trim().matches("[A-Za-z]{2}")) {
            throw new IllegalArgumentException("Airline code must be 2 letters");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Airline name must not be blank");
        }
        code = code.trim().toUpperCase();
        name = name.trim();
    }
}
