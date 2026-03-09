package one.jpro.platform.flexbox;

import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlexBoxShrinkTest extends FlexBoxTestBase {

    @Test
    void defaultShrinkIsOne() {
        Region r = createBox(100, 30);
        assertEquals(1, FlexBox.getShrink(r), 0.01);
    }

    @Test
    void equalShrinkDistributesEvenly() {
        FlexBox box = new FlexBox();
        Region a = createBox(200, 30);
        Region b = createBox(200, 30);
        a.setMinWidth(0);
        b.setMinWidth(0);
        FlexBox.setBasis(a, 200);
        FlexBox.setBasis(b, 200);
        box.getChildren().addAll(a, b);

        layoutAt(box, 300, 200);

        // Total basis = 400, available = 300, deficit = -100
        // Equal shrink: each loses 50
        assertEquals(150, a.getLayoutBounds().getWidth(), 0.5);
        assertEquals(150, b.getLayoutBounds().getWidth(), 0.5);
    }

    @Test
    void unequalShrinkDistributesProportionally() {
        FlexBox box = new FlexBox();
        Region a = createBox(200, 30);
        Region b = createBox(200, 30);
        a.setMinWidth(0);
        b.setMinWidth(0);
        FlexBox.setBasis(a, 200);
        FlexBox.setBasis(b, 200);
        FlexBox.setShrink(a, 1);
        FlexBox.setShrink(b, 3);
        box.getChildren().addAll(a, b);

        layoutAt(box, 300, 200);

        // deficit = -100, scaled shrink: a=1*200=200, b=3*200=600, total=800
        // a shrinks by 100*(200/800) = 25 → 175
        // b shrinks by 100*(600/800) = 75 → 125
        assertEquals(175, a.getLayoutBounds().getWidth(), 0.5);
        assertEquals(125, b.getLayoutBounds().getWidth(), 0.5);
    }

    @Test
    void shrinkZeroDoesNotShrink() {
        FlexBox box = new FlexBox();
        Region a = createBox(200, 30);
        Region b = createBox(200, 30);
        a.setMinWidth(0);
        b.setMinWidth(0);
        FlexBox.setBasis(a, 200);
        FlexBox.setBasis(b, 200);
        FlexBox.setShrink(a, 0);
        FlexBox.setShrink(b, 1);
        box.getChildren().addAll(a, b);

        layoutAt(box, 300, 200);

        // a doesn't shrink → stays at 200, b takes all deficit → 100
        assertEquals(200, a.getLayoutBounds().getWidth(), 0.5);
        assertEquals(100, b.getLayoutBounds().getWidth(), 0.5);
    }

    @Test
    void shrinkRespectsMinWidth() {
        FlexBox box = new FlexBox();
        Region a = createBox(200, 30);
        Region b = createBox(200, 30);
        a.setMinWidth(180);
        b.setMinWidth(0);
        FlexBox.setBasis(a, 200);
        FlexBox.setBasis(b, 200);
        box.getChildren().addAll(a, b);

        layoutAt(box, 300, 200);

        // deficit = -100, equal shrink. a would go to 150 but min is 180
        // a freezes at 180 (only shrunk 20), remaining deficit = -80 for b
        // b: 200 - 80 = 120
        assertEquals(180, a.getLayoutBounds().getWidth(), 0.5);
        assertEquals(120, b.getLayoutBounds().getWidth(), 0.5);
    }

    @Test
    void shrinkInColumnDirection() {
        FlexBox box = new FlexBox();
        box.setDirection(FlexDirection.COLUMN);
        Region a = createBox(50, 200);
        Region b = createBox(50, 200);
        a.setMinHeight(0);
        b.setMinHeight(0);
        FlexBox.setBasis(a, 200);
        FlexBox.setBasis(b, 200);
        box.getChildren().addAll(a, b);

        layoutAt(box, 200, 300);

        assertEquals(150, a.getLayoutBounds().getHeight(), 0.5);
        assertEquals(150, b.getLayoutBounds().getHeight(), 0.5);
    }
}
