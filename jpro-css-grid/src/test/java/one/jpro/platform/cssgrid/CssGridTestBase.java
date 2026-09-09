package one.jpro.platform.cssgrid;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.BeforeAll;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Base class for CssGrid tests. Initializes the JavaFX toolkit once.
 */
public abstract class CssGridTestBase {

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

    /** A region whose min size equals its pref size and whose max size is unbounded. */
    protected static Region createBox(double prefWidth, double prefHeight) {
        Region r = new Region();
        r.setPrefSize(prefWidth, prefHeight);
        r.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        r.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return r;
    }

    /** A region with explicit min and pref sizes and an unbounded max size. */
    protected static Region createBox(double minWidth, double prefWidth, double minHeight, double prefHeight) {
        Region r = new Region();
        r.setMinSize(minWidth, minHeight);
        r.setPrefSize(prefWidth, prefHeight);
        r.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return r;
    }

    protected static void layoutAt(CssGrid grid, double width, double height) {
        grid.resize(width, height);
        grid.layout();
    }

    protected static void assertBounds(Node node, double x, double y, double w, double h) {
        assertEquals(x, node.getLayoutX(), 0.5, "x of " + node);
        assertEquals(y, node.getLayoutY(), 0.5, "y of " + node);
        assertEquals(w, node.getLayoutBounds().getWidth(), 0.5, "width of " + node);
        assertEquals(h, node.getLayoutBounds().getHeight(), 0.5, "height of " + node);
    }

    protected static double width(Node node) {
        return node.getLayoutBounds().getWidth();
    }

    protected static double height(Node node) {
        return node.getLayoutBounds().getHeight();
    }
}
