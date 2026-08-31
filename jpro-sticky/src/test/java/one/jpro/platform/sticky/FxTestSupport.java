package one.jpro.platform.sticky;

import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Shared plumbing for the headless JavaFX unit tests of the desktop-parity implementations (the
 * {@code one.jpro.platform.sticky.impl} classes). No TestFX, no window: the toolkit is bootstrapped
 * once with {@link Platform#startup} and work runs on the JavaFX thread, with layout forced
 * synchronously via {@code root.applyCss()} + {@code root.layout()}.
 *
 * @author Tobias Horak
 */
public final class FxTestSupport {

    private FxTestSupport() {
        // test utility
    }

    /** Boots the JavaFX toolkit once; a no-op if it is already running. */
    public static void startToolkit() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
            latch.await();
        } catch (IllegalStateException alreadyStarted) {
            // Toolkit already initialized by an earlier test class.
        }
    }

    /**
     * Runs {@code action} on the JavaFX application thread and blocks until it completes, propagating
     * any {@link Throwable} it raises (so assertion failures surface as test failures) rather than
     * letting it vanish on the FX thread.
     */
    public static void onFx(FxAction action) {
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                done.countDown();
            }
        });
        try {
            if (!done.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("FX action did not complete within 10s");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted waiting for FX action", e);
        }
        Throwable t = error.get();
        if (t instanceof AssertionError) {
            throw (AssertionError) t;
        }
        if (t != null) {
            throw new AssertionError("FX action threw", t);
        }
    }

    /** An FX-thread action that may throw a checked exception or an assertion failure. */
    @FunctionalInterface
    public interface FxAction {
        void run() throws Exception;
    }
}
