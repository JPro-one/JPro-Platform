package one.jpro.platform.file;

import javafx.beans.property.ObjectProperty;

import java.util.List;
import java.util.function.Consumer;

/**
 * Multiple file selector interface.
 *
 * @author Besmir Beqiri
 */
public interface MultipleFileSelector {

    /**
     * Gets the handler to be called when the user selects files.
     *
     * @return the event handler or <code>null</code>.
     */
    Consumer<List<? extends FileSource>> getOnFilesSelected();

    /**
     * Sets the handler to be called when the user selects files.
     *
     * @param value the event handler or <code>null</code>.
     */
    void setOnFilesSelected(Consumer<List<? extends FileSource>> value);

    /**
     * Defines the handler to be called when the user selects files.
     * The handler returns the selected files or {@code null} if
     * no file has been selected.
     */
    ObjectProperty<Consumer<List<? extends FileSource>>> onFilesSelectedProperty();
}
