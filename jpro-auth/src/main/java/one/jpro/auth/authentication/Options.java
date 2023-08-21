package one.jpro.auth.authentication;

import org.json.JSONObject;

/**
 * An Options object is used for configuration purpose.
 *
 * @author Besmir Beqiri
 */
public interface Options {

    /**
     * Convert all configuration information to JSON format.
     *
     * @return a JSON object.
     */
    JSONObject toJSON();
}
