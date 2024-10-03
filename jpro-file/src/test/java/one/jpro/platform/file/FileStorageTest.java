package one.jpro.platform.file;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the FileStorage utility class.
 *
 * @author Besmir Beqiri
 */
class FileStorageTest {

    @TempDir
    static Path tempDir;

    /**
     * Ensures the JPRO_TMP_DIR exists before any tests are run.
     */
    @BeforeAll
    static void setup() {
        assertTrue(Files.exists(FileStorage.JPRO_TMP_DIR));
    }

    /**
     * Tests that a temporary file can be created with default parameters.
     */
    @Test
    void testCreateTempFileWithDefaults() throws IOException {
        Path tempFile = FileStorage.createTempFile(null, null, null);
        assertNotNull(tempFile);
        assertTrue(Files.exists(tempFile), "File should exist");
        assertTrue(tempFile.getFileName().toString().endsWith(".tmp"), "File should have .tmp extension");
    }

    /**
     * Tests that a temporary file can be created in a custom directory.
     */
    @Test
    void testCreateTempFileInCustomDir() throws IOException {
        Path customDir = tempDir.resolve("custom");
        Files.createDirectories(customDir);

        Path tempFile = FileStorage.createTempFile(customDir, "testfile", ".test");
        assertNotNull(tempFile);
        assertTrue(Files.exists(tempFile), "File should exist in custom directory");
        assertEquals(customDir, tempFile.getParent(), "File should be created in the custom directory");
        assertTrue(tempFile.getFileName().toString().endsWith(".test"), "File should have .test extension");
    }

    /**
     * Tests that a temporary file can be created with POSIX permissions.
     * Only runs on POSIX-compliant file systems.
     */
    @Test
    void testCreateTempFileWithPosixPermissions() throws IOException {
        if (FileStorage.isPosix) {
            Path posixFile = FileStorage.createTempFile(tempDir, "posixfile", ".posix");

            assertTrue(Files.exists(posixFile), "File should exist");
            assertTrue(Files.getPosixFilePermissions(posixFile).containsAll(PosixFilePermissions.fromString("rw-------")),
                    "File should have OWNER_READ and OWNER_WRITE permissions");
        }
    }

    /**
     * Tests that a file is created with a different file extension.
     */
    @Test
    void testCreateTempFileWithDifferentExtension() throws IOException {
        Path tempFile = FileStorage.createTempFile(null, "examplefile", ".dat");

        assertNotNull(tempFile);
        assertTrue(Files.exists(tempFile), "File should exist");
        assertTrue(tempFile.getFileName().toString().endsWith(".dat"), "File should have .dat extension");
    }

    /**
     * Tests that generatePath throws an IllegalArgumentException when the file name contains directory separators.
     */
    @Test
    void testGeneratePathWithInvalidFileName() {
        Path customDir = tempDir.resolve("custom");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            FileStorage.createTempFile(customDir, "invalid/file/name", ".test");
        });

        assertTrue(exception.getMessage().contains("Invalid fileName"), "Exception message should indicate invalid file name");
    }

    /**
     * Tests that createTempFile returns the path if the file already exists.
     */
    @Test
    void testCreateTempFileFileExists() throws IOException {
        Path existingFile = FileStorage.createTempFile(tempDir, "existingfile", ".ext");
        Path duplicateFile = FileStorage.createTempFile(tempDir, "existingfile", ".ext");

        assertEquals(existingFile, duplicateFile, "The method should return the existing file's path");
    }
}
