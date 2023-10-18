package one.jpro.platform.file.util;

import javafx.scene.Node;

import java.util.Map;

/**
 * Utility class for working with nodes.
 *
 * @author Besmir Beqiri
 */
public interface NodeUtils {

    Object MULTI_FILE_UPLOADER_KEY = new Object();

    /**
     * Retrieves the value of a property associated with the given key from a node.
     *
     * @param node         the node to retrieve the property value from
     * @param key          the key of the property to retrieve
     * @param defaultValue the default value to return if the property does not exist
     * @param <K>          the type of the key
     * @param <V>          the type of the value
     * @return the value of the property associated with the given key,
     * or the default value if the property does not exist
     */
    static <K, V> V getPropertyValue(Node node, K key, V defaultValue) {
        Map<Object, Object> properties = node.getProperties();

        // Check if property exists
        if (properties.containsKey(key)) {
            // Return the property value
            return (V) properties.get(key);
        } else {
            // Set the default value and return it
            properties.put(key, defaultValue);
            return defaultValue;
        }
    }
}
