package one.jpro.platform.auth.core.utils;

import one.jpro.platform.auth.core.crypto.PasswordEncoder;
import one.jpro.platform.auth.core.crypto.bcrypt.BCryptPasswordEncoder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utility methods.
 *
 * @author Besmir Beqiri
 */
public interface AuthUtils {

    PasswordEncoder BCRYPT_PASSWORD_ENCODER = new BCryptPasswordEncoder();

    /**
     * Checks that the specified string is not {@code null} and
     * throws a customized {@link NullPointerException} if it is, or blank
     * and throws a customized {@link IllegalArgumentException} if it is.
     * This method is designed primarily for doing parameter validation in methods and
     * constructors with multiple parameters, as demonstrated below:
     * <blockquote><pre>
     * public Credentials(String username, String password) {
     *     this.username = Utils.requireNonNullOrBlank(username, "id must not be null or blank");
     *     this.password = Utils.requireNonNullOrBlank(password, "password must not be null or blank");
     * }
     * </pre></blockquote>
     *
     * @param str     the string to check for nullity
     * @param message detail message to be used in the event that a {@code
     *                NullPointerException} is thrown
     * @return {@code str} if not {@code null} or not blank
     * @throws NullPointerException     if {@code str} is {@code null}
     * @throws IllegalArgumentException if {@code str} is blank
     */
    static String requireNonNullOrBlank(String str, String message) {
        if (str == null)
            throw new NullPointerException(message);
        if (str.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return str;
    }

    /**
     * Converts a JSON object to a query string.
     *
     * @param json a JSON object
     * @return a query string
     */
    static String jsonToQuery(JSONObject json) {
        return json.toMap().entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().toString().isBlank())
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" +
                        URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    /**
     * Converts a query string to a JSON object.
     *
     * @param query a query string
     * @return a JSON object
     */
    static JSONObject queryToJson(String query) {
        if (query == null) {
            return null;
        }

        final JSONObject json = new JSONObject();
        final String[] pairs = query.split("&");
        for (String pair : pairs) {
            final int idx = pair.indexOf("=");
            final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8) : pair;
            final String value = (idx > 0 && pair.length() > idx + 1) ?
                    URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8) : null;
            if (!json.has(key)) {
                json.put(key, value);
            } else {
                var oldValue = json.get(key);
                JSONArray array;
                if (oldValue instanceof JSONArray) {
                    array = (JSONArray) oldValue;
                } else {
                    array = new JSONArray();
                    array.put(oldValue);
                    json.put(key, array);
                }

                array.put(Objects.requireNonNullElse(value, JSONObject.NULL));
            }
        }

        return json;
    }

    /**
     * Find the value of a key in a JSON object.
     *
     * @param json the JSON object
     * @param key the key to search for
     * @return the value of the key
     */
    static Object findValueByKey(JSONObject json, String key) {
        // Check if the current JSON object has the key.
        if (json.has(key)) {
            return json.get(key);
        }

        // If the key is not in the current object, search deeper.
        for (String currentKey : json.keySet()) {
            Object value = json.get(currentKey);

            if (value instanceof JSONObject) {
                // If it's a nested JSONObject, search recursively in this object.
                final Object result = findValueByKey((JSONObject) value, key);
                if (result != null) {
                    return result;
                }
            } else if (value instanceof JSONArray array) {
                // If it's a JSONArray, iterate through its elements.
                for (int i = 0; i < array.length(); i++) {
                    Object arrayElement = array.get(i);
                    if (arrayElement instanceof JSONObject) {
                        // If an element of the array is a JSONObject, search recursively in this object.
                        Object result = findValueByKey((JSONObject) arrayElement, key);
                        if (result != null) {
                            return result;
                        }
                    }
                }
            }
        }

        // If the key was not found in the current object or any of its sub objects, return null.
        return null;
    }

    /**
     * Checks if the specified headers contain the specified value.
     *
     * @param headers the HTTP headers
     * @param value   the value to check
     * @return <code>true</code> if the specified headers contain the specified value
     */
    static boolean containsValue(HttpHeaders headers, String value) {
        return headers.map().entrySet().stream()
                .flatMap(entry -> entry.getValue().stream())
                .anyMatch(s -> s.contains(value));
    }

    /**
     * Processes the non-standard headers.
     *
     * @param json           the JSON object
     * @param response       the HTTP response
     * @param scopeSeparator the scope separator
     */
    static void processNonStandardHeaders(JSONObject json, HttpResponse<String> response, String scopeSeparator) {
        // inspect the response header for the non-standard:
        // X-OAuth-Scopes and X-Accepted-OAuth-Scopes
        final var xOAuthScopes = response.headers().firstValue("X-OAuth-Scopes");
        final var xAcceptedOAuthScopes = response.headers().firstValue("X-OAuth-Scopes");

        xOAuthScopes.ifPresent(scopes -> {
            if (json.has("scope")) {
                json.put("scope", json.getString("scope") + scopeSeparator + scopes);
            } else {
                json.put("scope", scopes);
            }
        });

        xAcceptedOAuthScopes.ifPresent(scopes -> json.put("acceptedScopes", scopes));
    }

    /**
     * Extracts the error description from the specified JSON object.
     *
     * @param json the JSON object
     * @return the error description
     */
    static String extractErrorDescription(JSONObject json) {
        if (json == null) {
            return "null";
        }

        String description;
        if (json.get("error") instanceof JSONObject error) {
            description = error.getString("message");
        } else {
            description = json.optString("error_description", json.getString("error"));
        }

        if (description == null) {
            return "null";
        }

        return description;
    }

    /**
     * Decode percent encoded <code>String</code> values.
     *
     * @param str the percent encoded <code>String</code>
     * @return expanded form of the input, for example, "foo%20bar" becomes "foo bar"
     */
    static String decodePercent(String str) {
        return URLDecoder.decode(str, StandardCharsets.UTF_8);
    }
}
