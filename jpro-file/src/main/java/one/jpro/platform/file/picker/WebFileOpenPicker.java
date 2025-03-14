package one.jpro.platform.file.picker;

import com.jpro.webapi.WebAPI;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import one.jpro.platform.file.ExtensionFilter;
import one.jpro.platform.file.FileSource;
import one.jpro.platform.file.WebFileSource;
import one.jpro.platform.file.util.NodeUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private final ListChangeListener<ExtensionFilter> webExtensionFilterListChangeListener;
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

        webExtensionFilterListChangeListener = change -> {
            final List<String> supportedExtensionsList = change.getList().stream()
                    .flatMap(ext -> ext.extensions().stream())
                    .toList();
            final Set<String> supportedExtensionsSet = new HashSet<>(supportedExtensionsList); // Remove duplicates
            multiFileUploader.supportedExtensions().setAll(supportedExtensionsSet.stream().toList());
        };

        // Wrap the listener into a WeakListChangeListener to avoid memory leaks,
        // that can occur if observers are not unregistered from observed objects after use.
        getExtensionFilters().addListener(new WeakListChangeListener<>(webExtensionFilterListChangeListener));
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
