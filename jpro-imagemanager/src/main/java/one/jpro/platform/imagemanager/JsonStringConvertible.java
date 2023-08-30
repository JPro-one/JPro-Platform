package one.jpro.platform.imagemanager;

import org.json.JSONObject;

public interface JsonStringConvertible {

    /**
     * Converts the object to a JSON representation.
     *
     * @return A JSONObject representing the object.
     */
    JSONObject toJSON();
}