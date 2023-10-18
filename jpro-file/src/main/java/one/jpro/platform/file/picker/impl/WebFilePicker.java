package one.jpro.platform.file.picker.impl;

import com.jpro.webapi.WebAPI;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import one.jpro.platform.file.ExtensionFilter;
import one.jpro.platform.file.WebFileSource;
import one.jpro.platform.file.picker.FilePicker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a {@link FilePicker} implementation for JavaFX applications
 * running on the web via JPro server. This class extends the {@link BaseFilePicker}
 * class and specializes it for web files.
 *
 * @author Besmir Beqiri
 */
public final class WebFilePicker extends BaseFilePicker<WebFileSource> {

    private final WebAPI.MultiFileUploader multiFileUploader;
    private List<WebFileSource> webFileSources = List.of();

    /**
     * Initializes a new instance associated with the specified node.
     *
     * @param node The node associated with this file picker.
     */
    public WebFilePicker(Node node) {
        super(node);

        multiFileUploader = WebAPI.makeMultiFileUploadNodeStatic(node);
        multiFileUploader.setSelectFileOnClick(true);
    }

    // title property
    private StringProperty title;

    @Override
    public String getTitle() {
        return (title != null) ? title.get() : null;
    }

    @Override
    public void setTitle(String value) {
        titleProperty().set(value);
    }

    @Override
    public StringProperty titleProperty() {
        if (title == null) {
            title = new SimpleStringProperty(this, "title");
        }
        return title;
    }

    // initialDirectory property
    private ObjectProperty<File> initialDirectory;

    @Override
    public File getInitialDirectory() {
        return (initialDirectory != null) ? initialDirectory.get() : null;
    }

    @Override
    public void setInitialDirectory(final File value) {
        initialDirectoryProperty().set(value);
    }

    @Override
    public ObjectProperty<File> initialDirectoryProperty() {
        if (initialDirectory == null) {
            initialDirectory = new SimpleObjectProperty<>(this, "initialDirectory");
        }
        return initialDirectory;
    }

    @Override
    public ObjectProperty<ExtensionFilter> selectedExtensionFilterProperty() {
        if (selectedExtensionFilter == null) {
            selectedExtensionFilter = new SimpleObjectProperty<>(this, "selectedExtensionFilter") {

                @Override
                protected void invalidated() {
                    final ExtensionFilter selectedExtensionFilter = get();
                    multiFileUploader.supportedExtensions().clear();
                    if (selectedExtensionFilter != null) {
                        selectedExtensionFilter.extensions()
                                .forEach(multiFileUploader.supportedExtensions()::add);
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
}
