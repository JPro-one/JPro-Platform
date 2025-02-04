package one.jpro.platform.file.dropper;

import com.sun.javafx.event.EventHandlerManager;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventDispatchChain;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import one.jpro.platform.file.ExtensionFilter;
import one.jpro.platform.file.FileSource;
import one.jpro.platform.file.event.FileDragEvent;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * This is an abstract class that implements the {@link FileDropper} interface.
 * It provides a base implementation for common functionality shared by different implementations.
 *
 * @author Besmir Beqiri
 */
abstract class BaseFileDropper implements FileDropper {

    private final Node node;

    public BaseFileDropper(Node node) {
        this.node = Objects.requireNonNull(node, "node must not be null");
    }

    public Node getNode() {
        return node;
    }

    // extension filter property
    ObjectProperty<ExtensionFilter> extensionFilter;

    @Override
    public final ExtensionFilter getExtensionFilter() {
        return (extensionFilter != null) ? extensionFilter.get() : null;
    }

    @Override
    public final void setExtensionFilter(final ExtensionFilter filter) {
        extensionFilterProperty().setValue(filter);
    }

    @Override
    public final ObjectProperty<ExtensionFilter> extensionFilterProperty() {
        if (extensionFilter == null) {
            extensionFilter = new SimpleObjectProperty<>(this, "extensionFilter");
        }
        return extensionFilter;
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

    @Override
    public ObjectProperty<SelectionMode> selectionModeProperty() {
        if (selectionMode == null) {
            selectionMode = new SimpleObjectProperty<>(this, "selectionMode", SelectionMode.SINGLE);
        }
        return selectionMode;
    }

    // On drag entered property
    private ObjectProperty<EventHandler<FileDragEvent>> onDragEntered;

    @Override
    public final EventHandler<FileDragEvent> getOnDragEntered() {
        return onDragEntered == null ? null : onDragEntered.get();
    }

    @Override
    public final void setOnDragEntered(EventHandler<FileDragEvent> value) {
        onDragEnteredProperty().setValue(value);
    }

    @Override
    public final ObjectProperty<EventHandler<FileDragEvent>> onDragEnteredProperty() {
        if (onDragEntered == null) {
            onDragEntered = new SimpleObjectProperty<>(this, "onDragEntered") {

                @Override
                protected void invalidated() {
                    eventHandlerManager.setEventHandler(FileDragEvent.FILE_DRAG_ENTERED, get());
                }
            };
        }
        return onDragEntered;
    }

    // On drag exited property
    private ObjectProperty<EventHandler<FileDragEvent>> onDragExited;

    @Override
    public final EventHandler<FileDragEvent> getOnDragExited() {
        return onDragExited == null ? null : onDragExited.get();
    }

    @Override
    public final void setOnDragExited(EventHandler<FileDragEvent> value) {
        onDragExitedProperty().setValue(value);
    }

    @Override
    public final ObjectProperty<EventHandler<FileDragEvent>> onDragExitedProperty() {
        if (onDragExited == null) {
            onDragExited = new SimpleObjectProperty<>(this, "onDragExited") {

                    @Override
                    protected void invalidated() {
                        eventHandlerManager.setEventHandler(FileDragEvent.FILE_DRAG_EXITED, get());
                    }
            };
        }
        return onDragExited;
    }

    // on files selected property
    ObjectProperty<Consumer<List<? extends FileSource>>> onFilesSelected;

    @Override
    public Consumer<List<? extends FileSource>> getOnFilesSelected() {
        return onFilesSelected == null ? null : onFilesSelected.get();
    }

    @Override
    public void setOnFilesSelected(Consumer<List<? extends FileSource>> value) {
        onFilesSelectedProperty().setValue(value);
    }

    @Override
    public ObjectProperty<Consumer<List<? extends FileSource>>> onFilesSelectedProperty() {
        if (onFilesSelected == null) {
            onFilesSelected = new SimpleObjectProperty<>(this, "onFilesSelected");
        }
        return onFilesSelected;
    }

    private final EventHandlerManager eventHandlerManager = new EventHandlerManager(this);

    @Override
    public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
        return tail.prepend(eventHandlerManager);
    }

    boolean hasSupportedExtension(List<File> files) {
        final ExtensionFilter extensionFilter = getExtensionFilter();
        return extensionFilter == null || files.stream()
                .anyMatch(file -> extensionFilter.extensions().stream()
                        .anyMatch(extension -> file.getName().toLowerCase().endsWith(extension)));
    }
}
