package one.jpro.platform.flexbox;

import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlexBoxGrowTest extends FlexBoxTestBase {

    @Test
    void singleChildGrowFillsSpace() {
        FlexBox box = new FlexBox();
        Region a = createBox(50, 30);
        FlexBox.setGrow(a, 1);
        box.getChildren().add(a);

        layoutAt(box, 400, 200);

        assertEquals(400, a.getLayoutBounds().getWidth(), 0.5);
    }

    @Test
    void equalGrowSplitsEvenly() {
        FlexBox box = new FlexBox();
        Region a = createBox(0, 30);
        Region b = createBox(0, 30);
        FlexBox.setGrow(a, 1);
        FlexBox.setGrow(b, 1);
        FlexBox.setBasis(a, 0);
        FlexBox.setBasis(b, 0);
        box.getChildren().addAll(a, b);

        layoutAt(box, 400, 200);

        assertEquals(200, a.getLayoutBounds().getWidth(), 0.5);
        assertEquals(200, b.getLayoutBounds().getWidth(), 0.5);
    }

    @Test
    void unequalGrowDistributesProportionally() {
        FlexBox box = new FlexBox();
        Region a = createBox(0, 30);
        Region b = createBox(0, 30);
        FlexBox.setGrow(a, 1);
        FlexBox.setGrow(b, 3);
        FlexBox.setBasis(a, 0);
        FlexBox.setBasis(b, 0);
        box.getChildren().addAll(a, b);

        layoutAt(box, 400, 200);

        assertEquals(100, a.getLayoutBounds().getWidth(), 0.5);
        assertEquals(300, b.getLayoutBounds().getWidth(), 0.5);
    }

    @Test
    void growWithBasis() {
        FlexBox box = new FlexBox();
        Region a = createBox(100, 30);
        Region b = createBox(100, 30);
        FlexBox.setGrow(a, 1);
        FlexBox.setGrow(b, 1);
        FlexBox.setBasis(a, 100);
        FlexBox.setBasis(b, 100);
        box.getChildren().addAll(a, b);

        layoutAt(box, 400, 200);

        // 400 total, 200 basis, 200 free space split equally
        assertEquals(200, a.getLayoutBounds().getWidth(), 0.5);
        assertEquals(200, b.getLayoutBounds().getWidth(), 0.5);
    }

    @Test
    void noGrowChildrenKeepBasis() {
        FlexBox box = new FlexBox();
        Region a = createBox(80, 30);
        Region b = createBox(120, 30);
        box.getChildren().addAll(a, b);

        layoutAt(box, 400, 200);

        assertEquals(80, a.getLayoutBounds().getWidth(), 0.5);
        assertEquals(120, b.getLayoutBounds().getWidth(), 0.5);
    }

    @Test
    void growInColumnDirection() {
        FlexBox box = new FlexBox();
        box.setDirection(FlexDirection.COLUMN);
        Region a = createBox(50, 0);
        Region b = createBox(50, 0);
        FlexBox.setGrow(a, 1);
        FlexBox.setGrow(b, 1);
        FlexBox.setBasis(a, 0);
        FlexBox.setBasis(b, 0);
        box.getChildren().addAll(a, b);

        layoutAt(box, 200, 400);

        assertEquals(200, a.getLayoutBounds().getHeight(), 0.5);
        assertEquals(200, b.getLayoutBounds().getHeight(), 0.5);
    }
}
