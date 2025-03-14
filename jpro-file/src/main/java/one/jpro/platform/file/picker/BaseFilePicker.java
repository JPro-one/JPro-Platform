package one.jpro.platform.file.picker;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.stage.FileChooser;
import one.jpro.platform.file.ExtensionFilter;

import java.io.File;
import java.util.Objects;

/**
 * Base file picker implementation.
 *
 * @author Besmir Beqiri
 */
abstract class BaseFilePicker implements FilePicker {

    private final Node node;

    /**
     * Constructs a new instance with the specified Node.
     *
     * @param node the node to which the file picker should be attached
     */
    BaseFilePicker(Node node) {
        this.node = Objects.requireNonNull(node, "node must not be null");
    }

    @Override
    public final Node getNode() {
        return node;
    }

    // initial file name property
    private StringProperty initialFileName;

    @Override
    public final String getInitialFileName() {
        return (initialFileName == null) ? null : initialFileName.get();
    }

    @Override
    public final void setInitialFileName(final String value) {
        initialFileNameProperty().set(value);
    }

    @Override
    public final StringProperty initialFileNameProperty() {
        if (initialFileName == null) {
            initialFileName = new SimpleStringProperty(this, "initialFileName");
        }
        return initialFileName;
    }

    private ObjectProperty<File> initialDirectory;

    @Override
    public final File getInitialDirectory() {
        return (initialDirectory != null) ? initialDirectory.get() : null;
    }

    @Override
    public final void setInitialDirectory(final File value) {
        initialDirectoryProperty().set(value);
    }

    @Override
    public final ObjectProperty<File> initialDirectoryProperty() {
        if (initialDirectory == null) {
            initialDirectory = new SimpleObjectProperty<>(this, "initialDirectory");
        }
        return initialDirectory;
    }

    private StringProperty title;

    @Override
    public final String getTitle() {
        return title.get();
    }

    @Override
    public final void setTitle(final String value) {
        titleProperty().set(value);
    }

    @Override
    public final StringProperty titleProperty() {
        if (title == null) {
            title = new SimpleStringProperty(this, "title");
        }
        return title;
    }

    private final ObservableList<ExtensionFilter> extensionFilters = FXCollections.observableArrayList();

    @Override
    public final ObservableList<ExtensionFilter> getExtensionFilters() {
        return extensionFilters;
    }

    // selected extension filter property
    private ObjectProperty<ExtensionFilter> selectedExtensionFilter;

    @Override
    public final ExtensionFilter getSelectedExtensionFilter() {
        return (selectedExtensionFilter != null) ? selectedExtensionFilter.get() : null;
    }

    @Override
    public final void setSelectedExtensionFilter(final ExtensionFilter value) {
        selectedExtensionFilterProperty().set(value);
    }

    @Override
    public final ObjectProperty<ExtensionFilter> selectedExtensionFilterProperty() {
        if (selectedExtensionFilter == null) {
            selectedExtensionFilter = new SimpleObjectProperty<>(this, "selectedExtensionFilter");
        }
        return selectedExtensionFilter;
    }

    /**
     * Finds and returns the currently selected {@link ExtensionFilter}. If no filter is selected
     * or the selected filter is not present in the list, the first filter in the list is returned.
     * If the list is empty, {@code null} is returned.
     *
     * @return the selected extension filter or a default filter, or {@code null} if no filters are available
     */
    final ExtensionFilter findSelectedFilter() {
        ExtensionFilter selectedFilter = getSelectedExtensionFilter();
        if (selectedFilter == null || !extensionFilters.contains(selectedFilter)) {
            return extensionFilters.isEmpty() ? null : extensionFilters.get(0);
        } else {
            return selectedFilter;
        }
    }

    /**
     * Sets the native {@link FileChooser.ExtensionFilter} in the provided {@link FileChooser}
     * based on the given {@link ExtensionFilter}. This is needed to keep the underlying
     * {@code FileChooser} in sync with the selected {@link ExtensionFilter} in this picker.
     *
     * @param fileChooser the file chooser whose selected filter will be updated
     * @param newFilter   the new {@link ExtensionFilter} to apply
     */
    final void setNativeSelectedExtensionFilter(FileChooser fileChooser, ExtensionFilter newFilter) {
        FileChooser.ExtensionFilter extensionFilter = null;
        if (newFilter != null) {
            for (FileChooser.ExtensionFilter ef : fileChooser.getExtensionFilters()) {
                if (newFilter.description().equals(ef.getDescription())) {
                    extensionFilter = ef;
                    break;
                }
            }
        }
        fileChooser.setSelectedExtensionFilter(extensionFilter);
    }
}