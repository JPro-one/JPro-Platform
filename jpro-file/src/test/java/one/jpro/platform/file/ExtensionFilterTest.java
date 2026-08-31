package one.jpro.platform.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ExtensionFilterTest {

    @TempDir
    Path dir;

    private File file(String name) throws IOException {
        return Files.createFile(dir.resolve(name)).toFile();
    }

    @Test
    void supportedExtensionsAreUnionWithoutDuplicates() {
        List<String> result = ExtensionFilter.toSupportedExtensions(List.of(
                ExtensionFilter.of("Images", ".png", ".jpg"),
                ExtensionFilter.of("Photos", ".jpg", ".jpeg")));
        assertEquals(List.of(".png", ".jpg", ".jpeg"), result);
    }

    @Test
    void anyOrDirectoryMeansNoRestriction() {
        assertEquals(List.of(), ExtensionFilter.toSupportedExtensions(List.of()));
        assertEquals(List.of(), ExtensionFilter.toSupportedExtensions(
                List.of(ExtensionFilter.of("Images", ".png"), ExtensionFilter.ANY)));
        assertEquals(List.of(), ExtensionFilter.toSupportedExtensions(
                List.of(ExtensionFilter.of("Images", ".png"), ExtensionFilter.DIRECTORY)));
    }

    @Test
    void acceptsIgnoresCaseAndHandlesAny() throws IOException {
        ExtensionFilter png = ExtensionFilter.of("Images", ".PNG");
        assertTrue(png.accepts(file("photo.png")));
        assertTrue(png.accepts(file("UPPER.PNG")));
        assertFalse(png.accepts(file("photo.jpg")));
        assertFalse(png.accepts(dir.toFile()));

        assertTrue(ExtensionFilter.ANY.accepts(file("anything.xyz")));
        assertFalse(ExtensionFilter.ANY.accepts(dir.toFile()));
        assertTrue(ExtensionFilter.DIRECTORY.accepts(dir.toFile()));
    }
}
