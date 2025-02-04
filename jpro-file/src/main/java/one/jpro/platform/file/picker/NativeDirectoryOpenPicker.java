package one.jpro.platform.file.picker;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import one.jpro.platform.file.FileSource;
import one.jpro.platform.file.NativeFileSource;
import one.jpro.platform.file.util.NodeUtils;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a {@link DirectoryOpenPicker} implementation for JavaFX desktop/mobile
 * applications. This class specializes for selecting and opening directories from
 * the native file system.
 *
 * @author Florian Kirmaier
 */
public class NativeDirectoryOpenPicker extends BaseDirectoryOpenPicker {

    private final DirectoryChooser fileChooser = new DirectoryChooser();
    private List<NativeFileSource> nativeFileSources = List.of();


    /**
     * Initializes a new instance associated with the specified node.
     *
     * @param node The node associated with this file picker.
     */
    public NativeDirectoryOpenPicker(Node node) {
        super(node);

        // Define the action that should be performed when the user clicks on the node.
        NodeUtils.addEventHandler(node, MouseEvent.MOUSE_CLICKED, actionEvent -> {
            Window window = node.getScene().getWindow();
            final File file = fileChooser.showDialog(window);
            if (file != null) {
                // Create a list of native file sources from the selected file.
                nativeFileSources = List.of(new NativeFileSource(file));

                // Invoke the onFilesSelected consumer.
                Consumer<List<? extends FileSource>> onFilesSelectedConsumer = getOnFilesSelected();
                if (onFilesSelectedConsumer != null) {
                    onFilesSelectedConsumer.accept(nativeFileSources);
                }
            }
        });
    }

    @Override
    public final String getTitle() {
        return fileChooser.getTitle();
    }

    @Override
    public final void setTitle(final String value) {
        fileChooser.setTitle(value);
    }

    @Override
    public final StringProperty titleProperty() {
        return fileChooser.titleProperty();
    }

    @Override
    public final StringProperty initialFileNameProperty() {
        if (initialFileName == null) {
            initialFileName = new SimpleStringProperty(this, "initialFileName");
        }
        return initialFileName;
    }

    @Override
    public final File getInitialDirectory() {
        return fileChooser.getInitialDirectory();
    }

    @Override
    public final void setInitialDirectory(final File value) {
        fileChooser.setInitialDirectory(value);
    }

    @Override
    public final ObjectProperty<File> initialDirectoryProperty() {
        return fileChooser.initialDirectoryProperty();
    }

    @Override
    public final ObjectProperty<SelectionMode> selectionModeProperty() {
        if (selectionMode == null) {
            selectionMode = new SimpleObjectProperty<>(this, "selectionMode", SelectionMode.SINGLE);
        }
        return selectionMode;
    }
}
