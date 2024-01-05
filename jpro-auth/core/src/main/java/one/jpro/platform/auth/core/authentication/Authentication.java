package one.jpro.platform.auth.core.authentication;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.security.Principal;
import java.util.*;

/**
 * Represents the state of an authentication.
 *
 * @author Besmir Beqiri
 */
public interface Authentication extends Principal {

    String KEY_NAME = "name";
    String KEY_ROLES = "roles";
    String KEY_ATTRIBUTES = "attributes";

    /**
     * Any additional attributes in the authentication.
     *
     * @return a {@link Map} containing the attributes;
     */
    @NotNull
    Map<String, Object> getAttributes();

    /**
     * Any roles associated with the authentication.
     *
     * @return a {@link Collection} of roles as string
     */
    @NotNull
    default Collection<String> getRoles() {
        return Collections.emptyList();
    }

    /**
     * Convert the authentication information to JSON format.
     *
     * @return a JSON object.
     */
    @NotNull
    default JSONObject toJSON() {
        final JSONObject json = new JSONObject();
        json.put(KEY_NAME, getName());
        json.put(KEY_ROLES, new JSONArray(getRoles()));
        json.put(KEY_ATTRIBUTES, new JSONObject(getAttributes()));
        return json;
    }

    /**
     * Build an {@link Authentication} instance for the user.
     *
     * @param username the user's name
     * @return an {@link Authentication} object
     */
    @NotNull
    static Authentication create(@NotNull String username) {
        return new User(username);
    }

    /**
     * Build an {@link Authentication} instance for the user.
     *
     * @param username the user's name
     * @param roles    the user's roles
     * @return an {@link Authentication} object
     */
    @NotNull
    static Authentication create(@NotNull String username,
                                 @NotNull Set<String> roles) {
        Objects.requireNonNull(roles, "User's roles are null.");
        return new User(username, roles);
    }

    /**
     * Build an {@link Authentication} instance for the user.
     *
     * @param username   the user's name
     * @param attributes the user's attributes
     * @return an {@link Authentication} object
     */
    @NotNull
    static Authentication create(@NotNull String username,
                                 @NotNull Map<String, Object> attributes) {
        Objects.requireNonNull(attributes, "User's attributes are null.");
        return new User(username, attributes);
    }

    /**
     * Builds an {@link Authentication} instance for the user.
     *
     * @param username   User's name
     * @param roles      User's roles
     * @param attributes User's attributes
     * @return an {@link Authentication} object
     */
    @NotNull
    static Authentication create(@NotNull String username,
                                 @Nullable Set<String> roles,
                                 @Nullable Map<String, Object> attributes) {
        return new User(username, roles, attributes);
    }

    /**
     * Builds an {@link Authentication} instance for the user from a {@link JSONObject}.
     *
     * @param json a {@link JSONObject} containing user's data.
     * @return an {@link Authentication} object
     */
    @NotNull
    static Authentication create(@NotNull JSONObject json) {
        return new User(json);
    }
}
