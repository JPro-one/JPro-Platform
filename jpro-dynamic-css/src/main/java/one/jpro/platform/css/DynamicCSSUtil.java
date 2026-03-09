package one.jpro.platform.css;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Java-friendly API for applying dynamic CSS strings to a {@link Parent} or {@link Scene}.
 * <p>
 * CSS content is written to temporary files and registered as stylesheets.
 * Each call replaces the previous CSS for that target.
 * <p>
 * <strong>Usage:</strong>
 * <pre>{@code
 * DynamicCSSUtil.setCssString(scene, ".button { -fx-background-color: red; }");
 * // later:
 * DynamicCSSUtil.setCssString(scene, ".button { -fx-background-color: blue; }");
 * }</pre>
 */
public class DynamicCSSUtil {

    private static final String KEY_PROP = "DynamicCSSUtil.key";
    private static final String URL_PROP = "DynamicCSSUtil.url";

    private static final AtomicInteger counter = new AtomicInteger(0);
    private static final Map<String, File> files = new HashMap<>();
    private static final File tempDir;

    static {
        try {
            tempDir = File.createTempFile("dynamiccss", "");
            tempDir.delete();
            tempDir.mkdirs();
            tempDir.deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp dir for dynamic CSS", e);
        }
    }

    /**
     * Sets or updates a dynamic CSS string on a {@link Scene}.
     */
    public static void setCssString(Scene scene, String css) {
        removePrevious(scene.getProperties(), scene.getStylesheets());
        if (css != null && !css.isEmpty()) {
            addNew(css, scene.getProperties(), scene.getStylesheets());
        }
    }

    /**
     * Sets or updates a dynamic CSS string on a {@link Parent} node.
     */
    public static void setCssString(Parent parent, String css) {
        removePrevious(parent.getProperties(), parent.getStylesheets());
        if (css != null && !css.isEmpty()) {
            addNew(css, parent.getProperties(), parent.getStylesheets());
        }
    }

    private static void removePrevious(javafx.collections.ObservableMap<Object, Object> props,
                                       java.util.List<String> stylesheets) {
        String prevKey = (String) props.get(KEY_PROP);
        String prevURL = (String) props.get(URL_PROP);
        if (prevURL != null) {
            stylesheets.remove(prevURL);
            props.remove(KEY_PROP);
            props.remove(URL_PROP);
            Platform.runLater(() -> unregister(prevKey));
        }
    }

    private static void addNew(String css,
                               javafx.collections.ObservableMap<Object, Object> props,
                               java.util.List<String> stylesheets) {
        String key = "dcss" + counter.incrementAndGet();
        File file = new File(tempDir, key + ".css");
        try (PrintWriter w = new PrintWriter(file)) {
            w.write(css);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write CSS file", e);
        }
        file.deleteOnExit();
        synchronized (files) {
            files.put(key, file);
        }

        String url;
        try {
            url = file.toURI().toURL().toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        props.put(KEY_PROP, key);
        props.put(URL_PROP, url);
        stylesheets.add(url);
    }

    private static void unregister(String key) {
        synchronized (files) {
            File f = files.remove(key);
            if (f != null) f.delete();
        }
    }
}
