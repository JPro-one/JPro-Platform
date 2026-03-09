package one.jpro.platform.flexbox;

import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlexBoxJustifyTest extends FlexBoxTestBase {

    @Test
    void flexStartIsDefault() {
        FlexBox box = new FlexBox();
        assertEquals(FlexJustifyContent.FLEX_START, box.getJustifyContent());
    }

    @Test
    void flexStartAlignsToBegining() {
        FlexBox box = new FlexBox();
        box.setJustifyContent(FlexJustifyContent.FLEX_START);
        Region a = createBox(50, 30);
        Region b = createBox(50, 30);
        box.getChildren().addAll(a, b);

        layoutAt(box, 400, 200);

        assertEquals(0, a.getLayoutX(), 0.5);
        assertEquals(50, b.getLayoutX(), 0.5);
    }

    @Test
    void flexEndAlignsToEnd() {
        FlexBox box = new FlexBox();
        box.setJustifyContent(FlexJustifyContent.FLEX_END);
        Region a = createBox(50, 30);
        Region b = createBox(50, 30);
        box.getChildren().addAll(a, b);

        layoutAt(box, 400, 200);

        // total children = 100, remaining = 300
        assertEquals(300, a.getLayoutX(), 0.5);
        assertEquals(350, b.getLayoutX(), 0.5);
    }

    @Test
    void centerCentersChildren() {
        FlexBox box = new FlexBox();
        box.setJustifyContent(FlexJustifyContent.CENTER);
        Region a = createBox(50, 30);
        Region b = createBox(50, 30);
        box.getChildren().addAll(a, b);

        layoutAt(box, 400, 200);

        // total children = 100, remaining = 300, offset = 150
        assertEquals(150, a.getLayoutX(), 0.5);
        assertEquals(200, b.getLayoutX(), 0.5);
    }

    @Test
    void spaceBetweenDistributes() {
        FlexBox box = new FlexBox();
        box.setJustifyContent(FlexJustifyContent.SPACE_BETWEEN);
        Region a = createBox(50, 30);
        Region b = createBox(50, 30);
        Region c = createBox(50, 30);
        box.getChildren().addAll(a, b, c);

        layoutAt(box, 400, 200);

        // total children = 150, remaining = 250, gap = 125
        assertEquals(0, a.getLayoutX(), 0.5);
        assertEquals(175, b.getLayoutX(), 0.5);
        assertEquals(350, c.getLayoutX(), 0.5);
    }

    @Test
    void spaceAroundDistributes() {
        FlexBox box = new FlexBox();
        box.setJustifyContent(FlexJustifyContent.SPACE_AROUND);
        Region a = createBox(50, 30);
        Region b = createBox(50, 30);
        box.getChildren().addAll(a, b);

        layoutAt(box, 400, 200);

        // total children = 100, remaining = 300
        // space-around: each child gets remaining/count/2 margin on each side = 75
        // a at 75, b at 75+50+150 = 275
        assertEquals(75, a.getLayoutX(), 0.5);
        assertEquals(275, b.getLayoutX(), 0.5);
    }

    @Test
    void spaceEvenlyDistributes() {
        FlexBox box = new FlexBox();
        box.setJustifyContent(FlexJustifyContent.SPACE_EVENLY);
        Region a = createBox(50, 30);
        Region b = createBox(50, 30);
        box.getChildren().addAll(a, b);

        layoutAt(box, 400, 200);

        // total children = 100, remaining = 300
        // space-evenly: gap = remaining/(count+1) = 100
        // a at 100, b at 100+50+100 = 250
        assertEquals(100, a.getLayoutX(), 0.5);
        assertEquals(250, b.getLayoutX(), 0.5);
    }
}
