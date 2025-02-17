package one.jpro.platform.file.picker;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.stage.FileChooser;
import one.jpro.platform.file.ExtensionFilter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;

import static one.jpro.platform.file.ExtensionFilter.toJavaFXExtensionFilter;

/**
 * Base file picker implementation.
 *
 * @author Besmir Beqiri
 */
abstract class BaseFilePicker implements FilePicker {

    private final Node node;

    // Flags to prevent infinite synchronization loops
    private boolean updatingFromFileChooser = false;
    private boolean updatingFromProperty = false;

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
    StringProperty initialFileName;

    @Override
    public final StringProperty initialFileNameProperty() {
        if (initialFileName == null) {
            initialFileName = new SimpleStringProperty(this, "initialFileName");
        }
        return initialFileName;
    }

    @Override
    public final String getInitialFileName() {
        return (initialFileName == null) ? null : initialFileName.get();
    }

    @Override
    public final void setInitialFileName(final String value) {
        initialFileNameProperty().setValue(value);
    }

    ObjectProperty<File> initialDirectory;

    @Override
    public final ObjectProperty<File> initialDirectoryProperty() {
        if (initialDirectory == null) {
            initialDirectory = new SimpleObjectProperty<>(this, "initialDirectory");
        }
        return initialDirectory;
    }

    @Override
    public final File getInitialDirectory() {
        return (initialDirectory != null) ? initialDirectory.get() : null;
    }

    @Override
    public final void setInitialDirectory(final File value) {
        initialDirectoryProperty().set(value);
    }

    StringProperty title = new SimpleStringProperty(this, "title");

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
        return title;
    }

    private final ObservableList<ExtensionFilter> extensionFilters = FXCollections.observableArrayList();

    @Override
    public final ObservableList<ExtensionFilter> getExtensionFilters() {
        return extensionFilters;
    }

    // selected extension filter property
    ObjectProperty<ExtensionFilter> selectedExtensionFilter;

    @Override
    public final ExtensionFilter getSelectedExtensionFilter() {
        return (selectedExtensionFilter != null) ? selectedExtensionFilter.get() : null;
    }

    @Override
    public final void setSelectedExtensionFilter(final ExtensionFilter filter) {
        selectedExtensionFilterProperty().setValue(filter);
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
     * Creates a {@link ChangeListener} that listens for changes in the native {@link FileChooser}'s
     * selected extension filter and updates the corresponding property in this file picker.
     *
     * @return a change listener for the native file chooser's selected extension filter
     */
    @NotNull
    final ChangeListener<FileChooser.ExtensionFilter> getNativeSelectedExtensionFilterChangeListener() {
        return (observable, oldFilter, newFilter) -> {
            if (updatingFromProperty) {
                return;
            }
            updatingFromFileChooser = true;
            try {
                ExtensionFilter extensionFilter = null;
                if (newFilter != null) {
                    for (ExtensionFilter ef : extensionFilters) {
                        if (newFilter.getDescription().equals(ef.description())) {
                            extensionFilter = ef;
                            break;
                        }
                    }
                }
                setSelectedExtensionFilter(extensionFilter);
            } finally {
                updatingFromFileChooser = false;
            }
        };
    }

    /**
     * Creates a {@link ChangeListener} that listens for changes in the native {@link FileChooser}'s
     * selected extension filter and updates the corresponding property in this file picker.
     *
     * @return a change listener for the native file chooser's selected extension filter
     */
    @NotNull
    final ChangeListener<ExtensionFilter> getSelectedExtensionFilterChangeListener(FileChooser fileChooser) {
        return (observable, oldFilter, newFilter) -> {
            if (updatingFromFileChooser) {
                return;
            }

            updatingFromProperty = true;
            try {
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
            } finally {
                updatingFromProperty = false;
            }
        };
    }

    /**
     * Creates a {@link ListChangeListener} that listens for changes in the list of {@link ExtensionFilter}
     * instances and updates the native {@link FileChooser}'s extension filters accordingly.
     * <p>
     * This listener handles both additions and removals of extension filters.
     * </p>
     *
     * @param fileChooser the native file chooser whose extension filters will be updated; must not be {@code null}
     * @return a list change listener for updating the native file chooser's extension filters
     */
    @NotNull
    final ListChangeListener<ExtensionFilter> getNativeExtensionFilterListChangeListener(FileChooser fileChooser) {
        return change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (ExtensionFilter extensionFilter : change.getAddedSubList()) {
                        fileChooser.getExtensionFilters().add(toJavaFXExtensionFilter(extensionFilter));
                    }
                } else if (change.wasRemoved()) {
                    for (ExtensionFilter extensionFilter : change.getRemoved()) {
                        fileChooser.getExtensionFilters().removeIf(filter ->
                                filter.getDescription().equals(extensionFilter.description()));
                    }
                }
            }
        };
    }
}
