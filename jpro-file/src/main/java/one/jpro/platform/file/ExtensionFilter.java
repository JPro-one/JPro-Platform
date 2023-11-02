package one.jpro.platform.file;

import java.util.List;

/**
 * Creates an {@code ExtensionFilter} with the specified description
 * and the file name extensions.
 * <p>
 * File name extension should be specified in the {@code *.<extension>}
 * format.
 *
 * @param description the textual description for the filter
 * @param extensions a list of the accepted file name extensions
 * @author Besmir Beqiri
 */
public record ExtensionFilter(String description, List<String> extensions) {

    public static final ExtensionFilter ANY = new ExtensionFilter("All Files", List.of("."));

    /**
     * Compact constructor for {@code ExtensionFilter}.
     *
     * @throws NullPointerException if the description or the extensions are {@code null}
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
     *
     * @throws NullPointerException     if the description or the extension are {@code null}
     * @throws IllegalArgumentException if the description or the extension are empty
     */
    public ExtensionFilter(String description, String extension) {
        this(description, List.of(extension));
    }

    /**
     * Creates an {@code ExtensionFilter} with the specified description
     * and the file name extensions.
     * <p>
     * File name extension should be specified in the {@code *.<extension>} format.
     *
     * @param description the textual description for the filter
     * @param extensions an array of the accepted file name extensions
     * @throws NullPointerException if the description or the extensions are {@code null}
     * @throws IllegalArgumentException if the description or the extensions are empty
     * @return the created {@code ExtensionFilter}
     */
    public static ExtensionFilter of(String description, String... extensions) {
        return new ExtensionFilter(description, List.of(extensions));
    }

    /**
     * Creates an {@code ExtensionFilter} with the specified description
     * and the file name extension.
     * <p>
     * File name extension should be specified in the {@code *.<extension>} format.
     *
     * @param description the textual description for the filter
     * @param extension the accepted file name extension
     * @throws NullPointerException if the description or the extension is {@code null}
     * @throws IllegalArgumentException if the description or the extension is empty
     * @return the created {@code ExtensionFilter}
     */
    public static ExtensionFilter of(String description, String extension) {
        return new ExtensionFilter(description, extension);
    }

    /**
     * Validates the arguments.
     *
     * @param description the textual description for the filter
     * @param extensions the accepted file name extensions
     * @throws NullPointerException if the description or the extensions are {@code null}
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

        if (extensions.isEmpty()) {
            throw new IllegalArgumentException("At least one extension must be defined");
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
