package one.jpro.auth.authentication;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

/**
 * An implementation of the {@link Authentication} interface to
 * be used on the server side to create authentication objects
 * from user data.
 *
 * @author Besmir Beqiri
 */
public class User implements Authentication {

    @NotNull
    private final String name;

    @NotNull
    private final Set<String> roles;

    @NotNull
    private final Map<String, Object> attributes;

    /**
     * Create a server authentication holding user's name, roles and attributes.
     *
     * @param name       The name the authenticated user
     * @param roles      Roles of the authenticated user
     * @param attributes Attributes of the authenticated user
     */
    public User(@NotNull String name,
                @Nullable Set<String> roles,
                @Nullable Map<String, Object> attributes) {
        Objects.requireNonNull(name, "User's name is null.");
        this.name = name;
        this.roles = (roles == null || roles.isEmpty()) ? Collections.emptySet() : roles;
        this.attributes = attributes == null ? Collections.emptyMap() : attributes;
    }

    public User(@NotNull JSONObject json) {
        String username = json.optString(KEY_NAME); // check with name key
        if (username == null || username.isBlank()) {
            username = json.optString("username"); // check with username key
            if (username == null || username.isBlank()) {
                throw new AuthenticationException("User's name is null.");
            }
        }

        name = username;

        if (json.has(KEY_ROLES)) {
            this.roles = json.getJSONArray(KEY_ROLES).toList().stream().map(Object::toString)
                    .collect(Collectors.toUnmodifiableSet());
        } else {
            this.roles = Collections.emptySet();
        }

        if (json.has(KEY_ATTRIBUTES)) {
            this.attributes = json.getJSONObject(KEY_ATTRIBUTES).toMap();
        } else {
            this.attributes = Collections.emptyMap();
        }
    }

    @Override
    @NotNull
    public String getName() {
        return name;
    }

    @Override
    @NotNull
    @Unmodifiable
    public Set<String> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    @Override
    @NotNull
    @Unmodifiable
    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public boolean hasRole(String role) {
        return getRoles().contains(role);
    }

    public boolean hasAttribute(String key) {
        return hasKey(toJSON().getJSONObject(KEY_ATTRIBUTES), key);
    }

    private boolean hasKey(JSONObject json, String key) {
        boolean exists = json.has(key);
        if (!exists) {
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String nextKey = keys.next();
                if (json.get(nextKey) instanceof JSONObject) {
                    exists = hasKey(json.getJSONObject(nextKey), key);
                }
            }
        }
        return exists;
    }
}
