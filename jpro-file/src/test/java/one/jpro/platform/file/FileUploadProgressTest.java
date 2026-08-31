package one.jpro.platform.file;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static one.jpro.platform.file.FxTestSupport.inFX;
import static org.junit.jupiter.api.Assertions.*;

public class FileUploadProgressTest {

    @TempDir
    Path dir;

    @BeforeAll
    static void startJavaFX() throws InterruptedException {
        FxTestSupport.startJavaFX();
    }

    private NativeFileSource file(String name, int size) throws IOException {
        Path p = dir.resolve(name);
        Files.write(p, new byte[size]);
        return new NativeFileSource(p.toFile());
    }

    @Test
    void progressIsWeightedBySize() throws IOException {
        NativeFileSource small = file("small.bin", 100);
        NativeFileSource large = file("large.bin", 300);
        inFX(() -> {
            FileUploadProgress uploads = new FileUploadProgress(List.of(small, large));
            assertEquals(400, uploads.getTotalSize());
            assertEquals(0, uploads.getUploadedSize());
            assertEquals(0.0, uploads.getProgress());
            assertFalse(uploads.isUploading());

            small.uploadFile();
            assertEquals(100, uploads.getUploadedSize());
            assertEquals(0.25, uploads.getProgress(), 1e-9);
            assertEquals(UploadStatus.COMPLETED, small.getUploadStatus());

            large.uploadFile();
            assertEquals(1.0, uploads.getProgress(), 1e-9);
        });
    }

    @Test
    void followsListChanges() throws IOException {
        NativeFileSource a = file("a.bin", 100);
        NativeFileSource b = file("b.bin", 100);
        inFX(() -> {
            FileUploadProgress uploads = new FileUploadProgress();
            assertEquals(0.0, uploads.getProgress());

            uploads.getFiles().add(a);
            a.uploadFile();
            assertEquals(1.0, uploads.getProgress(), 1e-9);

            uploads.getFiles().add(b);
            assertEquals(0.5, uploads.getProgress(), 1e-9);
            assertEquals(200, uploads.getTotalSize());

            uploads.getFiles().remove(a);
            assertEquals(0.0, uploads.getProgress(), 1e-9);
            assertEquals(100, uploads.getTotalSize());

            // a is no longer observed: its changes must not leak into the tracker
            uploads.getFiles().clear();
            assertEquals(0, uploads.getTotalSize());
        });
    }

    @Test
    void uploadAllSkipsCompletedFiles() throws IOException {
        NativeFileSource a = file("a.bin", 10);
        NativeFileSource b = file("b.bin", 10);
        inFX(() -> {
            FileUploadProgress uploads = new FileUploadProgress(List.of(a, b));
            a.uploadFile();
            uploads.uploadAll();
            assertEquals(UploadStatus.COMPLETED, a.getUploadStatus());
            assertEquals(UploadStatus.COMPLETED, b.getUploadStatus());
            assertEquals(1.0, uploads.getProgress(), 1e-9);
            uploads.cancelAll();
            assertEquals(1.0, uploads.getProgress(), 1e-9);
        });
    }
}
