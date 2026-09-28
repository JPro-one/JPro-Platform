package one.jpro.platform.file;

import com.jpro.webapi.WebAPI;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Web file source.
 *
 * @author Besmir Beqiri
 */
public final class WebFileSource extends FileSource {

    private final ReadOnlyObjectWrapper<UploadStatus> uploadStatus;

    public WebFileSource(WebAPI.JSFile jsFile) {
        super(jsFile);
        uploadStatus = new ReadOnlyObjectWrapper<>(this, "uploadStatus", toUploadStatus(jsFile.getUploadStatus()));
        jsFile.uploadStatusProperty().addListener((o, ov, nv) -> uploadStatus.set(toUploadStatus(nv)));
    }

    @Override
    public WebAPI.JSFile getPlatformFile() {
        return (WebAPI.JSFile) super.getPlatformFile();
    }

    @Override
    String _getName() {
        return getPlatformFile().getFilename();
    }

    @Override
    long _getSize() {
        return getPlatformFile().getFileSize();
    }

    @Override
    public double getProgress() {
        return getPlatformFile().getProgress();
    }

    @Override
    public ReadOnlyDoubleProperty progressProperty() {
        return getPlatformFile().progressProperty();
    }

    @Override
    public File getUploadedFile() {
        return getPlatformFile().getUploadedFile();
    }

    @Override
    public ReadOnlyObjectProperty<File> uploadedFileProperty() {
        return getPlatformFile().uploadedFileProperty();
    }

    @Override
    public UploadStatus getUploadStatus() {
        return uploadStatus.get();
    }

    @Override
    public ReadOnlyObjectProperty<UploadStatus> uploadStatusProperty() {
        return uploadStatus.getReadOnlyProperty();
    }

    private static UploadStatus toUploadStatus(WebAPI.UploadStatus status) {
        return status == null ? UploadStatus.NOT_STARTED : UploadStatus.valueOf(status.name());
    }

    @Override
    public void uploadFile() {
        getPlatformFile().uploadFile();
    }

    @Override
    public void cancelUpload() {
        getPlatformFile().cancelUpload();
    }

    @Override
    public CompletableFuture<File> uploadFileAsync() {
        final WebAPI.JSFile jsFile = getPlatformFile();
        jsFile.uploadFile();
        return jsFile.getUploadedFileFuture();
    }
}
