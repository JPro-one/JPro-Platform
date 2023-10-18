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

    @Override
    String _getObjectURL() {
        return getPlatformFile().toURI().toString();
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

    @Override
    public void uploadFile() {
        final Runnable runnable = () -> {
            setProgress(1.0);
            setUploadedFile(getPlatformFile());
        };

        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

    @Override
    public CompletableFuture<File> uploadFileAsync() {
        return CompletableFuture.supplyAsync(() -> {
            uploadFile();
            return getPlatformFile();
        });
    }
}
