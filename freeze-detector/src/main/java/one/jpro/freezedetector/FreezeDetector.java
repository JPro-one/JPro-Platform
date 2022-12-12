package one.jpro.freezedetector;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;

import java.time.Duration;
import java.util.function.Consumer;

public class FreezeDetector {

    Thread fxthread;
    Long lastUpdate;

    public FreezeDetector() {
        this(Duration.ofSeconds(1));
    }

    public FreezeDetector(Duration duration, Consumer<Thread> callback) {
        if(!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("Can run only on the FX thread");
        }
        fxthread = Thread.currentThread();

        lastUpdate = System.currentTimeMillis();
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                lastUpdate = System.currentTimeMillis();
            }
        }.start();

        new Thread(() -> {
            while(true) {
                long now = System.currentTimeMillis();
                long timeGone = now - lastUpdate;
                try {
                    Thread.sleep(duration.toMillis() - timeGone);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(System.currentTimeMillis() - lastUpdate > duration.toMillis()) {
                    callback.accept(fxthread);
                    lastUpdate = System.currentTimeMillis();
                }
            }
        }, "FX-Freeze-Detector-Thread").start();
    }

    public FreezeDetector(Duration duration) {
        this(duration, (thread) -> {
            System.out.println("Freeze detected");
            System.out.println(" Thread: " + thread.getName());
            for (StackTraceElement element : thread.getStackTrace()) {
                System.out.println(" " + element);
            }
        });
    }
}