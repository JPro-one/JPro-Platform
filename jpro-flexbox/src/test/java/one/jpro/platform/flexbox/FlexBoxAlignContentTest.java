package one.jpro.platform.flexbox;

import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlexBoxAlignContentTest extends FlexBoxTestBase {

    private FlexBox createWrappingBox() {
        FlexBox box = new FlexBox();
        box.setWrap(FlexWrap.WRAP);
        box.setAlignItems(FlexAlignItems.FLEX_START);
        // 3 items, each 150 wide, 40 tall → 2 lines in 350 wide box
        Region a = createBox(150, 40);
        Region b = createBox(150, 40);
        Region c = createBox(150, 40);
        box.getChildren().addAll(a, b, c);
        return box;
    }

    @Test
    void defaultAlignContentIsStretch() {
        FlexBox box = new FlexBox();
        assertEquals(FlexAlignContent.STRETCH, box.getAlignContent());
    }

    @Test
    void stretchDistributesExtraCrossSpace() {
        FlexBox box = createWrappingBox();
        box.setAlignContent(FlexAlignContent.STRETCH);
        layoutAt(box, 350, 200);

        // 2 lines, each pref 40, total = 80, free = 120, each gets 60 extra
        // Line 1 actual = 100, line 2 actual = 100
        Region a = (Region) box.getChildren().get(0);
        Region c = (Region) box.getChildren().get(2);
        assertEquals(0, a.getLayoutY(), 0.5);
        assertEquals(100, c.getLayoutY(), 0.5);
    }

    @Test
    void flexStartPacksToTop() {
        FlexBox box = createWrappingBox();
        box.setAlignContent(FlexAlignContent.FLEX_START);
        layoutAt(box, 350, 200);

        Region a = (Region) box.getChildren().get(0);
        Region c = (Region) box.getChildren().get(2);
        assertEquals(0, a.getLayoutY(), 0.5);
        assertEquals(40, c.getLayoutY(), 0.5);
    }

    @Test
    void flexEndPacksToBottom() {
        FlexBox box = createWrappingBox();
        box.setAlignContent(FlexAlignContent.FLEX_END);
        layoutAt(box, 350, 200);

        // free = 120, lines start at 120
        Region a = (Region) box.getChildren().get(0);
        Region c = (Region) box.getChildren().get(2);
        assertEquals(120, a.getLayoutY(), 0.5);
        assertEquals(160, c.getLayoutY(), 0.5);
    }

    @Test
    void centerCentersLines() {
        FlexBox box = createWrappingBox();
        box.setAlignContent(FlexAlignContent.CENTER);
        layoutAt(box, 350, 200);

        // free = 120, offset = 60
        Region a = (Region) box.getChildren().get(0);
        Region c = (Region) box.getChildren().get(2);
        assertEquals(60, a.getLayoutY(), 0.5);
        assertEquals(100, c.getLayoutY(), 0.5);
    }

    @Test
    void spaceBetweenDistributes() {
        FlexBox box = createWrappingBox();
        box.setAlignContent(FlexAlignContent.SPACE_BETWEEN);
        layoutAt(box, 350, 200);

        // free = 120, gap between 2 lines = 120
        Region a = (Region) box.getChildren().get(0);
        Region c = (Region) box.getChildren().get(2);
        assertEquals(0, a.getLayoutY(), 0.5);
        assertEquals(160, c.getLayoutY(), 0.5);
    }

    @Test
    void spaceAroundDistributes() {
        FlexBox box = createWrappingBox();
        box.setAlignContent(FlexAlignContent.SPACE_AROUND);
        layoutAt(box, 350, 200);

        // free = 120, 2 lines: margin = 120/(2*2)=30, gap = 120/2=60
        Region a = (Region) box.getChildren().get(0);
        Region c = (Region) box.getChildren().get(2);
        assertEquals(30, a.getLayoutY(), 0.5);
        assertEquals(130, c.getLayoutY(), 0.5);
    }

    @Test
    void spaceEvenlyDistributes() {
        FlexBox box = createWrappingBox();
        box.setAlignContent(FlexAlignContent.SPACE_EVENLY);
        layoutAt(box, 350, 200);

        // free = 120, 2 lines: gap = 120/3 = 40
        Region a = (Region) box.getChildren().get(0);
        Region c = (Region) box.getChildren().get(2);
        assertEquals(40, a.getLayoutY(), 0.5);
        assertEquals(120, c.getLayoutY(), 0.5);
    }
}
