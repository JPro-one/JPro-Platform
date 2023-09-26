package one.jpro.platform.openlink;

import com.jpro.webapi.WebAPI;
import javafx.stage.Stage;
import one.jpro.platform.openlink.impl.DesktopOpenLinkImpl;
import one.jpro.platform.openlink.impl.JProOpenLinkImpl;
import org.jetbrains.annotations.NotNull;

import java.net.URL;

/**
 * OpenLink interface for opening URLs.
 *
 * @author Besmir Beqiri
 */
public interface OpenLink {

    /**
     * Creates an instance of OpenLink based on the given application stage.
     *
     * @param stage the application stage
     * @return an instance of OpenLink
     */
    static OpenLink create(Stage stage) {
        if (WebAPI.isBrowser()) {
            WebAPI webAPI = WebAPI.getWebAPI(stage);
            return new JProOpenLinkImpl(webAPI);
        } else {
            return new DesktopOpenLinkImpl();
        }
    }

    /**
     * Opens the given URL in the browser.
     *
     * @param uri the URL to open
     */
    void openURL(@NotNull URL url);


    /**
     * Opens the given URL string in the browser.
     *
     * @param uri the URL string to open
     */
    void openURL(@NotNull String url);
}
