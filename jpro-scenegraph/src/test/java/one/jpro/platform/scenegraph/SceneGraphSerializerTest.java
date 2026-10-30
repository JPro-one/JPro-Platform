package one.jpro.platform.scenegraph;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SceneGraphSerializerTest {

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

    static class Toolbar extends HBox {
    }

    @Test
    void namedSubclassShowsItsLayoutClass() {
        var toolbar = new Toolbar();
        toolbar.setId("main");
        assertEquals("Toolbar(HBox) [id=main]\n", SceneGraphSerializer.serialize(toolbar));
    }

    @Test
    void anonymousSubclassShowsItsNamedSuperclass() {
        var root = new StackPane(new VBox() {}, new Toolbar() {}, new Label("Hi") {});
        assertEquals("""
                StackPane
                - VBox
                - Toolbar(HBox)
                - Label [label] "Hi"
                """, SceneGraphSerializer.serialize(root));
    }

    @Test
    void labeledShowsItsTextOnOneLine() {
        var root = new VBox(new Label("Two\nlines"), new Button("Save"), new Label());
        assertEquals("""
                VBox
                - Label [label] "Two\\nlines"
                - Button [button] "Save"
                - Label [label]
                """, SceneGraphSerializer.serialize(root));
    }
}
