package one.jpro.platform.file.picker;

import com.jpro.webapi.WebAPI;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.stage.FileChooser;
import one.jpro.platform.file.ExtensionFilter;
import org.jetbrains.annotations.NotNull;

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
    public final String getInitialFileName() {
        return (initialFileName == null) ? null : initialFileName.get();
    }

    @Override
    public final void setInitialFileName(final String value) {
        initialFileNameProperty().setValue(value);
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

    final ExtensionFilter findSelectedFilter() {
        ExtensionFilter selectedFilter = getSelectedExtensionFilter();
        if (selectedFilter == null || !extensionFilters.contains(selectedFilter)) {
            return extensionFilters.isEmpty() ? null : extensionFilters.get(0);
        } else {
            return selectedFilter;
        }
    }

    final void synchronizeSelectedExtensionFilter(FileChooser fileChooser) {
        fileChooser.selectedExtensionFilterProperty()
                .addListener(new WeakChangeListener<>(getNativeSelectedExtensionFilterChangeListener()));
        selectedExtensionFilterProperty()
                .addListener(new WeakChangeListener<>(getSelectedExtensionFilterChangeListener(fileChooser)));
    }

    private ChangeListener<FileChooser.ExtensionFilter> getNativeSelectedExtensionFilterChangeListener() {
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

    private ChangeListener<ExtensionFilter> getSelectedExtensionFilterChangeListener(FileChooser fileChooser) {
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
     * Creates a listener for changes in the list of extension filters for a native file chooser.
     * When extension filters are added or removed, the file chooser's extension filters are updated accordingly.
     *
     * @param fileChooser the native file chooser whose extension filters will be updated
     * @return A ListChangeListener that updates the extension filters of the native file chooser.
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

    /**
     * Creates a listener for changes in the list of extension filters for a web file uploader.
     * When extension filters are added or removed, the web file uploader's supported extensions are updated accordingly.
     *
     * @param multiFileUploader the web file uploader whose supported extensions will be updated
     * @return A ListChangeListener that updates the supported extensions of the web file uploader.
     */
    @NotNull
    final ListChangeListener<ExtensionFilter> getWebExtensionFilterListChangeListener(WebAPI.MultiFileUploader multiFileUploader) {
        return change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (ExtensionFilter extensionFilter : change.getAddedSubList()) {
                        extensionFilter.extensions()
                                .forEach(multiFileUploader.supportedExtensions()::add);
                    }
                } else if (change.wasRemoved()) {
                    for (ExtensionFilter extensionFilter : change.getRemoved()) {
                        extensionFilter.extensions()
                                .forEach(multiFileUploader.supportedExtensions()::remove);
                    }
                }
            }
        };
    }
}
