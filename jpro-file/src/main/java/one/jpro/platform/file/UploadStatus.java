package one.jpro.platform.file;

/**
 * The state of a {@link FileSource} upload.
 *
 * @author Florian Kirmaier
 */
public enum UploadStatus {
    /** {@link FileSource#uploadFile()} hasn't been called yet. */
    NOT_STARTED,
    /** The file is being transferred. */
    UPLOADING,
    /** The file was received; {@link FileSource#getUploadedFile()} is set. */
    COMPLETED,
    /** The transfer failed. {@link FileSource#uploadFile()} starts a new attempt. */
    FAILED,
    /** The transfer was stopped with {@link FileSource#cancelUpload()}. {@link FileSource#uploadFile()} starts a new attempt. */
    CANCELLED
}
