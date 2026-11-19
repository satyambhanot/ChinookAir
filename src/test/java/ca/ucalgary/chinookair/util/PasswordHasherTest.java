package ca.ucalgary.chinookair.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    @Test
    void hashHasTheStoredFormatAndHidesThePassword() {
        String hash = PasswordHasher.hash("admin123");
        assertTrue(hash.startsWith("pbkdf2$"));
        assertFalse(hash.contains("admin123"));
        String[] parts = hash.split("\\$");
        assertEquals(4, parts.length);
        assertEquals("120000", parts[1]);
    }

    @Test
    void rightPasswordVerifiesAndWrongOneDoesNot() {
        String hash = PasswordHasher.hash("admin123");
        assertTrue(PasswordHasher.verify("admin123", hash));
        assertFalse(PasswordHasher.verify("Admin123", hash));
        assertFalse(PasswordHasher.verify("", hash));
    }

    @Test
    void sameStoredValueVerifiesEveryTime() {
        String hash = PasswordHasher.hash("s3cret pass");
        assertTrue(PasswordHasher.verify("s3cret pass", hash));
        assertTrue(PasswordHasher.verify("s3cret pass", hash));
    }

    @Test
    void samePasswordTwiceGivesDifferentHashes() {
        String first = PasswordHasher.hash("admin123");
        String second = PasswordHasher.hash("admin123");
        assertNotEquals(first, second);
        assertTrue(PasswordHasher.verify("admin123", first));
        assertTrue(PasswordHasher.verify("admin123", second));
    }

    @Test
    void malformedStoredValuesReturnFalseWithoutThrowing() {
        String[] bad = {
            "garbage", "", "pbkdf2", "pbkdf2$120000$abc", "pbkdf2$x$AAAA$BBBB", "pbkdf2$0$AAAA$BBBB",
            "pbkdf2$120000$!!!$???", "md5$120000$AAAA$BBBB", "pbkdf2$99999999999$AAAA$BBBB",
            "pbkdf2$120000$$", "pbkdf2$120000$AAAA$BBBB$extra"
        };
        for (String stored : bad) {
            assertDoesNotThrow(() -> PasswordHasher.verify("admin123", stored), stored);
            assertFalse(PasswordHasher.verify("admin123", stored), stored);
        }
        assertFalse(PasswordHasher.verify("admin123", null));
        assertFalse(PasswordHasher.verify(null, PasswordHasher.hash("admin123")));
    }

    @Test
    void aNullPasswordCannotBeHashed() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(null));
    }
}
