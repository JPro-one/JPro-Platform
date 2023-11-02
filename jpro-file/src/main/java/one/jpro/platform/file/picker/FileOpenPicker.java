package one.jpro.platform.file.picker;

import com.jpro.webapi.WebAPI;
import javafx.scene.Node;
import one.jpro.platform.file.MultipleFileSelector;

import java.util.Objects;

/**
 * {@link FilePicker} interface extension for file open operations.
 *
 * @author Besmir Beqiri
 */
public interface FileOpenPicker extends FilePicker, MultipleFileSelector {

    /**
     * Creates a file picker. If the application is running in a
     * browser via JPro server, then a web version of the file
     * picker is returned. If the application is not running inside
     * the browser than a desktop version is returned.
     *
     * @param node the associated node for this file picker
     * @return a {@link FileOpenPicker} object.
     * @throws NullPointerException if the node is null
     */
    static FileOpenPicker create(Node node) {
        Objects.requireNonNull(node, "node must not be null");
        if (WebAPI.isBrowser()) {
            return new WebFileOpenPicker(node);
        }
        return new NativeFileOpenPicker(node);
    }
}
