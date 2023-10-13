package one.jpro.platform.file;

import com.jpro.webapi.WebAPI;

/**
 * Web file source.
 *
 * @author Besmir Beqiri
 */
public final class WebFileSource extends FileSource<WebAPI.JSFile> {

    public WebFileSource(WebAPI.JSFile jsFile) {
        super(jsFile);
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


}
