package one.jpro.platform.file.picker;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import one.jpro.platform.file.ExtensionFilter;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Represents a {@link FileSavePicker} implementation for JavaFX desktop/mobile
 * applications. This class specializes for selecting a file to save in the
 * native file system.
 *
 * @author Besmir Beqiri
 */
public class NativeFileSavePicker extends BaseFileSavePicker {

    private final FileChooser fileChooser = new FileChooser();
    private final ListChangeListener<ExtensionFilter> extensionFiltersListChangeListener = change -> {
        while (change.next()) {
            if (change.wasAdded()) {
                for (ExtensionFilter extensionFilter : change.getAddedSubList()) {
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                            extensionFilter.description(), extensionFilter.extensions().stream()
                            .map(ext -> "*" + ext).toList()));
                }
            } else if (change.wasRemoved()) {
                for (ExtensionFilter extensionFilter : change.getRemoved()) {
                    fileChooser.getExtensionFilters().removeIf(filter ->
                            filter.getDescription().equals(extensionFilter.description()));
                }
            }
        }
    };

    public NativeFileSavePicker(Node node) {
        super(node);

        // Wrap the listener into a WeakListChangeListener to avoid memory leaks,
        // that can occur if observers are not unregistered from observed objects after use.
        getExtensionFilters().addListener(new WeakListChangeListener<>(extensionFiltersListChangeListener));
    }

    @Override
    public final String getTitle() {
        return fileChooser.getTitle();
    }

    @Override
    public final void setTitle(final String value) {
        fileChooser.setTitle(value);
    }

    @Override
    public final StringProperty titleProperty() {
        return fileChooser.titleProperty();
    }

    @Override
    public final StringProperty initialFileNameProperty() {
        if (initialFileName == null) {
            initialFileName = new SimpleStringProperty(this, "initialFileName");
            fileChooser.initialFileNameProperty().bind(initialFileName);
        }
        return initialFileName;
    }

    @Override
    public final File getInitialDirectory() {
        return fileChooser.getInitialDirectory();
    }

    @Override
    public final void setInitialDirectory(final File value) {
        fileChooser.setInitialDirectory(value);
    }

    @Override
    public final ObjectProperty<File> initialDirectoryProperty() {
        return fileChooser.initialDirectoryProperty();
    }

    @Override
    public final ObjectProperty<ExtensionFilter> selectedExtensionFilterProperty() {
        if (selectedExtensionFilter == null) {
            selectedExtensionFilter = new SimpleObjectProperty<>(this, "selectedExtensionFilter") {

                @Override
                protected void invalidated() {
                    final ExtensionFilter selectedExtensionFilter = get();
                    if (selectedExtensionFilter != null) {
                        // check if the extension filter is already added to the file chooser
                        final var optionalExtensionFilter = fileChooser.getExtensionFilters().stream()
                                .filter(extensionFilter -> extensionFilter.getDescription()
                                        .equals(selectedExtensionFilter.description()))
                                .findFirst();
                        if (optionalExtensionFilter.isPresent()) {
                            fileChooser.setSelectedExtensionFilter(optionalExtensionFilter.get());
                        } else {
                            // add the extension filter which will automatically add the extension
                            // filter to the file chooser due to the registered listener
                            getExtensionFilters().add(selectedExtensionFilter);

                            // Retrieve the extension filter from the file chooser and set it as
                            // the selected extension filter
                            fileChooser.getExtensionFilters().stream()
                                    .filter(extensionFilter -> extensionFilter.getDescription()
                                            .equals(selectedExtensionFilter.description()))
                                    .findFirst().ifPresent(fileChooser::setSelectedExtensionFilter);
                        }
                    } else {
                        fileChooser.setSelectedExtensionFilter(null);
                    }
                }
            };
        }
        return selectedExtensionFilter;
    }

    @Override
    final void showDialog() {
        // Basic configuration
        fileChooser.setTitle("Save file as...");

        // Retrieve scene and window references from the node
        final Scene scene = getNode().getScene();
        if (scene == null) {
            throw new IllegalStateException("Node must be attached to a scene");
        }
        final Window window = scene.getWindow();
        if (window == null) {
            throw new IllegalStateException("Scene must be attached to a stage");
        }

        // Show save dialog
        final File saveToFile = fileChooser.showSaveDialog(window);
        if (saveToFile != null) {
            final Function<File, CompletableFuture<Void>> onFileSelected = getOnFileSelected();
            if (onFileSelected != null) {
                onFileSelected.apply(saveToFile);
            }
        }
    }
}
