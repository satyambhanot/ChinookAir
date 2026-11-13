package ca.ucalgary.chinookair.data;

import java.util.Optional;

/**
 * A small key and value store for facts the program has to remember between runs,
 * like the month in which the promotion was last shown.
 */
public interface AppStateDao {

    /** The value of a key, or empty if it was never set. */
    Optional<String> get(String key);

    /** Sets the value of a key. A key that already has a value is overwritten, there is still one row. */
    void put(String key, String value);
}
