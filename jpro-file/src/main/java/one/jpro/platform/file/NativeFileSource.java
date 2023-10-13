package one.jpro.platform.file;

import java.io.File;

/**
 * Java file source.
 *
 * @author Besmir Beqiri
 */
public final class NativeFileSource extends FileSource<File> {

    public NativeFileSource(final File file) {
        super(file);
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
}
