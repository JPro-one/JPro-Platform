package one.jpro.platform.flexbox;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.BeforeAll;

import java.util.concurrent.CountDownLatch;

/**
 * Base class for FlexBox tests. Initializes the JavaFX toolkit once.
 */
public abstract class FlexBoxTestBase {

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

    /**
     * Creates a simple test region with fixed preferred size.
     */
    protected static Region createBox(double prefWidth, double prefHeight) {
        Region r = new Region();
        r.setPrefSize(prefWidth, prefHeight);
        r.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        r.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return r;
    }

    /**
     * Force a layout pass on the FlexBox with the given size.
     */
    protected static void layoutAt(FlexBox box, double width, double height) {
        box.resize(width, height);
        box.layout();
    }

    protected static double mainPos(Node node, boolean isRow) {
        return isRow ? node.getLayoutX() : node.getLayoutY();
    }

    protected static double mainSize(Node node, boolean isRow) {
        return isRow ? node.getLayoutBounds().getWidth() : node.getLayoutBounds().getHeight();
    }

    protected static double crossPos(Node node, boolean isRow) {
        return isRow ? node.getLayoutY() : node.getLayoutX();
    }

    protected static double crossSize(Node node, boolean isRow) {
        return isRow ? node.getLayoutBounds().getHeight() : node.getLayoutBounds().getWidth();
    }
}
