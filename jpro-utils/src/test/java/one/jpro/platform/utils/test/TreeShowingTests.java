package one.jpro.platform.utils.test;

import javafx.beans.property.BooleanProperty;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import one.jpro.jmemorybuddy.JMemoryBuddy;
import one.jpro.platform.utils.TreeShowing;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

/**
 * Tests for the {@link TreeShowing} utility class.
 *
 * @author Florian Kirmaier
 */
public class TreeShowingTests extends ApplicationTest {

    @Test
    public void simpleTest() {
        interact(() -> {
            Parent node = new Group();
            BooleanProperty prop = TreeShowing.treeShowing(node);
            Assertions.assertFalse(prop.get());
        });
    }

    @Test
    public void memoryTest1() {
        interact(() -> JMemoryBuddy.memoryTest(checker -> {
            Parent node = new Group();
            BooleanProperty prop = TreeShowing.treeShowing(node);

            checker.assertCollectable(node);
            checker.assertCollectable(prop);
        }));
    }

    @Test
    public void memoryTest2() {
        interact(() -> JMemoryBuddy.memoryTest(checker -> {
            Parent node = new Group();
            BooleanProperty prop = TreeShowing.treeShowing(node);
            Scene scene = new Scene(node);
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.show();

            scene.setRoot(new Group());

            checker.setAsReferenced(scene);
            checker.setAsReferenced(stage);
            checker.assertCollectable(node);
            checker.assertCollectable(prop);
        }));
    }

    // @Test
    public void memoryTest3() {
        JMemoryBuddy.memoryTest(checker ->
                interact(() -> {
                    Parent node = new Group();
                    BooleanProperty prop = TreeShowing.treeShowing(node);
                    Scene scene = new Scene(node);
                    Stage stage = new Stage();
                    stage.setScene(scene);
                    stage.show();

                    Assertions.assertTrue(prop.get());
                    scene.setRoot(new Group());
                    stage.close();

                    Assertions.assertFalse(prop.get());

                    checker.setAsReferenced(node);
                    checker.assertCollectable(scene);
                    //checker.setAsReferenced(stage);
                    checker.assertCollectable(stage);
                }));
    }
}
