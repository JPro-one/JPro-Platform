package one.jpro.platform.file.dropper;

import javafx.scene.layout.Pane;
import one.jpro.platform.file.ExtensionFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NativeFileDropperTest {

    @TempDir
    File tempDir;

    private File folder;
    private File txt;
    private File png;

    @BeforeEach
    void createFiles() throws IOException {
        folder = new File(tempDir, "folder");
        assertTrue(folder.mkdir());
        txt = new File(tempDir, "notes.txt");
        assertTrue(txt.createNewFile());
        png = new File(tempDir, "image.png");
        assertTrue(png.createNewFile());
    }

    @Test
    public void directoriesAreIgnoredByDefault() {
        NativeFileDropper dropper = new NativeFileDropper(new Pane());
        assertFalse(dropper.isAcceptDirectories());
        assertEquals(List.of(txt, png), dropper.acceptedFiles(List.of(folder, txt, png)));
        assertFalse(dropper.hasAcceptedFile(List.of(folder)));
    }

    @Test
    public void directoriesAndFilteredFilesCanBeMixed() {
        NativeFileDropper dropper = new NativeFileDropper(new Pane());
        dropper.setAcceptDirectories(true);
        dropper.setExtensionFilter(ExtensionFilter.of("Text", ".txt"));
        assertEquals(List.of(folder, txt), dropper.acceptedFiles(List.of(folder, txt, png)));
        assertTrue(dropper.hasAcceptedFile(List.of(folder)));
        assertFalse(dropper.hasAcceptedFile(List.of(png)));
    }

    @Test
    public void directoriesWithoutFilterAcceptEverything() {
        NativeFileDropper dropper = new NativeFileDropper(new Pane());
        dropper.setAcceptDirectories(true);
        assertEquals(List.of(folder, txt, png), dropper.acceptedFiles(List.of(folder, txt, png)));
    }
}
