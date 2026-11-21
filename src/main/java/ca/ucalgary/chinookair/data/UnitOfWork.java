package ca.ucalgary.chinookair.data;

import java.util.function.Supplier;

/**
 * Lets a controller change several things in the database as one: either all of the changes are kept or none,
 * without the controller knowing anything about SQL. Calls inside a running unit join it.
 */
public interface UnitOfWork {

    /** Runs the work and keeps all of its changes, or undoes them all if it throws. */
    <T> T call(Supplier<T> work);

    /** The same for work that gives nothing back. */
    default void run(Runnable work) {
        call(() -> {
            work.run();
            return null;
        });
    }
}
