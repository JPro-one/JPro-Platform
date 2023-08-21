package one.jpro.auth.http.impl;

import java.time.Duration;
import java.util.*;

/**
 * Scheduler is a simple data structure for efficiently scheduling deferred tasks and draining
 * expired tasks. A {@link Cancellable} handle is returned to clients when a new task is scheduled.
 * That handle can be used to cancel a task.
 *
 * @author Besmir Beqiri
 */
final class Scheduler {

    private final Clock clock;
    private final SortedSet<Task> tasks;
    private long counter;

    /**
     * Constructs a new Scheduler with the default system clock.
     */
    Scheduler() {
        this(new SystemClock());
    }

    /**
     * Constructs a new Scheduler with the specified clock.
     *
     * @param clock the clock to use for time-related operations
     */
    Scheduler(Clock clock) {
        this.clock = clock;
        this.tasks = new TreeSet<>(Comparator.comparing((Task t) -> t.time).thenComparing(t -> t.id));
    }

    /**
     * Returns the number of tasks in the scheduler.
     *
     * @return the number of tasks
     */
    int size() {
        return tasks.size();
    }

    /**
     * Schedules a task to be executed after the specified duration.
     *
     * @param task     the task to schedule
     * @param duration the duration after which the task should be executed
     * @return a Cancellable object that can be used to cancel the scheduled task
     */
    Cancellable schedule(Runnable task, Duration duration) {
        final Task t = new Task(task, clock.nanoTime() + duration.toNanos(), counter++);
        tasks.add(t);
        return t;
    }

    /**
     * Retrieves a list of tasks that have expired (i.e., their scheduled time has passed).
     * The expired tasks are removed from the scheduler.
     *
     * @return a list of expired tasks
     */
    List<Runnable> expired() {
        long time = clock.nanoTime();
        List<Runnable> result = new ArrayList<>();
        Iterator<Task> it = tasks.iterator();
        Task item;
        while (it.hasNext() && (item = it.next()).time <= time) {
            result.add(item.task);
            it.remove();
        }
        return result;
    }

    /**
     * System based clock.
     */
    static class SystemClock implements Clock {

        @Override
        public long nanoTime() {
            return System.nanoTime();
        }
    }

    /**
     * Represents a task scheduled in the scheduler.
     */
    class Task implements Cancellable {
        final Runnable task;
        final long time;
        final long id;

        /**
         * Constructs a new Task with the specified task, scheduled time, and unique identifier.
         *
         * @param task the task to be executed
         * @param time the scheduled time of the task
         * @param id   the unique identifier of the task
         */
        Task(Runnable task, long time, long id) {
            this.task = task;
            this.time = time;
            this.id = id;
        }

        /**
         * Cancels the task by removing it from the scheduler.
         */
        @Override
        public void cancel() {
            tasks.remove(this);
        }
    }
}
