package one.jpro.platform.file.picker.impl;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import one.jpro.platform.file.ExtensionFilter;
import one.jpro.platform.file.FileSource;
import one.jpro.platform.file.picker.FilePicker;

/**
 * Base {@link FilePicker} implementation.
 *
 * @author Besmir Beqiri
 */
abstract class BaseFilePicker<FS extends FileSource<?>> implements FilePicker<FS> {

    private final Node node;

    ListChangeListener<ExtensionFilter> extensionListFiltersListener;

    WeakListChangeListener<ExtensionFilter> weakExtensionListFiltersListener;

    public BaseFilePicker(Node node) {
        this.node = node;
    }

    @Override
    public final Node getNode() {
        return node;
    }

    // progress property
    private ReadOnlyDoubleWrapper progress;

    @Override
    public final double getProgress() {
        return progress == null ? 0.0 : progress.get();
    }

    final void setProgress(double value) {
        progressPropertyImpl().set(value);
    }

    @Override
    public final ReadOnlyDoubleProperty progressProperty() {
        return progressPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyDoubleWrapper progressPropertyImpl() {
        if (progress == null) {
            progress = new ReadOnlyDoubleWrapper(this, "progress", 0.0);
        }
        return progress;
    }

    /**
     * Specifies the extension filters used in the displayed file dialog.
     */
    private final ObservableList<ExtensionFilter> extensionFilters = FXCollections.observableArrayList();

    // selected extension property
    ObjectProperty<ExtensionFilter> selectedExtensionFilter;

    @Override
    public final ObservableList<ExtensionFilter> getExtensionFilters() {
        return extensionFilters;
    }

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
}
