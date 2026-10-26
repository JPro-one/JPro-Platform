package one.jpro.platform.file.picker.impl;

import javafx.scene.control.Label;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static one.jpro.jmemorybuddy.JMemoryBuddy.memoryTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    public void titleAndInitialDirectoryReachTheChooser(@TempDir File tempDir) {
        File other = new File(tempDir, "other");
        assertTrue(other.mkdir());
        inFX(() -> {
            NativeDirectoryOpenPicker picker = new NativeDirectoryOpenPicker(new Label());
            File dir = tempDir;
            picker.setTitle("Pick");
            picker.setInitialDirectory(dir);
            var chooser = picker.createDirectoryChooser();
            assertEquals("Pick", chooser.getTitle());
            assertEquals(dir, chooser.getInitialDirectory());

            picker.setInitialDirectory(other);
            assertEquals(other, chooser.getInitialDirectory());
        });
    }

    @Test
    public void onShowingAndMissingInitialDirectory(@TempDir File tempDir) {
        inFX(() -> {
            NativeDirectoryOpenPicker picker = new NativeDirectoryOpenPicker(new Label());
            picker.setOnShowing(() -> picker.setInitialDirectory(new File(tempDir, "deleted")));
            assertEquals(tempDir, picker.createDirectoryChooser().getInitialDirectory());
        });
    }
}
