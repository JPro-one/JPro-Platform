package one.jpro.platform.file.picker.impl;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import one.jpro.platform.file.picker.DirectoryOpenPicker;
import one.jpro.platform.file.util.NodeUtils;

import java.io.File;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * {@link DirectoryOpenPicker} backed by a JavaFX {@link DirectoryChooser}.
 *
 * @author Florian Kirmaier
 */
public class NativeDirectoryOpenPicker implements DirectoryOpenPicker {

    private final Node node;

    public NativeDirectoryOpenPicker(Node node) {
        this.node = Objects.requireNonNull(node, "node must not be null");
        NodeUtils.addEventHandler(node, MouseEvent.MOUSE_CLICKED, event -> {
            Window window = node.getScene() == null ? null : node.getScene().getWindow();
            File directory = createDirectoryChooser().showDialog(window);
            Consumer<File> handler = getOnDirectorySelected();
            if (directory != null && handler != null) {
                handler.accept(directory);
            }
        });
    }

    @Override
    public final Node getNode() {
        return node;
    }

    private StringProperty title;

    @Override
    public final String getTitle() {
        return title == null ? null : title.get();
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

    private ObjectProperty<File> initialDirectory;

    @Override
    public final File getInitialDirectory() {
        return initialDirectory == null ? null : initialDirectory.get();
    }

    @Override
    public final void setInitialDirectory(File value) {
        initialDirectoryProperty().set(value);
    }

    @Override
    public final ObjectProperty<File> initialDirectoryProperty() {
        if (initialDirectory == null) {
            initialDirectory = new SimpleObjectProperty<>(this, "initialDirectory");
        }
        return initialDirectory;
    }

    private ObjectProperty<Consumer<File>> onDirectorySelected;

    @Override
    public final Consumer<File> getOnDirectorySelected() {
        return onDirectorySelected == null ? null : onDirectorySelected.get();
    }

    @Override
    public final void setOnDirectorySelected(Consumer<File> value) {
        onDirectorySelectedProperty().set(value);
    }

    @Override
    public final ObjectProperty<Consumer<File>> onDirectorySelectedProperty() {
        if (onDirectorySelected == null) {
            onDirectorySelected = new SimpleObjectProperty<>(this, "onDirectorySelected");
        }
        return onDirectorySelected;
    }

    DirectoryChooser createDirectoryChooser() {
        final DirectoryChooser chooser = new DirectoryChooser();
        chooser.titleProperty().bind(titleProperty());
        chooser.initialDirectoryProperty().bind(initialDirectoryProperty());
        return chooser;
    }
}
