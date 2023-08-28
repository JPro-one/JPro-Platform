package one.jpro.platform.sessionmanager;

import javafx.application.Platform;
import javafx.collections.ObservableMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestSessionManager {

    @BeforeAll
    public static void startJavaFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(latch::countDown);
        latch.await();
    }

    @Test
    public void testSessionManager() {
        inFX(this::testSessionManagerFX);
    }

    private void testSessionManagerFX() {
        // SessionManager should only be instantiated once per VM (per context)
        SessionManager sm1 = new SessionManager("test");
        ObservableMap<String, String> s1 = sm1.getSession("tester1");
        System.out.println("" + sm1.getFolder());

        s1.put("k1", "v1");
        s1.put("k2", "v2");

        SessionManager sm2 = new SessionManager("test");
        ObservableMap<String, String> s2 = sm2.getSession("tester1");
        ObservableMap<String, String> s3 = sm2.getSession("tester1");

        assertEquals(s2, s3, "Assert that the session are equal");

        assertEquals(s1.get("k1"), "v1");
        assertEquals(s1.get("k2"), "v2");
        s2.put("k1", "vv1");
        s2.remove("k2");
        assertEquals(s1.get("k1"), "v1");
        assertEquals(s1.get("k2"), "v2");
        assertEquals(s3.get("k1"), "vv1");
        assertFalse(s3.containsKey("k2"));

        // Make sure data is loaded correctly from directory
        SessionManager sm4 = new SessionManager("test");
        ObservableMap<String, String> s4 = sm4.getSession("tester1");
        assertEquals(s4.get("k1"), "vv1");
        assertFalse(s4.containsKey("k2"));


        // Make sure other context are not affected
        SessionManager sm5 = new SessionManager("test2");
        ObservableMap<String, String> s5 = sm5.getSession("tester1");
        assertFalse(s5.containsKey("k1"));
        SessionManager sm6 = new SessionManager("test");
        ObservableMap<String, String> s6 = sm6.getSession("tester2");
        assertFalse(s6.containsKey("k1"));
    }

    private void inFX(Runnable r) {
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
