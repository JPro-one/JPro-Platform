package one.jpro.platform.file;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Java file source.
 *
 * @author Besmir Beqiri
 */
public final class NativeFileSource extends FileSource {

    public NativeFileSource(final File file) {
        super(file);
    }

    @Override
    public File getPlatformFile() {
        return (File) super.getPlatformFile();
    }

    @Override
    String _getName() {
        return getPlatformFile().getName();
    }

    @Override
    long _getSize() {
        return getPlatformFile().length();
    }

    // progress property
    private ReadOnlyDoubleWrapper progress;

    @Override
    public double getProgress() {
        return progress == null ? 0.0 : progress.get();
    }

    private void setProgress(double value) {
        progressPropertyImpl().set(value);
    }

    @Override
    public ReadOnlyDoubleProperty progressProperty() {
        return progressPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyDoubleWrapper progressPropertyImpl() {
        if (progress == null) {
            progress = new ReadOnlyDoubleWrapper(this, "progress", 0.0);
        }
        return progress;
    }

    // uploadedFile property
    private ReadOnlyObjectWrapper<File> uploadedFile;

    @Override
    public File getUploadedFile() {
        return (uploadedFile == null) ? null : getPlatformFile();
    }

    private void setUploadedFile(File value) {
        uploadedFilePropertyImpl().set(value);
    }

    @Override
    public ReadOnlyObjectProperty<File> uploadedFileProperty() {
        return uploadedFilePropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<File> uploadedFilePropertyImpl() {
        if (uploadedFile == null) {
            uploadedFile = new ReadOnlyObjectWrapper<>(this, "uploadedFile");
        }
        return uploadedFile;
    }

    // uploadStatus property
    private ReadOnlyObjectWrapper<UploadStatus> uploadStatus;

    @Override
    public UploadStatus getUploadStatus() {
        return uploadStatus == null ? UploadStatus.NOT_STARTED : uploadStatus.get();
    }

    @Override
    public ReadOnlyObjectProperty<UploadStatus> uploadStatusProperty() {
        return uploadStatusPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<UploadStatus> uploadStatusPropertyImpl() {
        if (uploadStatus == null) {
            uploadStatus = new ReadOnlyObjectWrapper<>(this, "uploadStatus", UploadStatus.NOT_STARTED);
        }
        return uploadStatus;
    }

    @Override
    public void uploadFile() {
        final Runnable runnable = () -> {
            setProgress(1.0);
            setUploadedFile(getPlatformFile());
            uploadStatusPropertyImpl().set(UploadStatus.COMPLETED);
        };

        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

    /** A native file is available immediately, so there is never a running upload to cancel. */
    @Override
    public void cancelUpload() {
    }

    @Override
    public CompletableFuture<File> uploadFileAsync() {
        return CompletableFuture.supplyAsync(() -> {
            uploadFile();
            return getPlatformFile();
        });
    }
}
