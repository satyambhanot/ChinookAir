package ca.ucalgary.chinookair.business.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RandomConfirmationNumberGeneratorTest {

    @Test
    void aThousandNumbersHaveTheRightShapeAndAreUnique() {
        ConfirmationNumberGenerator generator = new RandomConfirmationNumberGenerator(new Random(42));
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String number = generator.next();
            assertTrue(number.matches("^CA[A-Z0-9]{6}$"), number);
            assertTrue(seen.add(number), "duplicate " + number);
        }
        assertEquals(1000, seen.size());
    }

    @Test
    void theDefaultGeneratorWorksToo() {
        ConfirmationNumberGenerator generator = new RandomConfirmationNumberGenerator();
        assertTrue(generator.next().matches("^CA[A-Z0-9]{6}$"));
    }

    @Test
    void theSameSeedGivesTheSameNumbers() {
        assertEquals(new RandomConfirmationNumberGenerator(new Random(7)).next(),
                new RandomConfirmationNumberGenerator(new Random(7)).next());
    }
}
