package one.jpro.platform.auth.core.http.impl;

/**
 * Simple clock abstraction that produces clock times suitable for calculating time deltas.
 *
 * @author Besmir Beqiri
 */
@FunctionalInterface
interface Clock {

    /**
     * Time now in nanoseconds.
     */
    long nanoTime();
}
