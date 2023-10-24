package one.jpro.platform.file.picker;

import com.jpro.webapi.WebAPI;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import one.jpro.platform.file.ExtensionFilter;
import one.jpro.platform.file.FileSource;
import one.jpro.platform.file.WebFileSource;
import one.jpro.platform.file.util.NodeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a {@link FileOpenPicker} implementation for JavaFX applications
 * running on the web via JPro server. This class specializes for selecting
 * and opening local files on a web application.
 *
 * @author Besmir Beqiri
 */
public class WebFileOpenPicker extends BaseFileOpenPicker {

    private final WebAPI.MultiFileUploader multiFileUploader;
    private List<WebFileSource> webFileSources = List.of();

    /**
     * Initializes a new instance associated with the specified node.
     *
     * @param node The node associated with this file picker.
     */
    public WebFileOpenPicker(Node node) {
        super(node);

        multiFileUploader = NodeUtils.getPropertyValue(node, NodeUtils.MULTI_FILE_UPLOADER_KEY,
                WebAPI.makeMultiFileUploadNodeStatic(node));
        multiFileUploader.setSelectFileOnClick(true);
    }

    // title property
    private StringProperty title;

    @Override
    public final String getTitle() {
        return (title != null) ? title.get() : null;
    }

    @Override
    public final void setTitle(String value) {
        titleProperty().set(value);
    }

    @Override
    public final StringProperty titleProperty() {
        if (title == null) {
            title = new SimpleStringProperty(this, "title");
        }
        return title;
    }

    // initial file name property
    @Override
    public final StringProperty initialFileNameProperty() {
        if (initialFileName == null) {
            initialFileName = new SimpleStringProperty(this, "initialFileName");
        }
        return initialFileName;
    }

    // initial directory property
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

    @Override
    public final ObjectProperty<ExtensionFilter> selectedExtensionFilterProperty() {
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
}
