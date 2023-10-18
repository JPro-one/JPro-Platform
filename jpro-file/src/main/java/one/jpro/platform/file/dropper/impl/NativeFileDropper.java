package one.jpro.platform.file.dropper.impl;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.TransferMode;
import one.jpro.platform.file.ExtensionFilter;
import one.jpro.platform.file.NativeFileSource;
import one.jpro.platform.file.dropper.FileDropper;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;


/**
 * Represents a {@link FileDropper} implementation for JavaFX desktop/mobile
 * applications. This class extends the {@link BaseFileDropper} class and
 * specializes it for selecting files from the native file system.
 *
 * @author Besmir Beqiri
 */
public final class NativeFileDropper extends BaseFileDropper<NativeFileSource> {

    public NativeFileDropper(Node node) {
        super(node);

        node.setOnDragOver(dragEvent -> {
            List<File> files = dragEvent.getDragboard().getFiles();
            if (files != null && !files.isEmpty()) {
                if (hasSupportedExtension(files)) {
                    dragEvent.acceptTransferModes(TransferMode.ANY);
                    setFilesDragOver(true);
                }
            }
        });

        // reset files drag over value when drag event ends
        node.setOnDragExited(dragEvent -> setFilesDragOver(false));

        node.setOnDragDropped(dragEvent -> {
            if (dragEvent.getDragboard().hasFiles()) {
                final ExtensionFilter extensionFilter = getExtensionFilter();
                List<File> files = dragEvent.getDragboard().getFiles().stream()
                        .filter(file -> extensionFilter != null && extensionFilter.extensions().stream()
                                .anyMatch(extension -> file.getName().endsWith(extension)))
                        .toList();

                if (!files.isEmpty()) {
                    // if single selection mode, then only allow one file, the first one
                    if (getSelectionMode() == SelectionMode.SINGLE) {
                        files = List.of(files.get(0));
                    }
                    Consumer<List<NativeFileSource>> onFilesSelectedConsumer = getOnFilesSelected();
                    if (onFilesSelectedConsumer != null) {
                        onFilesSelectedConsumer.accept(files.stream().map(NativeFileSource::new).toList());
                    }
                }
            }
        });
    }

    // files drag over property
    private ReadOnlyBooleanWrapper filesDragOver;

    @Override
    public boolean isFilesDragOver() {
        return (filesDragOver != null) && filesDragOver.get();
    }

    void setFilesDragOver(boolean value) {
        filesDragOverPropertyImpl().set(value);
    }

    @Override
    public ReadOnlyBooleanProperty filesDragOverProperty() {
        return filesDragOverPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyBooleanWrapper filesDragOverPropertyImpl() {
        if (filesDragOver == null) {
            filesDragOver = new ReadOnlyBooleanWrapper(this, "filesDragOver", false);
        }
        return filesDragOver;
    }

    // on files selected property
    ObjectProperty<Consumer<List<NativeFileSource>>> onFilesSelected;

    @Override
    public Consumer<List<NativeFileSource>> getOnFilesSelected() {
        return onFilesSelected == null ? null : onFilesSelected.get();
    }

    @Override
    public void setOnFilesSelected(Consumer<List<NativeFileSource>> value) {
        onFilesSelectedProperty().setValue(value);
    }

    @Override
    public ObjectProperty<Consumer<List<NativeFileSource>>> onFilesSelectedProperty() {
        if (onFilesSelected == null) {
            onFilesSelected = new SimpleObjectProperty<>(this, "onFilesSelected");
        }
        return onFilesSelected;
    }

    private boolean hasSupportedExtension(List<File> files) {
        final ExtensionFilter extensionFilter = getExtensionFilter();
        return extensionFilter == null || files.stream()
                .anyMatch(file -> extensionFilter.extensions().stream()
                        .anyMatch(extension -> file.getName().endsWith(extension)));
    }
}
