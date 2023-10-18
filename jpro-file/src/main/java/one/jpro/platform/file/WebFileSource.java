package one.jpro.platform.file;

import com.jpro.webapi.WebAPI;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Web file source.
 *
 * @author Besmir Beqiri
 */
public final class WebFileSource extends FileSource {

    public WebFileSource(WebAPI.JSFile jsFile) {
        super(jsFile);
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
    String _getObjectURL() {
        return getPlatformFile().getObjectURL().getName();
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
    public void uploadFile() {
        getPlatformFile().uploadFile();
    }

    @Override
    public CompletableFuture<File> uploadFileAsync() {
        return getPlatformFile().getUploadedFileFuture();
    }
}
