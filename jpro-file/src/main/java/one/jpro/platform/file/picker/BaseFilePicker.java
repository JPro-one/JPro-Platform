package one.jpro.platform.file.picker;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.stage.FileChooser;
import one.jpro.platform.file.ExtensionFilter;

import java.util.Objects;

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
}
