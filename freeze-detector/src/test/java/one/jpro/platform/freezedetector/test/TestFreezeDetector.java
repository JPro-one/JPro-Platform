package one.jpro.platform.freezedetector.test;

import one.jpro.platform.freezedetector.FreezeDetector;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the freeze detector.
 *
 * @author Besmir Beqiri
 */
public class TestFreezeDetector extends ApplicationTest {

    @Test
    public void testFreezeDetector() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        interact(() -> new FreezeDetector(Duration.ofMillis(100),
                (thread, duration) -> counter.incrementAndGet()));

        assertThat(counter.get()).isEqualTo(0);
        Thread.sleep(200);
        assertThat(counter.get()).isEqualTo(0);
        interact(() -> {
            try {
                Thread.sleep(150);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        });
        assertThat(counter.get()).isEqualTo(1);
        Thread.sleep(200);
        assertThat(counter.get()).isEqualTo(1);
        interact(() -> {
            try {
                Thread.sleep(150);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        });
        assertThat(counter.get()).isEqualTo(2);
    }
}
