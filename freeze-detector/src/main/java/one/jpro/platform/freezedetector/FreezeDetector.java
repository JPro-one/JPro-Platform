package one.jpro.platform.freezedetector;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;

import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class FreezeDetector {

    Thread fxthread;
    Long lastUpdate;

    public FreezeDetector() {
        this(Duration.ofSeconds(1));
    }

    public FreezeDetector(Duration duration, BiConsumer<Thread, Duration> callback) {
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
            while(fxthread.getState() != Thread.State.TERMINATED) {
                long now = System.currentTimeMillis();
                long timeGone = now - lastUpdate;
                try {
                    long toSleep = duration.toMillis() - timeGone;
                    if(toSleep > 0) {
                        Thread.sleep(toSleep);
                    }
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                long timeGone2 = System.currentTimeMillis() - lastUpdate;
                if(timeGone2 > duration.toMillis()) {
                    callback.accept(fxthread, Duration.ofMillis(timeGone2));
                    try {
                        Thread.sleep(duration.toMillis());
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }, "FX-Freeze-Detector-Thread");
        //t.setDaemon(true);
        t.start();
    }

    public FreezeDetector(Duration duration) {
        this(duration, (thread, timeGone) -> {
            System.out.println("Freeze detected for " + timeGone.toMillis() + "ms");
            System.out.println(" Thread: " + thread.getName());
            for (StackTraceElement element : thread.getStackTrace()) {
                System.out.println(" " + element);
            }
        });
    }
}
