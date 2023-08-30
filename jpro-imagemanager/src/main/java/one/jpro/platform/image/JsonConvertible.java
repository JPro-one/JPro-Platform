package one.jpro.platform.image;

import org.json.JSONObject;

public interface JsonConvertible {

    /**
     * Converts the object to a JSON representation.
     *
     * @return A JSONObject representing the object.
     */
    JSONObject toJSON();
}