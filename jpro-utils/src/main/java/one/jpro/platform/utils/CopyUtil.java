package one.jpro.platform.utils;

import com.jpro.webapi.JSVariable;
import com.jpro.webapi.WebAPI;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CopyUtil {

    private static final Logger logger = LoggerFactory.getLogger(CopyUtil.class.getName());
    // Unique key for storing the copy handler instance in a node’s properties
    private static final Object COPY_HANDLER_KEY = new Object();

    /**
     * Sets up the given node so that clicking it copies the provided text.
     * Only one copy handler instance (desktop or JPro) is created per Node.
     *
     * @param node the JavaFX node to set the copy behavior on
     * @param text the text to copy to the clipboard when the node is clicked
     */
    public static void setCopyOnClick(Node node, String text) {
        CopyHandler handler = (CopyHandler) node.getProperties().get(COPY_HANDLER_KEY);
        if (handler == null) {
            if (WebAPI.isBrowser()) {
                handler = new CopyJPro(text, node);
            } else {
                handler = new CopyDesktop(text, node);
            }
            node.getProperties().put(COPY_HANDLER_KEY, handler);
        } else {
            handler.setText(text);
        }
        logger.debug("setCopyOnClick: " + node + ", " + text);
    }

    /**
     * Common interface for copy handlers.
     */
    private interface CopyHandler {
        void setText(String text);
    }

    /**
     * Implementation for desktop: registers a mouse-click event handler that copies the text
     * to the system clipboard.
     */
    private static class CopyDesktop implements CopyHandler {
        private String text;

        public CopyDesktop(String text, Node node) {
            this.text = text;
            install(node);
        }

        @Override
        public void setText(String text) {
            this.text = text;
        }

        private void install(Node node) {
            node.setOnMouseClicked(event -> {
                if(this.text != null) {
                    Clipboard clipboard = Clipboard.getSystemClipboard();
                    ClipboardContent content = new ClipboardContent();
                    content.putString(this.text);
                    clipboard.setContent(content);
                }
            });
        }
    }

    /**
     * Implementation for JPro (browser): uses the JPro WebAPI to register a JavaScript event handler
     * on the underlying DOM element so that clicking the node copies the text using the Clipboard API.
     */
    private static class CopyJPro implements CopyHandler {
        private final SimpleStringProperty copyTextProperty = new SimpleStringProperty();

        public CopyJPro(String text, Node node) {
            setText(text);
            install(node);
        }

        @Override
        public void setText(String text) {
            copyTextProperty.set(text);
        }

        private void install(Node node) {
            WebAPI.getWebAPI(node, webapi -> {
                final JSVariable jsElem = webapi.getElement(node);
                Runnable run = () -> {
                    String origText = copyTextProperty.get();
                    if(origText == null) {
                        String script = jsElem.getName() + ".onmousedown = undefined;";
                    } else {
                        String escapedText = copyTextProperty.get()
                                .replace("'", "\\'")
                                .replace("\"", "\\\"")
                                .replace("\n", "\\n");
                        String script = jsElem.getName() + ".onmousedown = function(event) {"
                                + "  console.log('copy: " + escapedText + "');"
                                + "  navigator.clipboard.writeText('" + escapedText + "');"
                                + "};";
                        webapi.executeScript(script);
                    }
                };
                copyTextProperty.addListener((observable, oldValue, newValue) -> {
                    run.run();
                });
                run.run();
            });
        }
    }
}
