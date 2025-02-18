package one.jpro.platform.file.picker;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import one.jpro.platform.file.ExtensionFilter;
import one.jpro.platform.file.FileSource;
import one.jpro.platform.file.NativeFileSource;
import one.jpro.platform.file.util.NodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a {@link FileOpenPicker} implementation for JavaFX desktop/mobile
 * applications. This class specializes for selecting and opening files from
 * the native file system.
 *
 * @author Besmir Beqiri
 * @author Indrit Beqiri
 * @author Florian Kirmaier
 */
public class NativeFileOpenPicker extends BaseFileOpenPicker {
    private static final Logger logger = LoggerFactory.getLogger(NativeFileOpenPicker.class);

    private List<NativeFileSource> nativeFileSources = List.of();

    /**
     * Initializes a new instance associated with the specified node.
     *
     * @param node The node associated with this file picker.
     */
    public NativeFileOpenPicker(Node node) {
        super(node);

        // Define the action that should be performed when the user clicks on the node.
        NodeUtils.addEventHandler(node, MouseEvent.MOUSE_CLICKED, actionEvent -> {
            Window window = node.getScene().getWindow();
            var useDirectory = getExtensionFilters().stream().anyMatch(x -> x.allowDirectory());
            var hasFilesTypes = getExtensionFilters().stream().anyMatch(x -> x.extensions().size() > 0);

            if (useDirectory && hasFilesTypes) {
                logger.warn("You can't use directory and file types at the same time. Directory will be used.");
            }
            if (useDirectory) {
                DirectoryChooser directoryChooser = createDirectoryChooser();
                final File file = directoryChooser.showDialog(window);
                if (file != null) {
                    // Create a list of native file sources from the selected file.
                    nativeFileSources = List.of(new NativeFileSource(file));
                    // Invoke the onFilesSelected consumer.
                    Consumer<List<? extends FileSource>> onFilesSelectedConsumer = getOnFilesSelected();
                    if (onFilesSelectedConsumer != null) {
                        onFilesSelectedConsumer.accept(nativeFileSources);
                    }
                }
            } else if (getSelectionMode() == SelectionMode.MULTIPLE) {
                FileChooser fileChooser = createFileChooser();
                final List<File> files = fileChooser.showOpenMultipleDialog(window);
                if (files != null && !files.isEmpty()) {
                    // Create a list of native file sources from the selected files.
                    nativeFileSources = files.stream().map(NativeFileSource::new).toList();

                    // Invoke the onFilesSelected consumer.
                    Consumer<List<? extends FileSource>> onFilesSelectedConsumer = getOnFilesSelected();
                    if (onFilesSelectedConsumer != null) {
                        onFilesSelectedConsumer.accept(nativeFileSources);
                    }
                }
            } else {
                FileChooser fileChooser = createFileChooser();
                final File file = fileChooser.showOpenDialog(window);
                if (file != null) {
                    // Create a list of native file sources from the selected file.
                    nativeFileSources = List.of(new NativeFileSource(file));

                    // Invoke the onFilesSelected consumer.
                    Consumer<List<? extends FileSource>> onFilesSelectedConsumer = getOnFilesSelected();
                    if (onFilesSelectedConsumer != null) {
                        onFilesSelectedConsumer.accept(nativeFileSources);
                    }
                }
            }
        });
    }

    @Override
    public final ObjectProperty<SelectionMode> selectionModeProperty() {
        if (selectionMode == null) {
            selectionMode = new SimpleObjectProperty<>(this, "selectionMode", SelectionMode.SINGLE);
        }
        return selectionMode;
    }

    private FileChooser createFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(getExtensionFilters().stream()
                .map(ExtensionFilter::toJavaFXExtensionFilter)
                .toList());
        fileChooser.titleProperty().bind(titleProperty());
        return fileChooser;
    }

    private DirectoryChooser createDirectoryChooser() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.titleProperty().bind(titleProperty());
        return directoryChooser;
    }
}
