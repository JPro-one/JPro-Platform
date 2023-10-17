package one.jpro.platform.file.picker.impl;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import one.jpro.platform.file.ExtensionFilter;
import one.jpro.platform.file.FileSource;
import one.jpro.platform.file.picker.FilePicker;

import java.util.List;

/**
 * The BaseFilePicker class is an abstract class that implements the FilePicker interface.
 * It provides a base implementation for common functionality shared by different file picker implementations.
 *
 * @param <F> the type of FileSource used by the file picker
 * @author Besmir Beqiri
 */
abstract class BaseFilePicker<F extends FileSource<?>> implements FilePicker<F> {

    private final Node node;

    /**
     * Constructs a new instance of BaseFilePicker with the specified Node.
     *
     * @param node the node to which the file picker should be attached
     */
    BaseFilePicker(Node node) {
        this.node = node;
    }

    @Override
    public final Node getNode() {
        return node;
    }

    // upload progress property
    private ReadOnlyDoubleWrapper uploadProgress;

    @Override
    public final double getUploadProgress() {
        return uploadProgress == null ? 0.0 : uploadProgress.get();
    }

    final void setUploadProgress(double value) {
        uploadProgressPropertyImpl().set(value);
    }

    @Override
    public final ReadOnlyDoubleProperty uploadProgressProperty() {
        return uploadProgressPropertyImpl().getReadOnlyProperty();
    }

    final ReadOnlyDoubleWrapper uploadProgressPropertyImpl() {
        if (uploadProgress == null) {
            uploadProgress = new ReadOnlyDoubleWrapper(this, "uploadProgress", 0.0);
        }
        return uploadProgress;
    }

    private final ObservableList<ExtensionFilter> extensionFilters = FXCollections.observableArrayList();

    @Override
    public final ObservableList<ExtensionFilter> getExtensionFilters() {
        return extensionFilters;
    }

    // selected extension property
    ObjectProperty<ExtensionFilter> selectedExtensionFilter;

    @Override
    public final ExtensionFilter getSelectedExtensionFilter() {
        return (selectedExtensionFilter != null) ? selectedExtensionFilter.get() : null;
    }

    @Override
    public final void setSelectedExtensionFilter(final ExtensionFilter filter) {
        selectedExtensionFilterProperty().setValue(filter);
    }

    // selection mode property
    ObjectProperty<SelectionMode> selectionMode;

    @Override
    public final SelectionMode getSelectionMode() {
        return selectionMode == null ? SelectionMode.SINGLE : selectionMode.get();
    }

    @Override
    public final void setSelectionMode(final SelectionMode value) {
        selectionModeProperty().setValue(value);
    }

    // max file upload size property
    private LongProperty maxFileUploadSize;

    @Override
    public long getMaxFileUploadSize() {
        return maxFileUploadSize == null ? INDEFINITE : maxFileUploadSize.get();
    }

    @Override
    public void setMaxFileUploadSize(long value) {
        maxFileUploadSizeProperty().setValue(value);
    }

    @Override
    public LongProperty maxFileUploadSizeProperty() {
        if (maxFileUploadSize == null) {
            maxFileUploadSize = new SimpleLongProperty(this, "maxFileUploadSize", INDEFINITE);
        }
        return maxFileUploadSize;
    }

    void updateTotalProgress(final List<? extends FileSource<?>> fileSources) {
        uploadProgressPropertyImpl().unbind();
        uploadProgressPropertyImpl().bind(Bindings.createDoubleBinding(() ->
                        fileSources.stream()
                                .mapToDouble(FileSource::getProgress)
                                .reduce(0.0, Double::sum) / fileSources.size(),
                fileSources.stream()
                        .map(FileSource::progressProperty)
                        .toList().toArray(new ReadOnlyDoubleProperty[fileSources.size()])));
    }
}
