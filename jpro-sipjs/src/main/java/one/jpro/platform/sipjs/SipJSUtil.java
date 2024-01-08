package one.jpro.platform.sipjs;

import com.jpro.webapi.WebAPI;

public class SipJSUtil {

    /**
     * Loads the sip.js library into the given webapi.
     * This method is used internally by jpro-sipjs.
     * This method is indempotent. Calling it multiple times will not load the library multiple times.
     * @param webapi
     */
    public static void loadSipJS(WebAPI webapi) {
        webapi.executeScript("window.root = typeof window !== 'undefined' ? window : this;");
        webapi.loadJSFile(SipJSUtil.class.getResource("sip-0.21.2.js"));
    }
}
