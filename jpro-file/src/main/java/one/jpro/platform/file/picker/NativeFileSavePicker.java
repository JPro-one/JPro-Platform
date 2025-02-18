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
import javafx.stage.DirectoryChooser;
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

    public NativeFileSavePicker(Node node) {
        super(node);
    }

    @Override
    final void showDialog() {
        var fileChooser = createFileChooser();
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

    private FileChooser createFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(getExtensionFilters().stream()
                .map(ExtensionFilter::toJavaFXExtensionFilter)
                .toList());
        return fileChooser;
    }
}
