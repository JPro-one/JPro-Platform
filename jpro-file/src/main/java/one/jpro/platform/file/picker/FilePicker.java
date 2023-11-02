package one.jpro.platform.file.picker;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import one.jpro.platform.file.ExtensionFilter;

import java.io.File;

/**
 * A file picker interface for selecting and interacting with files.
 *
 * @author Besmir Beqiri
 */
public interface FilePicker {

    /**
     * Returns the associated node for this file picker.
     *
     * @return the associated node for this file picker.
     */
    Node getNode();

    /**
     * Gets the title of the displayed file dialog.
     *
     * @return the title string
     */
    String getTitle();

    /**
     * Sets the title of the displayed file dialog.
     *
     * @param value the title string
     */
    void setTitle(String value);

    /**
     * The title of the displayed file dialog.
     */
    StringProperty titleProperty();

    /**
     * The initial file name for the displayed dialog.
     *
     * @return the file name as a string
     */
    String getInitialFileName();

    /**
     * Sets the initial file name for the displayed dialog.
     *
     * @param value the file name
     */
    void setInitialFileName(final String value);

    /**
     * The initial file name for the displayed dialog.
     * <p>
     * This property is used mostly in the displayed file save dialogs as the
     * initial file name for the file being saved. If set for a file open
     * dialog it will have any impact on the displayed dialog only if the
     * corresponding platform provides support for such property in its
     * file open dialogs.
     * </p>
     */
    StringProperty initialFileNameProperty();

    /**
     * The initial directory for the displayed file dialog.
     *
     * @return the initial directory as a {@code File} object
     */
    File getInitialDirectory();

    /**
     * Sets the initial directory for the displayed file dialog.
     *
     * @param value the initial directory as a {@code File} object
     */
    void setInitialDirectory(File value);

    /**
     * Defines the initial directory for the displayed file dialog.
     */
    ObjectProperty<File> initialDirectoryProperty();

    /**
     * Gets the extension filters used in the displayed file dialog. Only
     * one extension filter from the list is active at any time in the displayed
     * dialog and only files which correspond to this extension filter are
     * shown. The first extension filter from the list is activated when the
     * dialog is invoked. Then the user can switch the active extension filter
     * to any other extension filter from the list and in this way control the
     * set of displayed files.
     *
     * @return An observable list of the extension filters used in the dialog
     */
    ObservableList<ExtensionFilter> getExtensionFilters();

    /**
     * Gets the extension filter which is currently selected in the displayed
     * file dialog.
     *
     * @return the selected extension filter or {@code null} if no extension
     */
    ExtensionFilter getSelectedExtensionFilter();

    /**
     * Sets the extension filter which is currently selected in the displayed
     * file dialog.
     *
     * @param value the selected extension filter
     */
    void setSelectedExtensionFilter(ExtensionFilter value);

    /**
     * This property is used to pre-select the extension filter for the next
     * displayed dialog and to read the user-selected extension filter from the
     * dismissed dialog.
     * <p>
     * When the file dialog is shown, the selectedExtensionFilter will be checked.
     * If the value of selectedExtensionFilter is null or is not contained in
     * the list of extension filters, then the first extension filter in the list
     * of extension filters will be selected instead. Otherwise, the specified
     * selectedExtensionFilter will be activated.
     * <p>
     * After the dialog is dismissed the value of this property is updated to
     * match the user-selected extension filter from the dialog.
     */
    ObjectProperty<ExtensionFilter> selectedExtensionFilterProperty();
}
