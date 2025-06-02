package one.jpro.platform.file.picker;

import javafx.scene.control.Label;
import org.junit.jupiter.api.Test;

import static one.jpro.jmemorybuddy.JMemoryBuddy.memoryTest;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for {@link NativeFileSavePicker} functionality.
 * Tests memory management and filename property configuration.
 */
public class NativeFileSavePickerTest extends NativeFilePickerTest {

    /**
     * Tests memory management of FileChooser created by NativeFileSavePicker.
     * Verifies that FileChooser can be garbage collected while picker remains referenced.
     */
    @Test
    public void testMemoryRef() {
        memoryTest(checker -> inFX(() -> {
            Label label = new Label();
            NativeFileSavePicker picker = new NativeFileSavePicker(label);
            var chooser = picker.createFileChooser();

            checker.setAsReferenced(picker);
            checker.assertCollectable(chooser);
        }));
    }

    /**
     * Tests setting and updating initial filename property.
     * Verifies that filename changes are properly applied to the FileChooser.
     */
    @Test
    public void testInitialFileName() {
        inFX(() -> {
            Label label = new Label();
            NativeFileOpenPicker picker = new NativeFileOpenPicker(label);
            picker.setInitialFileName("test.txt");
            var chooser = picker.createFileChooser();
            assertEquals("test.txt", chooser.getInitialFileName());

            picker.setInitialFileName("test123.txt");
            assertEquals("test123.txt", chooser.getInitialFileName());
        });
    }
}