package one.jpro.auth.http.impl;

/**
 * Task handle returned by {@link Scheduler} that facilitates task cancellation.
 *
 * @author Besmir Beqiri
 */
@FunctionalInterface
interface Cancellable {

    /**
     * Cancel a scheduled task.
     */
    void cancel();
}
