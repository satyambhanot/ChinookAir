package ca.ucalgary.chinookair.business.control;

import java.security.SecureRandom;
import java.util.Random;

/** The usual generator: CA and six random uppercase letters or digits. */
public class RandomConfirmationNumberGenerator implements ConfirmationNumberGenerator {

    private static final String PREFIX = "CA";
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int LENGTH = 6;

    private final Random random;

    public RandomConfirmationNumberGenerator() {
        this(new SecureRandom());
    }

    /** A given Random lets a test get the same numbers every time. */
    public RandomConfirmationNumberGenerator(Random random) {
        this.random = random;
    }

    @Override
    public String next() {
        StringBuilder number = new StringBuilder(PREFIX);
        for (int i = 0; i < LENGTH; i++) {
            number.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return number.toString();
    }
}
