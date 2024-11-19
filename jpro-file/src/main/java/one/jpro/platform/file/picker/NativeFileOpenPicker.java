package one.jpro.platform.file.picker;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a {@link FileOpenPicker} implementation for JavaFX desktop/mobile
 * applications. This class specializes for selecting and opening files from
 * the native file system.
 *
 * @author Besmir Beqiri
 */
public class NativeFileOpenPicker extends BaseFileOpenPicker {

    private final FileChooser fileChooser = new FileChooser();
    private List<NativeFileSource> nativeFileSources = List.of();
    private final ListChangeListener<ExtensionFilter> extensionFiltersListChangeListener = change -> {
        while (change.next()) {
            if (change.wasAdded()) {
                for (ExtensionFilter extensionFilter : change.getAddedSubList()) {
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                            extensionFilter.description(), extensionFilter.extensions().stream()
                            .map(ext -> "*" + ext).toList()));
                }
            } else if (change.wasRemoved()) {
                for (ExtensionFilter extensionFilter : change.getRemoved()) {
                    fileChooser.getExtensionFilters().removeIf(filter ->
                            filter.getDescription().equals(extensionFilter.description()));
                }
            }
        }
    };

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
        getExtensionFilters().addListener(new WeakListChangeListener<>(extensionFiltersListChangeListener));

        // Define the action that should be performed when the user clicks on the node.
        node.addEventHandler(MouseEvent.MOUSE_CLICKED, actionEvent -> {
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
    public final ObjectProperty<ExtensionFilter> selectedExtensionFilterProperty() {
        if (selectedExtensionFilter == null) {
            selectedExtensionFilter = new SimpleObjectProperty<>(this, "selectedExtensionFilter") {

                @Override
                protected void invalidated() {
                    final ExtensionFilter selectedExtensionFilter = get();
                    if (selectedExtensionFilter != null) {
                        // check if the extension filter is already added to the file chooser
                        final var optionalExtensionFilter = fileChooser.getExtensionFilters().stream()
                                .filter(extensionFilter -> extensionFilter.getDescription()
                                        .equals(selectedExtensionFilter.description()))
                                .findFirst();
                        if (optionalExtensionFilter.isPresent()) {
                            fileChooser.setSelectedExtensionFilter(optionalExtensionFilter.get());
                        } else {
                            // add the extension filter which will automatically add the extension
                            // filter to the file chooser due to the registered listener
                            getExtensionFilters().add(selectedExtensionFilter);

                            // Retrieve the extension filter from the file chooser and set it as
                            // the selected extension filter
                            fileChooser.getExtensionFilters().stream()
                                    .filter(extensionFilter -> extensionFilter.getDescription()
                                            .equals(selectedExtensionFilter.description()))
                                    .findFirst().ifPresent(fileChooser::setSelectedExtensionFilter);
                        }
                    } else {
                        fileChooser.setSelectedExtensionFilter(null);
                    }
                }
            };
        }
        return selectedExtensionFilter;
    }

    @Override
    public final ObjectProperty<SelectionMode> selectionModeProperty() {
        if (selectionMode == null) {
            selectionMode = new SimpleObjectProperty<>(this, "selectionMode", SelectionMode.SINGLE);
        }
        return selectionMode;
    }
}
