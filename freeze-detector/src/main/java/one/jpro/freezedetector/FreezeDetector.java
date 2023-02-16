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

        var t = new Thread(() -> {
            while(true && fxthread.getState() != Thread.State.TERMINATED) {
                long now = System.currentTimeMillis();
                long timeGone = now - lastUpdate;
                try {
                    long toSleep = duration.toMillis() - timeGone;
                    if(toSleep > 0) {
                        Thread.sleep(toSleep);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(System.currentTimeMillis() - lastUpdate > duration.toMillis()) {
                    callback.accept(fxthread);
                    lastUpdate = System.currentTimeMillis();
                    try {
                        Thread.sleep(duration.toMillis());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, "FX-Freeze-Detector-Thread");
        //t.setDaemon(true);
        t.start();
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
