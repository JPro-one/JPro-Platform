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
import javafx.stage.FileChooser;
import javafx.stage.Window;
import one.jpro.platform.file.ExtensionFilter;
import one.jpro.platform.file.FileSource;
import one.jpro.platform.file.NativeFileSource;
import one.jpro.platform.file.util.NodeUtils;

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
 */
public class NativeFileOpenPicker extends BaseFileOpenPicker {

    private final FileChooser fileChooser = new FileChooser();
    private final ChangeListener<FileChooser.ExtensionFilter> nativeSelectedExtensionFilterChangeListener =
            getNativeSelectedExtensionFilterChangeListener();
    private final ChangeListener<ExtensionFilter> selectedExtensionFilterChangeListener =
            getSelectedExtensionFilterChangeListener(fileChooser);
    private final ListChangeListener<ExtensionFilter> nativeExtensionFilterListChangeListener =
            getNativeExtensionFilterListChangeListener(fileChooser);
    private List<NativeFileSource> nativeFileSources = List.of();

    /**
     * Initializes a new instance associated with the specified node.
     *
     * @param node The node associated with this file picker.
     */
    public NativeFileOpenPicker(Node node) {
        super(node);

        // Initializes synchronization between the FileChooser's selectedExtensionFilterProperty
        // and the FilePicker's selectedExtensionFilter property.
        synchronizeSelectedExtensionFilter(fileChooser);

        // Wrap the listener into a WeakListChangeListener to avoid memory leaks,
        // that can occur if observers are not unregistered from observed objects after use.
        getExtensionFilters().addListener(new WeakListChangeListener<>(nativeExtensionFilterListChangeListener));

        // Define the action that should be performed when the user clicks on the node.
        NodeUtils.addEventHandler(node, MouseEvent.MOUSE_CLICKED, actionEvent -> {
            Window window = node.getScene().getWindow();
            if (getSelectionMode() == SelectionMode.MULTIPLE) {
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
    public final ObjectProperty<SelectionMode> selectionModeProperty() {
        if (selectionMode == null) {
            selectionMode = new SimpleObjectProperty<>(this, "selectionMode", SelectionMode.SINGLE);
        }
        return selectionMode;
    }
}
