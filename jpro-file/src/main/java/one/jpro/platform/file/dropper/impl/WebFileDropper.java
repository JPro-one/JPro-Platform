package one.jpro.platform.file.dropper.impl;

import com.jpro.webapi.WebAPI;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import one.jpro.platform.file.WebFileSource;
import one.jpro.platform.file.dropper.FileDropper;

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
public final class WebFileDropper extends BaseFileDropper<WebFileSource> {

    private final WebAPI.MultiFileUploader multiFileUploader;
    private List<WebFileSource> webFileSources = List.of();

    public WebFileDropper(Node node) {
        super(node);

        multiFileUploader = WebAPI.makeMultiFileUploadNodeStatic(node);
        multiFileUploader.setSelectFileOnDrop(true);
    }

    @Override
    public ObjectProperty<SelectionMode> selectionModeProperty() {
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

    // on files selected property
    private ObjectProperty<Consumer<List<WebFileSource>>> onFilesSelected;

    @Override
    public Consumer<List<WebFileSource>> getOnFilesSelected() {
        return onFilesSelected == null ? null : onFilesSelected.get();
    }

    @Override
    public void setOnFilesSelected(Consumer<List<WebFileSource>> value) {
        onFilesSelectedProperty().setValue(value);
    }

    @Override
    public ObjectProperty<Consumer<List<WebFileSource>>> onFilesSelectedProperty() {
        if (onFilesSelected == null) {
            onFilesSelected = new SimpleObjectProperty<>(this, "onFilesSelected") {

                @Override
                protected void invalidated() {
                    final Consumer<List<WebFileSource>> onFilesSelectedConsumer = get();
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
    public boolean isFilesDragOver() {
        return multiFileUploader.getFileDragOver();
    }

    @Override
    public ReadOnlyBooleanProperty filesDragOverProperty() {
        return multiFileUploader.fileDragOverProperty();
    }
}
