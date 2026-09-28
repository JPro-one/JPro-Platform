package one.jpro.platform.file.picker;

import com.jpro.webapi.WebAPI;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import one.jpro.platform.file.picker.impl.NativeDirectoryOpenPicker;

import java.io.File;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Lets the user choose a directory when the associated node is clicked. Desktop only:
 * browsers can't hand a directory to the server, so {@link #create(Node)} throws in JPro.
 *
 * @author Florian Kirmaier
 */
public interface DirectoryOpenPicker {

    /**
     * Creates a directory picker for the given node.
     *
     * @param node the node that opens the chooser when clicked
     * @return the picker
     * @throws UnsupportedOperationException when running in the browser
     */
    static DirectoryOpenPicker create(Node node) {
        Objects.requireNonNull(node, "node must not be null");
        if (WebAPI.isBrowser()) {
            throw new UnsupportedOperationException("Choosing a directory is not supported in the browser");
        }
        return new NativeDirectoryOpenPicker(node);
    }

    /**
     * The node that opens the chooser when clicked.
     */
    Node getNode();

    String getTitle();

    void setTitle(String value);

    /**
     * The title of the directory chooser dialog.
     */
    StringProperty titleProperty();

    File getInitialDirectory();

    void setInitialDirectory(File value);

    /**
     * The directory the chooser opens in.
     */
    ObjectProperty<File> initialDirectoryProperty();

    Consumer<File> getOnDirectorySelected();

    void setOnDirectorySelected(Consumer<File> value);

    /**
     * Called with the chosen directory.
     */
    ObjectProperty<Consumer<File>> onDirectorySelectedProperty();
}
