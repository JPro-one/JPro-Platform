package one.jpro.platform.file;

import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

final class FxTestSupport {

    private FxTestSupport() {}

    static void startJavaFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException ex) {
            Platform.runLater(latch::countDown);
        } finally {
            latch.await();
        }
    }

    static void inFX(Runnable r) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                r.run();
            } catch (Throwable e) {
                error.set(e);
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (error.get() != null) throw new RuntimeException(error.get());
    }
}
