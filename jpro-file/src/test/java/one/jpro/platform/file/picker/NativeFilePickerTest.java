package one.jpro.platform.file.picker;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract base class for testing native file picker functionality with JavaFX integration.
 * Provides utilities for JavaFX initialization and thread-safe test execution.
 *
 * @author Besmir Beqiri
 */
public abstract class NativeFilePickerTest {

    /**
     * Initializes the JavaFX Platform before running tests.
     * Blocks until JavaFX startup is complete.
     *
     * @throws InterruptedException if interrupted while waiting for JavaFX startup
     */
    @BeforeAll
    public static void startJavaFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(latch::countDown);
        latch.await();
    }

    /**
     * Executes a Runnable on the JavaFX Application Thread and waits for completion.
     * Propagates any exceptions back to the calling thread.
     *
     * @param r the Runnable to execute on the JavaFX thread
     * @throws RuntimeException if execution fails or thread is interrupted
     */
    void inFX(Runnable r) {
        CountDownLatch l = new CountDownLatch(1);
        AtomicReference<Throwable> ex = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                r.run();
            } catch (Throwable e) {
                ex.set(e);
            } finally {
                l.countDown();
            }
        });
        try {
            l.await();
            if (ex.get() != null) {
                throw new RuntimeException(ex.get());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}