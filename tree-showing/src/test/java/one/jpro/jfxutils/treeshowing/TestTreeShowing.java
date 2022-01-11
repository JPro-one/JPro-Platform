package one.jpro.jfxutils.treeshowing;

import de.sandec.jmemorybuddy.JMemoryBuddy;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import one.jpro.jfxutils.treeshowing.TreeShowing;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class TestTreeShowing {


    @BeforeAll
    public static void startJavaFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(() -> {
            latch.countDown();
        });

        latch.await();
    }

    @Test
    public void simpleTest() { inFX(() -> {
        Parent node = new Group();
        BooleanProperty prop = TreeShowing.treeShowing(node);
        Assertions.assertFalse(prop.get());

        JMemoryBuddy.memoryTest(checker -> {

        });
    });}

    @Test
    public void memoryTest1() { inFX(() -> {
        JMemoryBuddy.memoryTest(checker -> {
            Parent node = new Group();
            BooleanProperty prop = TreeShowing.treeShowing(node);

            checker.assertCollectable(node);
            checker.assertCollectable(prop);
        });
    });}

    @Test
    public void memoryTest2() { inFX(() -> {
        JMemoryBuddy.memoryTest(checker -> {
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
        });
    });}


    @Test
    public void memoryTest3() {
        JMemoryBuddy.memoryTest(checker -> {
            inFX(() -> {
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
            });
        });
    }

    public void inFX(Runnable r) {
        CountDownLatch l = new CountDownLatch(1);
        AtomicReference<Throwable> ex = new AtomicReference();
        Platform.runLater(() -> {
            try {
                r.run();
            } catch (Throwable e) {
                ex.set(e);
            } finally {
                l.countDown();
            }
        });
        try {
            l.await();
            if(ex.get() != null) {
                throw new RuntimeException(ex.get());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
