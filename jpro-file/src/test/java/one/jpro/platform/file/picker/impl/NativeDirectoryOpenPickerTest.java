package one.jpro.platform.file.picker.impl;

import javafx.scene.control.Label;
import org.junit.jupiter.api.Test;

import java.io.File;

import static one.jpro.jmemorybuddy.JMemoryBuddy.memoryTest;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NativeDirectoryOpenPickerTest extends NativeFilePickerTest {

    @Test
    public void chooserIsCollectableWhilePickerLives() {
        memoryTest(checker -> inFX(() -> {
            NativeDirectoryOpenPicker picker = new NativeDirectoryOpenPicker(new Label());
            var chooser = picker.createDirectoryChooser();
            checker.setAsReferenced(picker);
            checker.assertCollectable(chooser);
        }));
    }

    @Test
    public void titleAndInitialDirectoryReachTheChooser() {
        inFX(() -> {
            NativeDirectoryOpenPicker picker = new NativeDirectoryOpenPicker(new Label());
            File dir = new File("test");
            picker.setTitle("Pick");
            picker.setInitialDirectory(dir);
            var chooser = picker.createDirectoryChooser();
            assertEquals("Pick", chooser.getTitle());
            assertEquals(dir, chooser.getInitialDirectory());

            picker.setInitialDirectory(new File("other"));
            assertEquals(new File("other"), chooser.getInitialDirectory());
        });
    }
}
