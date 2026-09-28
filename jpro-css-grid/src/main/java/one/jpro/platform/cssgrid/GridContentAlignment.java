package one.jpro.platform.cssgrid;

/**
 * Distribution of the grid tracks inside the container (CSS {@code justify-content}, {@code align-content}).
 * <p>
 * {@code STRETCH} distributes free space to {@code auto} tracks; when there are none it behaves like {@code START}.
 */
public enum GridContentAlignment {
    START,
    END,
    CENTER,
    STRETCH,
    SPACE_BETWEEN,
    SPACE_AROUND,
    SPACE_EVENLY
}
