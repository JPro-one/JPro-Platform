package one.jpro.platform.file;

import javafx.beans.InvalidationListener;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.Collection;

/**
 * Tracks the combined upload progress of several {@link FileSource}s, weighted by file size:
 * {@code uploadedSize / totalSize}. Add and remove files through {@link #getFiles()}.
 * Use it on the JavaFX application thread.
 *
 * <pre>{@code
 * FileUploadProgress uploads = new FileUploadProgress(fileSources);
 * progressBar.progressProperty().bind(uploads.progressProperty());
 * uploads.uploadAll();
 * }</pre>
 *
 * @author Florian Kirmaier
 */
public final class FileUploadProgress {

    private final ObservableList<FileSource> files = FXCollections.observableArrayList();
    private final ReadOnlyLongWrapper totalSize = new ReadOnlyLongWrapper(this, "totalSize", 0L);
    private final ReadOnlyLongWrapper uploadedSize = new ReadOnlyLongWrapper(this, "uploadedSize", 0L);
    private final ReadOnlyDoubleWrapper progress = new ReadOnlyDoubleWrapper(this, "progress", 0.0);
    private final ReadOnlyBooleanWrapper uploading = new ReadOnlyBooleanWrapper(this, "uploading", false);
    private final InvalidationListener fileListener = observable -> update();

    public FileUploadProgress() {
        files.addListener((ListChangeListener<FileSource>) change -> {
            while (change.next()) {
                change.getRemoved().forEach(this::unobserve);
                change.getAddedSubList().forEach(this::observe);
            }
            update();
        });
    }

    public FileUploadProgress(Collection<? extends FileSource> files) {
        this();
        this.files.setAll(files);
    }

    /**
     * The tracked files.
     */
    public ObservableList<FileSource> getFiles() {
        return files;
    }

    /**
     * The sum of the sizes of all tracked files in bytes.
     */
    public long getTotalSize() {
        return totalSize.get();
    }

    public ReadOnlyLongProperty totalSizeProperty() {
        return totalSize.getReadOnlyProperty();
    }

    /**
     * The uploaded bytes over all tracked files, derived from each file's progress.
     */
    public long getUploadedSize() {
        return uploadedSize.get();
    }

    public ReadOnlyLongProperty uploadedSizeProperty() {
        return uploadedSize.getReadOnlyProperty();
    }

    /**
     * {@code uploadedSize / totalSize} between 0.0 and 1.0; 0.0 when there are no files.
     */
    public double getProgress() {
        return progress.get();
    }

    public ReadOnlyDoubleProperty progressProperty() {
        return progress.getReadOnlyProperty();
    }

    /**
     * Whether at least one tracked file is currently uploading.
     */
    public boolean isUploading() {
        return uploading.get();
    }

    public ReadOnlyBooleanProperty uploadingProperty() {
        return uploading.getReadOnlyProperty();
    }

    /**
     * Starts the upload of every tracked file that isn't uploading or completed.
     */
    public void uploadAll() {
        for (FileSource file : files) {
            final UploadStatus status = file.getUploadStatus();
            if (status != UploadStatus.UPLOADING && status != UploadStatus.COMPLETED) {
                file.uploadFile();
            }
        }
    }

    /**
     * Cancels every running upload; see {@link FileSource#cancelUpload()}.
     */
    public void cancelAll() {
        files.forEach(FileSource::cancelUpload);
    }

    private void observe(FileSource file) {
        file.sizeProperty().addListener(fileListener);
        file.progressProperty().addListener(fileListener);
        file.uploadStatusProperty().addListener(fileListener);
    }

    private void unobserve(FileSource file) {
        file.sizeProperty().removeListener(fileListener);
        file.progressProperty().removeListener(fileListener);
        file.uploadStatusProperty().removeListener(fileListener);
    }

    private void update() {
        long total = 0;
        long uploaded = 0;
        boolean anyUploading = false;
        for (FileSource file : files) {
            final long size = file.getSize();
            final UploadStatus status = file.getUploadStatus();
            total += size;
            uploaded += status == UploadStatus.COMPLETED ? size : Math.round(size * file.getProgress());
            anyUploading |= status == UploadStatus.UPLOADING;
        }
        totalSize.set(total);
        uploadedSize.set(uploaded);
        progress.set(total == 0 ? 0.0 : Math.min(1.0, (double) uploaded / total));
        uploading.set(anyUploading);
    }
}
