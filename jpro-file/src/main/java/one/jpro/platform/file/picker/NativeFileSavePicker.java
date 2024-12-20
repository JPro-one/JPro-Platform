package one.jpro.platform.file.picker;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import one.jpro.platform.file.ExtensionFilter;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Represents a {@link FileSavePicker} implementation for JavaFX desktop/mobile
 * applications. This class specializes for selecting a file to save in the
 * native file system.
 *
 * @author Besmir Beqiri
 */
public class NativeFileSavePicker extends BaseFileSavePicker {

    private final FileChooser fileChooser = new FileChooser();
    private final ChangeListener<FileChooser.ExtensionFilter> nativeSelectedExtensionFilterChangeListener =
            getNativeSelectedExtensionFilterChangeListener();
    private final ChangeListener<ExtensionFilter> selectedExtensionFilterChangeListener =
            getSelectedExtensionFilterChangeListener(fileChooser);
    private final ListChangeListener<ExtensionFilter> nativeExtensionFilterListChangeListener =
            getNativeExtensionFilterListChangeListener(fileChooser);

    public NativeFileSavePicker(Node node) {
        super(node);

        // Initializes synchronization between the FileChooser's selectedExtensionFilterProperty
        // and the FilePicker's selectedExtensionFilter property.
        synchronizeSelectedExtensionFilter(fileChooser);

        // Wrap the listener into a WeakListChangeListener to avoid memory leaks,
        // that can occur if observers are not unregistered from observed objects after use.
        getExtensionFilters().addListener(new WeakListChangeListener<>(nativeExtensionFilterListChangeListener));
    }

    /**
     * Synchronizes the selected {@link ExtensionFilter} between this file picker and the native {@link FileChooser}.
     * This ensures that changes in one are reflected in the other without causing infinite update loops.
     *
     * @param fileChooser the native file chooser to synchronize with; must not be {@code null}
     */
    final void synchronizeSelectedExtensionFilter(FileChooser fileChooser) {
        fileChooser.selectedExtensionFilterProperty()
                .addListener(new WeakChangeListener<>(nativeSelectedExtensionFilterChangeListener));
        selectedExtensionFilterProperty()
                .addListener(new WeakChangeListener<>(selectedExtensionFilterChangeListener));
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
            fileChooser.initialFileNameProperty().bind(initialFileName);
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
    final void showDialog() {
        // Basic configuration
        fileChooser.setTitle("Save file as...");

        // Retrieve scene and window references from the node
        final Scene scene = getNode().getScene();
        if (scene == null) {
            throw new IllegalStateException("Node must be attached to a scene");
        }
        final Window window = scene.getWindow();
        if (window == null) {
            throw new IllegalStateException("Scene must be attached to a stage");
        }

        // Show save dialog
        final File saveToFile = fileChooser.showSaveDialog(window);
        if (saveToFile != null) {
            final Function<File, CompletableFuture<Void>> onFileSelected = getOnFileSelected();
            if (onFileSelected != null) {
                onFileSelected.apply(saveToFile);
            }
        }
    }
}
