package one.jpro.platform.file;

import javafx.beans.property.ObjectProperty;
import javafx.scene.control.SelectionMode;

import java.util.List;
import java.util.function.Consumer;

/**
 * Multiple file selector interface.
 *
 * @author Besmir Beqiri
 */
public interface MultipleFileSelector {

    /**
     * Returns the selection mode.
     * <p>
     * The default value is {@link SelectionMode#SINGLE}.
     *
     * @return the selection mode of the file dialog
     */
    SelectionMode getSelectionMode();

    /**
     * Sets the selection mode.
     *
     * @param value The selection mode to be set. This should be one of the values
     *              defined in the SelectionMode enumeration. Possible values are
     *              {@link SelectionMode#SINGLE} or {@link SelectionMode#MULTIPLE}.
     */
    void setSelectionMode(SelectionMode value);

    /**
     * Defines the selection mode. The selection mode determines how
     * the file dialog allows the user to select files.
     */
    ObjectProperty<SelectionMode> selectionModeProperty();

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
