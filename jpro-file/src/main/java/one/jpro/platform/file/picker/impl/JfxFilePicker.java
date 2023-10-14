package one.jpro.platform.file.picker.impl;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
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
 * {@link FilePicker} implementation for JavaFX on the desktop/mobile.
 *
 * @author Besmir Beqiri
 */
public final class JfxFilePicker extends BaseFilePicker<NativeFileSource> {

    private final FileChooser fileChooser;
    private boolean multiple = false;

    public JfxFilePicker(Node node) {
        super(node);
        fileChooser = new FileChooser();

        extensionListFiltersListener = change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (ExtensionFilter extensionFilter : change.getAddedSubList()) {
                        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                                extensionFilter.description(), extensionFilter.extensions()));
                    }
                } else if (change.wasRemoved()) {
                    for (ExtensionFilter extensionFilter : change.getRemoved()) {
                        fileChooser.getExtensionFilters().removeIf(filter ->
                                filter.getExtensions().equals(extensionFilter.extensions()));
                    }
                }
            }
        };

        // Wrap the listener into a WeakListChangeListener to avoid memory leaks,
        // that can occur if observers are not unregistered from observed objects after use.
        weakExtensionListFiltersListener = new WeakListChangeListener<>(extensionListFiltersListener);
        getExtensionFilters().addListener(weakExtensionListFiltersListener);

        // Define the action that should be performed when the user clicks on the node.
        node.setOnMouseClicked(mouseEvent -> {
            Window window = node.getScene().getWindow();
            if (multiple) {
                List<File> files = fileChooser.showOpenMultipleDialog(window);
                if (files != null && !files.isEmpty()) {
                    Consumer<List<NativeFileSource>> onFilesSelectedConsumer = getOnFilesSelected();
                    if (onFilesSelectedConsumer != null) {
                        onFilesSelectedConsumer.accept(files.stream().map(NativeFileSource::new).toList());
                    }
                }
            } else {
                File file = fileChooser.showOpenDialog(window);
                if (file != null) {
                    Consumer<List<NativeFileSource>> onFilesSelectedConsumer = getOnFilesSelected();
                    if (onFilesSelectedConsumer != null) {
                        onFilesSelectedConsumer.accept(List.of(new NativeFileSource(file)));
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
    public String getInitialFileName() {
        return fileChooser.getInitialFileName();
    }

    @Override
    public void setInitialFileName(final String value) {
        fileChooser.setInitialFileName(value);
    }

    @Override
    public ObjectProperty<String> initialFileNameProperty() {
        return fileChooser.initialFileNameProperty();
    }

    @Override
    public ObjectProperty<ExtensionFilter> selectedExtensionFilterProperty() {
        if (selectedExtensionFilter == null) {
            selectedExtensionFilter = new SimpleObjectProperty<>(this, "selectedExtensionFilter") {

                @Override
                protected void invalidated() {
                    final ExtensionFilter filter = get();
                    if (filter != null) {
                        fileChooser.setSelectedExtensionFilter(
                                new FileChooser.ExtensionFilter(filter.description(), filter.extensions()));
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
            selectionMode = new SimpleObjectProperty<>(this, "selectionMode", SelectionMode.SINGLE) {

                @Override
                protected void invalidated() {
                    final SelectionMode selectionMode = get();
                    multiple = selectionMode == SelectionMode.MULTIPLE;
                }
            };
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
