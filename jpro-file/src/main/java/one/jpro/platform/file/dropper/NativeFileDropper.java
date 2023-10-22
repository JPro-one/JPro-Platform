package one.jpro.platform.file.dropper;

import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.TransferMode;
import one.jpro.platform.file.ExtensionFilter;
import one.jpro.platform.file.FileSource;
import one.jpro.platform.file.NativeFileSource;
import one.jpro.platform.file.event.FileDragEvent;

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
public class NativeFileDropper extends BaseFileDropper {

    public NativeFileDropper(Node node) {
        super(node);

        node.setOnDragOver(dragEvent -> {
            if (dragEvent.getDragboard().hasFiles()) {
                List<File> files = dragEvent.getDragboard().getFiles();
                if (hasSupportedExtension(files)) {
                    dragEvent.acceptTransferModes(TransferMode.COPY);
                } else {
                    dragEvent.acceptTransferModes(TransferMode.NONE);
                }
            }
        });

        node.setOnDragEntered(dragEvent -> Event.fireEvent(NativeFileDropper.this,
                new FileDragEvent(NativeFileDropper.this, getNode(), FileDragEvent.FILE_DRAG_ENTERED)));
        node.setOnDragExited(dragEvent -> Event.fireEvent(NativeFileDropper.this,
                new FileDragEvent(NativeFileDropper.this, getNode(), FileDragEvent.FILE_DRAG_EXITED)));

        node.setOnDragDropped(dragEvent -> {
            if (dragEvent.getDragboard().hasFiles()) {
                final ExtensionFilter extensionFilter = getExtensionFilter();
                List<NativeFileSource> nativeFileSources = dragEvent.getDragboard().getFiles().stream()
                        .filter(file -> extensionFilter != null && extensionFilter.extensions().stream()
                                .anyMatch(extension -> file.getName().endsWith(extension)))
                        .map(NativeFileSource::new)
                        .toList();

                // handle selected files
                Consumer<List<? extends FileSource>> onFilesSelectedConsumer = getOnFilesSelected();
                if (onFilesSelectedConsumer != null) {
                    // if single selection mode, then only allow one file, the first one
                    if (getSelectionMode() == SelectionMode.SINGLE) {
                        nativeFileSources = List.of(nativeFileSources.get(0));
                    }
                    onFilesSelectedConsumer.accept(nativeFileSources);
                }
            }
        });
    }
}
