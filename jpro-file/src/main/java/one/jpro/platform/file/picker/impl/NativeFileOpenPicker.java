package one.jpro.platform.file.picker.impl;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import one.jpro.platform.file.ExtensionFilter;
import one.jpro.platform.file.FileSource;
import one.jpro.platform.file.NativeFileSource;
import one.jpro.platform.file.util.NodeUtils;
import one.jpro.platform.file.picker.FileOpenPicker;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a {@link FileOpenPicker} implementation for JavaFX desktop/mobile
 * applications. This class specializes for selecting and opening files from
 * the native file system with a {@link FileChooser}, in single or multiple selection mode.
 *
 * @see FileChooser
 * @see NativeFileSource
 *
 * @author Besmir Beqiri
 * @author Indrit Beqiri
 * @author Florian Kirmaier
 */
public class NativeFileOpenPicker extends BaseFileOpenPicker {

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
            if (getSelectionMode() == SelectionMode.MULTIPLE) {
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

    /**
     * Creates and configures a new {@link FileChooser} instance.
     * <p>
     * The file chooser's title, initial directory, and initial file name are bound to the corresponding
     * properties of this picker. Additionally, it applies the extension filters defined in the picker to
     * filter the visible files.
     *
     * @return a configured {@code FileChooser} instance.
     */
    FileChooser createFileChooser() {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.titleProperty().bind(titleProperty());
        fileChooser.initialDirectoryProperty().bind(initialDirectoryProperty());
        fileChooser.initialFileNameProperty().bind(initialFileNameProperty());
        fileChooser.getExtensionFilters().addAll(getExtensionFilters().stream()
                .map(ExtensionFilter::toJavaFXExtensionFilter)
                .toList());
        setNativeSelectedExtensionFilter(fileChooser, getSelectedExtensionFilter());
        return fileChooser;
    }

    /**
     * Creates and configures a new {@link DirectoryChooser} instance.
     * <p>
     * The directory chooser's title and initial directory are bound to the corresponding properties of this picker.
     *
     * @return a configured {@code DirectoryChooser} instance.
     */
}
