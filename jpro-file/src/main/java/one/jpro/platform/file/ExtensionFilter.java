package one.jpro.platform.file;

import javafx.stage.FileChooser;

import java.util.List;

/**
 * Creates an {@code ExtensionFilter} with the specified description
 * and the file name extensions.
 * <p>
 * File name extension should be specified in the {@code *.<extension>}
 * format.
 *
 * @param description the textual description for the filter
 * @param extensions  a list of the accepted file name extensions
 * @author Besmir Beqiri
 */
public record ExtensionFilter(String description, boolean allowDirectory, List<String> extensions) {

    public static final ExtensionFilter ANY = new ExtensionFilter("All Files", false, List.of("."));
    public static final ExtensionFilter DIRECTORY = new ExtensionFilter("Directory", true, List.of());

    /**
     * Compact constructor for {@code ExtensionFilter}.
     *
     * @throws NullPointerException     if the description or the extensions are {@code null}
     * @throws IllegalArgumentException if the description or the extensions are empty
     */
    public ExtensionFilter {
        validateArgs(description, extensions);
    }

    /**
     * Constructor for {@code ExtensionFilter} with a single extension.
     *
     * @param description the description of the filter
     * @param extension   the extension to filter
     * @throws NullPointerException     if the description or the extension are {@code null}
     * @throws IllegalArgumentException if the description or the extension are empty
     */
    public ExtensionFilter(String description, String... extension) {
        this(description, false, List.of(extension));
    }

    /**
     * Creates an {@code ExtensionFilter} with the specified description
     * and the file name extensions.
     * <p>
     * File name extension should be specified in the {@code *.<extension>} format.
     *
     * @param description the textual description for the filter
     * @param extensions  an array of the accepted file name extensions
     * @return the created {@code ExtensionFilter}
     * @throws NullPointerException     if the description or the extensions are {@code null}
     * @throws IllegalArgumentException if the description or the extensions are empty
     */
    public static ExtensionFilter of(String description, String... extensions) {
        return new ExtensionFilter(description, false, List.of(extensions));
    }

    /**
     * Creates an {@code ExtensionFilter} with the specified description
     * and the file name extensions.
     * <p>
     * File name extension should be specified in the {@code *.<extension>} format.
     *
     * @param description the textual description for the filter
     * @param extensions  an array of the accepted file name extensions
     * @return the created {@code ExtensionFilter}
     * @throws NullPointerException     if the description or the extensions are {@code null}
     * @throws IllegalArgumentException if the description or the extensions are empty
     */
    public static ExtensionFilter of(String description, boolean allowDirectory, String... extensions) {
        return new ExtensionFilter(description, allowDirectory, List.of(extensions));
    }

    /**
     * Converts this {@code ExtensionFilter} to a JavaFX {@link FileChooser.ExtensionFilter}.
     *
     * @return a corresponding {@link FileChooser.ExtensionFilter} instance
     */
    public static FileChooser.ExtensionFilter toJavaFXExtensionFilter(ExtensionFilter extensionFilter) {
        if (extensionFilter == null) return null;
        return new FileChooser.ExtensionFilter(extensionFilter.description(),
                extensionFilter.extensions().stream().map(ext -> "*" + ext).toList());
    }

    /**
     * Converts a JavaFX {@link FileChooser.ExtensionFilter} to an {@code ExtensionFilter}.
     *
     * @param extensionFilter the JavaFX {@link FileChooser.ExtensionFilter} to convert
     * @return the corresponding {@code ExtensionFilter} instance
     */
    public static ExtensionFilter fromJavaFXExtensionFilter(FileChooser.ExtensionFilter extensionFilter) {
        if (extensionFilter == null) return null;
        return new ExtensionFilter(extensionFilter.getDescription(), false,
                extensionFilter.getExtensions().stream()
                        .filter(ext -> ext.startsWith("*"))
                        .map(ext -> ext.substring(1)).toList());
    }

    /**
     * Validates the arguments.
     *
     * @param description the textual description for the filter
     * @param extensions  the accepted file name extensions
     * @throws NullPointerException     if the description or the extensions are {@code null}
     * @throws IllegalArgumentException if the description or the extensions are empty
     */
    private static void validateArgs(final String description, final List<String> extensions) {
        if (description == null) {
            throw new NullPointerException("Description must not be null");
        }

        if (description.isEmpty()) {
            throw new IllegalArgumentException("Description must not be empty");
        }

        if (extensions == null) {
            throw new NullPointerException("Extensions must not be null");
        }

        for (String extension : extensions) {
            if (extension == null) {
                throw new NullPointerException("Extension must not be null");
            }

            if (extension.isEmpty()) {
                throw new IllegalArgumentException("Extension must not be empty");
            }

            if (extension.startsWith("*")) {
                throw new IllegalArgumentException("Extension with regex is not supported");
            }
        }
    }
}
