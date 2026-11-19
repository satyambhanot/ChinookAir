package ca.ucalgary.chinookair.business.control;

/** Makes the code that identifies a reservation, like CA7K2P9X. Tests use a fixed one. */
public interface ConfirmationNumberGenerator {

    String next();
}
