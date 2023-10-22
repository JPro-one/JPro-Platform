package one.jpro.platform.file.event;

import javafx.scene.input.DataFormat;
import one.jpro.platform.file.FileSource;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data transfer class.
 *
 * @author Besmir Beqiri
 */
 public class DataTransfer {

    /**
     * Represents a single File.
     */
    public static final DataFormat FILE = new DataFormat("application/x-java-file");

    /**
     * Represents a List of File sources.
     */
    public static final DataFormat FILE_SOURCES = new DataFormat("application/x-java-file-source-list");

    /**
     * Represents a List of MIME types.
     */
    public static final DataFormat MIME_TYPES = new DataFormat("application/x-java-mime-type-list");

    private final Map<DataFormat, Object> data;

    public DataTransfer() {
        this.data = new HashMap<>();
    }

    public final Object getData(DataFormat format) {
        return data.get(format);
    }

    public final void putData(DataFormat format, Object value) {
        data.put(format, value);
    }

    public final void setData(Map<DataFormat, Object> map) {
        data.putAll(map);
    }

    public final void clearData() {
        data.clear();
    }

    /**
     * Tests whether there is any data of the given DataFormat type.
     * @param format the format type
     * @return true if there is data for this type
     */
    public final boolean hasData(DataFormat format) {
        return data.containsKey(format);
    }

    /**
     * Gets whether a {@link File} has been registered as data.
     *
     * @return true if <code>hasData(DataTransfer.FILE)</code> returns true, false otherwise
     */
    public final boolean hasFile() {
        return hasData(FILE);
    }

    /**
     * Gets the {@link File} which had previously been registered.
     * This is equivalent to invoking <code>getData(DataTransfer.FILE)</code>.
     * If no such entry exists, null is returned.
     *
     * @return The File source associated with {@link DataTransfer#FILE}, or null if
     * there is none.
     */
    public final File getFile() {
        return (File) data.get(FILE);
    }

    /**
     * Gets whether a List of {@link FileSource} has been registered as data.
     *
     * @return true if <code>hasData(DataTransfer.FILE_SOURCES)</code> returns true, false otherwise
     */
    public final boolean hasFileSources() {
        return hasData(FILE_SOURCES);
    }

    /**
     * Gets the List of {@link FileSource} which had previously been registered.
     * This is equivalent to invoking <code>getData(DataTransfer.FILE_SOURCES)</code>.
     * If no such entry exists, null is returned.
     *
     * @return The List of Files associated with {@link DataTransfer#FILE_SOURCES}, or null if
     * there is none.
     */
    @SuppressWarnings("unchecked")
    public final List<FileSource> getFileSources() {
        return (List<FileSource>) data.get(FILE_SOURCES);
    }

    /**
     * Gets whether a List of MIME types as strings has been registered as data.
     *
     * @return true if <code>hasData(DataTransfer.MIME_TYPES)</code> returns true, false otherwise
     */
    public final boolean hasMimeTypes() {
        return hasData(MIME_TYPES);
    }

    /**
     * Gets the List of MIME types as strings which had previously been registered.
     * This is equivalent to invoking <code>getData(DataTransfer.MIME_TYPES)</code>.
     * If no such entry exists, null is returned.
     *
     * @return The List of MIME types as strings associated with {@link DataTransfer#MIME_TYPES},
     * or null if there is none.
     */
    @SuppressWarnings("unchecked")
    public final List<String> getMimeTypes() {
        return (List<String>) data.get(MIME_TYPES);
    }
}
