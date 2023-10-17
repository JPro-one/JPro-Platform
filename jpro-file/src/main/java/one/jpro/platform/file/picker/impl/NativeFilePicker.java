package one.jpro.platform.file.picker.impl;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import one.jpro.platform.file.ExtensionFilter;
import one.jpro.platform.file.NativeFileSource;
import one.jpro.platform.file.picker.FilePicker;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a {@link FilePicker} implementation for JavaFX desktop/mobile
 * applications. This class extends the {@link BaseFilePicker} class and
 * specializes it for selecting files from the native file system.
 *
 * @author Besmir Beqiri
 */
public final class NativeFilePicker extends BaseFilePicker<NativeFileSource> {

    private final FileChooser fileChooser = new FileChooser();
    private List<NativeFileSource> nativeFileSources = List.of();
    private final ListChangeListener<ExtensionFilter> extensionListFiltersListener = change -> {
        while (change.next()) {
            if (change.wasAdded()) {
                for (ExtensionFilter extensionFilter : change.getAddedSubList()) {
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                            extensionFilter.description(), extensionFilter.extensions()));
                }
            } else if (change.wasRemoved()) {
                for (ExtensionFilter extensionFilter : change.getRemoved()) {
                    fileChooser.getExtensionFilters().removeIf(filter ->
                            filter.getDescription().equals(extensionFilter.description()));
                }
            }
        }
    };
    private final WeakListChangeListener<ExtensionFilter> weakExtensionListFiltersListener =
            new WeakListChangeListener<>(extensionListFiltersListener);

    /**
     * Initializes a new instance associated with the specified node.
     *
     * @param node The node associated with this file picker.
     */
    public NativeFilePicker(Node node) {
        super(node);

        // Wrap the listener into a WeakListChangeListener to avoid memory leaks,
        // that can occur if observers are not unregistered from observed objects after use.
        getExtensionFilters().addListener(weakExtensionListFiltersListener);

        // Define the action that should be performed when the user clicks on the node.
        node.setOnMouseClicked(mouseEvent -> {
            Window window = node.getScene().getWindow();
            if (getSelectionMode() == SelectionMode.MULTIPLE) {
                final List<File> files = fileChooser.showOpenMultipleDialog(window);
                if (files != null && !files.isEmpty()) {
                    // Create a list of native file sources from the selected files.
                    nativeFileSources = files.stream().map(NativeFileSource::new).toList();

                    // Invoke the onFilesSelected consumer.
                    Consumer<List<NativeFileSource>> onFilesSelectedConsumer = getOnFilesSelected();
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
                    Consumer<List<NativeFileSource>> onFilesSelectedConsumer = getOnFilesSelected();
                    if (onFilesSelectedConsumer != null) {
                        onFilesSelectedConsumer.accept(nativeFileSources);
                    }
                }
            }
        });
    }

    @Override
    public String getTitle() {
        return fileChooser.getTitle();
    }

    @Override
    public void setTitle(final String value) {
        fileChooser.setTitle(value);
    }

    @Override
    public StringProperty titleProperty() {
        return fileChooser.titleProperty();
    }

    @Override
    public File getInitialDirectory() {
        return fileChooser.getInitialDirectory();
    }

    @Override
    public void setInitialDirectory(final File value) {
        fileChooser.setInitialDirectory(value);
    }

    @Override
    public ObjectProperty<File> initialDirectoryProperty() {
        return fileChooser.initialDirectoryProperty();
    }

    @Override
    public ObjectProperty<ExtensionFilter> selectedExtensionFilterProperty() {
        if (selectedExtensionFilter == null) {
            selectedExtensionFilter = new SimpleObjectProperty<>(this, "selectedExtensionFilter") {

                @Override
                protected void invalidated() {
                    final ExtensionFilter filter = get();
                    if (filter != null) {
                        final FileChooser.ExtensionFilter selectedExtensionFilter = fileChooser.getExtensionFilters().stream()
                                .filter(extensionFilter -> extensionFilter.getExtensions().equals(filter.extensions()))
                                .findFirst()
                                .orElse(null);
                        fileChooser.setSelectedExtensionFilter(selectedExtensionFilter);
                    } else {
                        fileChooser.setSelectedExtensionFilter(null);
                    }
                }
            };
        }
        return selectedExtensionFilter;
    }

    @Override
    public ObjectProperty<SelectionMode> selectionModeProperty() {
        if (selectionMode == null) {
            selectionMode = new SimpleObjectProperty<>(this, "selectionMode", SelectionMode.SINGLE);
        }
        return selectionMode;
    }

    // on files selected property
    ObjectProperty<Consumer<List<NativeFileSource>>> onFilesSelected;

    @Override
    public Consumer<List<NativeFileSource>> getOnFilesSelected() {
        return onFilesSelected == null ? null : onFilesSelected.get();
    }

    @Override
    public void setOnFilesSelected(Consumer<List<NativeFileSource>> value) {
        onFilesSelectedProperty().setValue(value);
    }

    @Override
    public ObjectProperty<Consumer<List<NativeFileSource>>> onFilesSelectedProperty() {
        if (onFilesSelected == null) {
            onFilesSelected = new SimpleObjectProperty<>(this, "onFilesSelected");
        }
        return onFilesSelected;
    }
}
