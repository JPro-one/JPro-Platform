package one.jpro.platform.file;

import com.jpro.webapi.WebAPI;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static one.jpro.platform.file.FxTestSupport.inFX;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebFileSourceTest {

    @BeforeAll
    static void startJavaFX() throws InterruptedException {
        FxTestSupport.startJavaFX();
    }

    private static WebAPI.JSFile jsFile(long size, SimpleObjectProperty<WebAPI.UploadStatus> status,
                                        ReadOnlyDoubleWrapper progress) {
        WebAPI.JSFile jsFile = mock(WebAPI.JSFile.class);
        when(jsFile.getFilename()).thenReturn("file.bin");
        when(jsFile.getFileSize()).thenReturn(size);
        when(jsFile.uploadStatusProperty()).thenReturn(status);
        when(jsFile.getUploadStatus()).thenAnswer(inv -> status.get());
        when(jsFile.progressProperty()).thenReturn(progress.getReadOnlyProperty());
        when(jsFile.getProgress()).thenAnswer(inv -> progress.get());
        when(jsFile.uploadedFileProperty()).thenReturn(new ReadOnlyObjectWrapper<File>().getReadOnlyProperty());
        return jsFile;
    }

    @Test
    void everyStatusChangeReachesInvalidationObservers() {
        inFX(() -> {
            var status = new SimpleObjectProperty<>(WebAPI.UploadStatus.NOT_STARTED);
            var progress = new ReadOnlyDoubleWrapper(0.0);
            WebFileSource source = new WebFileSource(jsFile(100, status, progress));
            FileUploadProgress uploads = new FileUploadProgress(List.of(source));

            status.set(WebAPI.UploadStatus.UPLOADING);
            assertEquals(UploadStatus.UPLOADING, source.getUploadStatus());
            assertTrue(uploads.isUploading());

            // no progress event in between: the second status change alone must get through
            status.set(WebAPI.UploadStatus.CANCELLED);
            assertEquals(UploadStatus.CANCELLED, source.getUploadStatus());
            assertFalse(uploads.isUploading());
        });
    }

    @Test
    void progressIsWeightedAndResetOnCancel() {
        inFX(() -> {
            var status = new SimpleObjectProperty<>(WebAPI.UploadStatus.NOT_STARTED);
            var progress = new ReadOnlyDoubleWrapper(0.0);
            WebFileSource source = new WebFileSource(jsFile(200, status, progress));
            FileUploadProgress uploads = new FileUploadProgress(List.of(source));

            status.set(WebAPI.UploadStatus.UPLOADING);
            progress.set(0.5);
            assertEquals(100, uploads.getUploadedSize());
            assertEquals(0.5, uploads.getProgress(), 1e-9);

            status.set(WebAPI.UploadStatus.CANCELLED);
            progress.set(0.0);
            assertEquals(0, uploads.getUploadedSize());
            assertEquals(0.0, uploads.getProgress(), 1e-9);

            status.set(WebAPI.UploadStatus.COMPLETED);
            assertEquals(1.0, uploads.getProgress(), 1e-9);
        });
    }
}
