package one.jpro.platform.image;

import org.json.JSONObject;

/**
 * This interface defines a contract for objects that can be converted to a JSON representation.
 *
 * @author Florian Kirmaier
 * @author Besmir Beqiri
 */
public interface JsonConvertible {

    /**
     * Converts the object to a JSON representation.
     *
     * @return A JSONObject representing the object.
     */
    JSONObject toJSON();
}