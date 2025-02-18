package one.jpro.platform.file.picker;

import javafx.application.Platform;
import javafx.scene.control.Label;
import one.jpro.jmemorybuddy.JMemoryBuddy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class NativeFileOpenPickerTest {

    @BeforeAll
    public static void startJavaFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(latch::countDown);
        latch.await();
    }

    @Test
    public void test() {
        JMemoryBuddy.memoryTest(checker -> {
            inFX(() -> {
                Label label = new Label();
                NativeFileOpenPicker picker = new NativeFileOpenPicker(label);
                var chooser = picker.createFileChooser();

                checker.setAsReferenced(picker);
                checker.assertCollectable(chooser);
            });
        });
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
