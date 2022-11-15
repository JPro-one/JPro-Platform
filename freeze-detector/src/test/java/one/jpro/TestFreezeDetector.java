package one.jpro;

import one.jpro.freezedetector.FreezeDetector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;

public class TestFreezeDetector {

    @BeforeAll
    public static void startJavaFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(() -> {
            latch.countDown();
        });

        latch.await();
    }

    @Test
    public void testFreezeDetector() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        inFX(() -> {
            new FreezeDetector(Duration.ofMillis(100), thread -> counter.incrementAndGet());
        });

        Assertions.assertTrue(counter.get() == 0);
        Thread.sleep(200);
        Assertions.assertTrue(counter.get() == 0);
        inFX(() -> {
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        Assertions.assertTrue(counter.get() == 1);
        Thread.sleep(200);
        Assertions.assertTrue(counter.get() == 1);
        inFX(() -> {
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        Assertions.assertTrue(counter.get() == 2);
    }



    public void inFX(Runnable r) {
        CountDownLatch l = new CountDownLatch(1);
        AtomicReference<Throwable> ex = new AtomicReference();
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
