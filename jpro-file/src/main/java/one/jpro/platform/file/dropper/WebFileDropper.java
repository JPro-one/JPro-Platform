package one.jpro.platform.file.dropper;

import com.jpro.webapi.WebAPI;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import one.jpro.platform.file.FileSource;
import one.jpro.platform.file.WebFileSource;
import one.jpro.platform.file.event.DataTransfer;
import one.jpro.platform.file.event.FileDragEvent;
import one.jpro.platform.file.util.NodeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a {@link FileDropper} implementation for JavaFX applications
 * running on the web via JPro server. This class extends the {@link BaseFileDropper}
 * class and specializes it for web files.
 *
 * @author Besmir Beqiri
 */
public class WebFileDropper extends BaseFileDropper {

    private final WebAPI.MultiFileUploader multiFileUploader;
    private List<WebFileSource> webFileSources = List.of();
    private final InvalidationListener fileDragOverListener;

    public WebFileDropper(Node node) {
        super(node);

        multiFileUploader = NodeUtils.getPropertyValue(node, NodeUtils.MULTI_FILE_UPLOADER_KEY,
                WebAPI.makeMultiFileUploadNodeStatic(node));
        multiFileUploader.setSelectFileOnDrop(true);

        // Add file drag over listener
        fileDragOverListener = (observable) -> {
            if (multiFileUploader.getFileDragOver()) {
                FileDragEvent fileDragEvent = new FileDragEvent(WebFileDropper.this, getNode(),
                        FileDragEvent.FILE_DRAG_ENTERED);
                final List<String> fileMimeTypes = multiFileUploader.getFilesDragOverTypes();
                if (!fileMimeTypes.isEmpty()) {
                    fileDragEvent.getDataTransfer().putData(DataTransfer.MIME_TYPES, fileMimeTypes);
                }
                Event.fireEvent(WebFileDropper.this, fileDragEvent);
            } else {
                Event.fireEvent(WebFileDropper.this,
                        new FileDragEvent(WebFileDropper.this, getNode(), FileDragEvent.FILE_DRAG_EXITED));
            }
        };

        // Wrap the listener into a WeakInvalidationListener to avoid memory leaks,
        // that can occur if observers are not unregistered from observed objects after use.
        multiFileUploader.fileDragOverProperty().addListener(new WeakInvalidationListener(fileDragOverListener));
    }

    @Override
    public final ObjectProperty<SelectionMode> selectionModeProperty() {
        if (selectionMode == null) {
            selectionMode = new SimpleObjectProperty<>(this, "selectionMode", SelectionMode.SINGLE) {

                @Override
                protected void invalidated() {
                    final SelectionMode selectionMode = get();
                    multiFileUploader.setSelectionMode(selectionMode);
                }
            };
        }
        return selectionMode;
    }

    @Override
    public final ObjectProperty<Consumer<List<? extends FileSource>>> onFilesSelectedProperty() {
        if (onFilesSelected == null) {
            onFilesSelected = new SimpleObjectProperty<>(this, "onFilesSelected") {

                @Override
                protected void invalidated() {
                    final Consumer<List<? extends FileSource>> onFilesSelectedConsumer = get();
                    multiFileUploader.setOnFilesSelected(onFilesSelectedConsumer == null ? null : jsFiles -> {
                        if (jsFiles != null) {
                            // Create a list of web file sources from the selected files.
                            webFileSources = new ArrayList<>(jsFiles.size());
                            jsFiles.stream().map(WebFileSource::new).forEach(webFileSources::add);

                            // Invoke the onFilesSelected consumer.
                            onFilesSelectedConsumer.accept(webFileSources);
                        }
                    });
                }
            };
        }
        return onFilesSelected;
    }

    @Override
    public final boolean isFilesDragOver() {
        return multiFileUploader.getFileDragOver();
    }

    @Override
    public final ReadOnlyBooleanProperty filesDragOverProperty() {
        return multiFileUploader.fileDragOverProperty();
    }

    /**
     * Returns the MIME types of the files which are currently dragged above the node.
     */
    public final ObservableList<String> getFilesDragOverTypes() {
        return multiFileUploader.getFilesDragOverTypes();
    }
}
