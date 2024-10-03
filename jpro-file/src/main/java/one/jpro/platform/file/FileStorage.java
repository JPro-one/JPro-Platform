package one.jpro.platform.file;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;

/**
 * Utility class for file storage operations.
 *
 * @author Besmir Beqiri
 */
public final class FileStorage {

    /**
     * Determines if the default file system supports POSIX file attribute views.
     */
    static final boolean isPosix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

    /**
     * The directory path to store temporary files. Defaults to a 'tmp' directory within a '.jpro' directory
     * in the user's home directory.
     */
    public static final Path JPRO_TMP_DIR = Path.of(System.getProperty("user.home"), ".jpro", "tmp");

    /**
     * Inner class to hold the default POSIX file and directory permissions.
     * Permissions are OWNER_READ and OWNER_WRITE.
     */
    private static class PosixPermissions {
        static final FileAttribute<Set<PosixFilePermission>> filePermissions =
                PosixFilePermissions.asFileAttribute(EnumSet.of(OWNER_READ, OWNER_WRITE));
    }

    /**
     * Generates a path by appending the specified file type to the file name and resolving it against
     * the provided directory path.
     *
     * @param fileName the base file name.
     * @param fileType the file extension or type to append to the file name.
     * @param dir      the directory path to resolve the file name against.
     * @return A resolved Path that combines the directory and the modified file name.
     * @throws IllegalArgumentException If the generated name is not a simple file name, i.e., if it has a parent path.
     */
    private static Path generatePath(String fileName, String fileType, Path dir) {
        String s = fileName;
        int lastIndexOf = fileName.lastIndexOf('.');
        if (lastIndexOf > 0) {
            s = fileName.substring(0, lastIndexOf) + fileType;
        }
        Path name = dir.getFileSystem().getPath(s);
        // the generated name should be a simple file name
        if (name.getParent() != null)
            throw new IllegalArgumentException("Invalid fileName or fileType");
        return dir.resolve(name);
    }

    /**
     * Creates a temporary file in the specified directory with the provided file name and type.
     * If any of the parameters are null, it falls back to default values.
     *
     * @param dir      the directory in which to create the file,
     *                 if null, the directory defaults to {@link #JPRO_TMP_DIR}
     * @param fileName the name of the file to create. Defaults to "filename" if null
     * @param fileType the file extension or type. Defaults to ".tmp" if null
     * @return a Path object representing the created file
     * @throws IOException if an I/O error occurs or the file already exists
     */
    public static Path createTempFile(@Nullable Path dir,
                                      @Nullable String fileName,
                                      @Nullable String fileType) throws IOException {
        dir = Objects.requireNonNullElse(dir, JPRO_TMP_DIR);
        fileName = Objects.requireNonNullElse(fileName, "filename");
        fileType = Objects.requireNonNullElse(fileType, ".tmp");

        FileAttribute<?>[] attrs = null;
        if (isPosix && (dir.getFileSystem() == FileSystems.getDefault())) {
            attrs = new FileAttribute<?>[1];
            attrs[0] = PosixPermissions.filePermissions;
        }

        Path path = generatePath(fileName, fileType, dir);
        return Files.createFile(path, attrs);
    }

    private FileStorage() {
        // no-op
    }
}
