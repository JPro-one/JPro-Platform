package one.jpro.platform.file.picker;

import javafx.scene.control.Label;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

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
    public void testInitialDirectoryAndInitialFileName() {
        inFX(() -> {
            Label label = new Label();
            NativeFileOpenPicker picker = new NativeFileOpenPicker(label);
            var file = new File("test");
            var initialFileName = "test.txt";
            picker.setInitialDirectory(file);
            picker.setInitialFileName("test.txt");
            var chooser = picker.createFileChooser();

            Assertions.assertEquals(file, chooser.getInitialDirectory());
            Assertions.assertEquals(initialFileName, chooser.getInitialFileName());
        });
    }
}