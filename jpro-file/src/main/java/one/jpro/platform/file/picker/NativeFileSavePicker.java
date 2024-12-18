package one.jpro.platform.file.picker;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Window;

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

    private final FileChooser fileChooser;

    public NativeFileSavePicker(Node node) {
        super(node);

        // Initialize the FileChooser
        fileChooser = new FileChooser();

        // Initializes synchronization between the FileChooser's selectedExtensionFilterProperty
        // and the FilePicker's selectedExtensionFilter property.
        synchronizeSelectedExtensionFilter(fileChooser);

        getExtensionFilters().addListener(getNativeExtensionFilterListChangeListener(fileChooser));
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
