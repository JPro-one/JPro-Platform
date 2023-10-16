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
 * {@link FilePicker} implementation for the web.
 *
 * @author Besmir Beqiri
 */
public final class WebFilePicker extends BaseFilePicker<WebFileSource> {

    private final WebAPI.MultiFileUploader multiFileUploader;

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
                        selectedExtensionFilter.extensions().stream()
                                .map(ext -> {
                                    if (ext.startsWith("*")) {
                                        ext = ext.substring(1);
                                    }
                                    return ext;
                                }) // remove the leading
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
                    final Consumer<List<WebFileSource>> onFilesSelected = get();
                    multiFileUploader.setOnFilesSelected(onFilesSelected == null ? null : jsFiles -> {
                        if (jsFiles == null) return;
                        List<WebFileSource> files = new ArrayList<>(jsFiles.size());
                        jsFiles.stream().map(WebFileSource::new).forEach(files::add);
                        onFilesSelected.accept(files);
                    });
                }
            };
        }
        return onFilesSelected;
    }
}
