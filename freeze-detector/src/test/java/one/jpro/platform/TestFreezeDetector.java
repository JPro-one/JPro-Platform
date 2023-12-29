package one.jpro.platform;

import javafx.application.Platform;
import one.jpro.platform.freezedetector.FreezeDetector;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFreezeDetector {

    @BeforeAll
    public static void startJavaFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(latch::countDown);
        latch.await();
    }

    @Test
    public void testFreezeDetector() throws InterruptedException {
        // THIS IS UNSTABLE WITH MAC AND JAVAFX < 21
        AtomicInteger counter = new AtomicInteger(0);
        inFX(() -> new FreezeDetector(Duration.ofMillis(100),
                (thread,duration) -> {
            counter.incrementAndGet();
        }));

        assertEquals(0, counter.get());
        Thread.sleep(200);
        assertEquals(0, counter.get());
        inFX(() -> {
            try {
                Thread.sleep(150);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        });
        assertEquals(1, counter.get());
        Thread.sleep(200);
        assertEquals(1, counter.get());
        inFX(() -> {
            try {
                Thread.sleep(150);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        });
        assertEquals(2, counter.get());
    }

    public static void inFX(Runnable r) {
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
            if(ex.get() != null) {
                throw new RuntimeException(ex.get());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
