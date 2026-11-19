package ca.ucalgary.chinookair.business.entity;

/** A place flights leave from or arrive at. The code is the three-letter code, like YYC. */
public record Airport(String code, String city) {

    public Airport {
        if (code == null || !code.trim().matches("[A-Za-z]{3}")) {
            throw new IllegalArgumentException("Airport code must be 3 letters");
        }
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City must not be blank");
        }
        code = code.trim().toUpperCase();
        city = city.trim();
    }
}
