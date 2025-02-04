package one.jpro.platform.file.picker;

import com.jpro.webapi.WebAPI;
import javafx.scene.Node;
import one.jpro.platform.file.MultipleFileSelector;

import java.util.Objects;

/**
 * {@link FilePicker} interface extension for file open operations.
 *
 * @author Florian Kirmaier
 */
public interface DirectoryOpenPicker extends FileOpenPicker {

    /**
     * Creates a file picker.
     * It only works in desktop applications.
     *
     * @param node the associated node for this file picker
     * @return a {@link DirectoryOpenPicker} object.
     * @throws NullPointerException if the node is null
     */
    static DirectoryOpenPicker create(Node node) {
        Objects.requireNonNull(node, "node must not be null");
        if (WebAPI.isBrowser()) {
            throw new UnsupportedOperationException("DirectoryOpenPicker is not supported in the browser");
        }
        return new NativeDirectoryOpenPicker(node);
    }
}
