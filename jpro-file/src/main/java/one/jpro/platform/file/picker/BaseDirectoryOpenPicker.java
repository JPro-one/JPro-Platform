package one.jpro.platform.file.picker;

import javafx.scene.Node;

/**
 * This is an abstract class that implements the {@link DirectoryOpenPicker} interface.
 * It provides a base implementation for common functionality used by native implementation.
 *
 * @author Florian Kirmaier
 */
abstract class BaseDirectoryOpenPicker extends BaseFileOpenPicker implements DirectoryOpenPicker {
    /**
     * {@inheritDoc}
     */
    BaseDirectoryOpenPicker(Node node) {
        super(node);
    }
}
