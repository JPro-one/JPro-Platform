package one.jpro.platform.file.picker;

import com.jpro.webapi.WebAPI;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import one.jpro.platform.file.ExtensionFilter;
import one.jpro.platform.file.FileSource;
import one.jpro.platform.file.picker.impl.NativeFilePicker;
import one.jpro.platform.file.picker.impl.WebFilePicker;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A file picker interface for selecting and interacting with files.
 * The {@link FilePicker} interface provides methods and properties
 * to customize the file picker UI, set initial directory, handle
 * file selection events, and more.
 *
 * @param <F> the type of the file source
 * @author Besmir Beqiri
 */
public interface FilePicker<F extends FileSource> {

    /**
     * Creates a file picker. If the application is running in a
     * browser via JPro server, then a web version of the file
     * picker is returned. If the application is not running inside
     * the browser than a desktop version is returned.
     *
     * @param node the associated node for this file picker
     * @return a {@link FilePicker} object.
     * @throws NullPointerException if the node is null
     */
    static FilePicker<? extends FileSource> create(Node node) {
        Objects.requireNonNull(node, "node must not be null");
        if (WebAPI.isBrowser()) {
            return new WebFilePicker(node);
        }
        return new NativeFilePicker(node);
    }

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

    /**
     * Returns the selection mode of the file picker.
     * The selection mode determines how the file dialog allows the user to select files.
     * <p>
     * The default value is {@link SelectionMode#SINGLE}.
     *
     * @return the selection mode of the file dialog
     */
    SelectionMode getSelectionMode();

    /**
     * Sets the selection mode of the file chooser.
     *
     * @param value The selection mode to be set. This should be one of the values
     *              defined in the SelectionMode enumeration. Possible values are
     *              {@link SelectionMode#SINGLE} or {@link SelectionMode#MULTIPLE}.
     */
    void setSelectionMode(SelectionMode value);

    /**
     * Defines the selection mode of the file chooser.
     * The default value is {@link SelectionMode#SINGLE}.
     */
    ObjectProperty<SelectionMode> selectionModeProperty();

    /**
     * Gets the handler to be called when the user selects files.
     *
     * @return the handler
     */
    Consumer<List<F>> getOnFilesSelected();

    /**
     * Sets the handler to be called when the user selects files.
     *
     * @param value the handler
     */
    void setOnFilesSelected(Consumer<List<F>> value);

    /**
     * Defines the handler to be called when the user selects files.
     * The handler returns the selected files or {@code null} if
     * no file has been selected.
     */
    ObjectProperty<Consumer<List<F>>> onFilesSelectedProperty();
}