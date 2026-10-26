package one.jpro.platform.file.picker.impl;

import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import one.jpro.platform.file.ExtensionFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;

import static one.jpro.jmemorybuddy.JMemoryBuddy.memoryTest;

/**
 * Test class for {@link NativeFileOpenPicker} functionality.
 * Tests memory management and property configuration of file open picker components.
 */
public class NativeFileOpenPickerTest extends NativeFilePickerTest {

    /**
     * Tests memory management of FileChooser created by NativeFileOpenPicker.
     * Verifies that FileChooser can be garbage collected while picker remains referenced.
     */
    @Test
    public void test() {
        memoryTest(checker -> inFX(() -> {
            Label label = new Label();
            NativeFileOpenPicker picker = new NativeFileOpenPicker(label);
            var chooser = picker.createFileChooser();

            checker.setAsReferenced(picker);
            checker.assertCollectable(chooser);
        }));
    }

    /**
     * Tests setting and retrieving initial directory and filename properties.
     * Verifies that configured properties are properly applied to the FileChooser.
     */
    @Test
    public void testInitialDirectoryAndInitialFileName(@TempDir File tempDir) {
        inFX(() -> {
            Label label = new Label();
            NativeFileOpenPicker picker = new NativeFileOpenPicker(label);
            var file = tempDir;
            var initialFileName = "test.txt";
            picker.setInitialDirectory(file);
            picker.setInitialFileName("test.txt");
            var chooser = picker.createFileChooser();

            Assertions.assertEquals(file, chooser.getInitialDirectory());
            Assertions.assertEquals(initialFileName, chooser.getInitialFileName());
        });
    }

    @Test
    public void missingInitialDirectoryFallsBackToExistingParent(@TempDir File tempDir) {
        inFX(() -> {
            NativeFileOpenPicker picker = new NativeFileOpenPicker(new Label());
            File missing = new File(tempDir, "deleted/sub");
            picker.setInitialDirectory(missing);
            Assertions.assertEquals(tempDir, picker.createFileChooser().getInitialDirectory());
            Assertions.assertEquals(missing, picker.getInitialDirectory());
        });
    }

    @Test
    public void onShowingRunsBeforeTheChooserIsCreated(@TempDir File tempDir) {
        inFX(() -> {
            NativeFileOpenPicker picker = new NativeFileOpenPicker(new Label());
            picker.setOnShowing(() -> {
                picker.setInitialDirectory(tempDir);
                picker.getExtensionFilters().setAll(ExtensionFilter.of("Text", ".txt"));
            });
            var chooser = picker.createFileChooser();
            Assertions.assertEquals(tempDir, chooser.getInitialDirectory());
            Assertions.assertEquals(List.of("Text"),
                    chooser.getExtensionFilters().stream().map(FileChooser.ExtensionFilter::getDescription).toList());
        });
    }
}
