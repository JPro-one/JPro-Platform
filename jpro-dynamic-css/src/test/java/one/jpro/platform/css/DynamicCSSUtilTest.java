package one.jpro.platform.css;

import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DynamicCSSUtilTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
            latch.await();
        } catch (IllegalStateException e) {
            // Toolkit already initialized
        }
    }

    private static void runOnFxThread(Runnable action) throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        latch.await();
        if (error.get() != null) {
            throw new RuntimeException(error.get());
        }
    }

    @Test
    void parentCssStringProperty() throws Exception {
        runOnFxThread(() -> {
            VBox parent = new VBox();
            StringProperty prop = DynamicCSSUtil.cssStringProperty(parent);
            assertEquals("", prop.get());

            prop.set(".label { -fx-font-size: 18; }");
            assertEquals(".label { -fx-font-size: 18; }", DynamicCSSUtil.getCssString(parent));

            DynamicCSSUtil.setCssString(parent, ".label { -fx-font-size: 24; }");
            assertEquals(".label { -fx-font-size: 24; }", prop.get());
        });
    }

    @Test
    void sceneCssStringProperty() throws Exception {
        runOnFxThread(() -> {
            Scene scene = new Scene(new StackPane());
            StringProperty prop = DynamicCSSUtil.cssStringProperty(scene);
            assertEquals("", prop.get());

            prop.set(".root { -fx-background-color: red; }");
            assertEquals(".root { -fx-background-color: red; }", DynamicCSSUtil.getCssString(scene));
        });
    }

    @Test
    void nullClearsValue() throws Exception {
        runOnFxThread(() -> {
            VBox parent = new VBox();
            DynamicCSSUtil.setCssString(parent, ".a {}");
            DynamicCSSUtil.setCssString(parent, null);
            assertEquals("", DynamicCSSUtil.getCssString(parent));
        });
    }

    @Test
    void multipleParentsIndependent() throws Exception {
        runOnFxThread(() -> {
            VBox a = new VBox();
            VBox b = new VBox();
            DynamicCSSUtil.setCssString(a, ".a {}");
            DynamicCSSUtil.setCssString(b, ".b {}");
            assertEquals(".a {}", DynamicCSSUtil.getCssString(a));
            assertEquals(".b {}", DynamicCSSUtil.getCssString(b));
        });
    }
}
