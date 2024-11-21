package one.jpro.platform.file.util;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;

import java.util.Map;

/**
 * Utility class for working with nodes.
 *
 * @author Besmir Beqiri
 * @author Indrit Beqiri
 */
public interface NodeUtils {

    Object MULTI_FILE_UPLOADER_KEY = new Object();
    Object EVENT_HANDLER_KEY = new Object();

    /**
     * Retrieves the value of a property associated with the given key from a node.
     *
     * @param node         the node to retrieve the property value from
     * @param key          the key of the property to retrieve
     * @param defaultValue the default value to return if the property does not exist
     * @param <K>          the type of the key
     * @param <V>          the type of the value
     * @return the value of the property associated with the given key,
     *         or the default value if the property does not exist
     */
    static <K, V> V getPropertyValue(Node node, K key, V defaultValue) {
        final Map<Object, Object> nodeProperties = node.getProperties();

        // Check if property exists
        if (nodeProperties.containsKey(key)) {
            // Return the property value
            return (V) nodeProperties.get(key);
        } else {
            // Set the default value and return it
            nodeProperties.put(key, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Adds an event handler to the specified node for the given event type.
     * If an event handler for the event type is already added, it will not add another one.
     *
     * @param node         the node to which the event handler will be added
     * @param eventType    the type of the event to handle
     * @param eventHandler the event handler to be added
     * @param <T>          the type of the event
     */
    static <T extends Event> void addEventHandler(Node node, EventType<T> eventType,
                                                  EventHandler<? super T> eventHandler) {
        final Map<Object, Object> nodeProperties = node.getProperties();

        // Create the event handler key for the given event type
        final String eventTypeKey = EVENT_HANDLER_KEY + "_" + eventType.getName();

        // Check if the event handler is already added
        if (!nodeProperties.containsKey(eventTypeKey)) {
            node.addEventHandler(eventType, eventHandler);
            nodeProperties.put(eventTypeKey, eventHandler);
        }
    }
}
