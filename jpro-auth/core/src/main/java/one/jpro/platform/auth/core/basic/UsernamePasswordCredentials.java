package one.jpro.platform.auth.core.basic;

import one.jpro.platform.auth.core.authentication.CredentialValidationException;
import one.jpro.platform.auth.core.authentication.Credentials;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

import static one.jpro.platform.auth.core.utils.AuthUtils.BASE64_ENCODER;
import static one.jpro.platform.auth.core.utils.AuthUtils.BCRYPT_PASSWORD_ENCODER;

/**
 * Username and password credentials holder.
 *
 * @author Besmir Beqiri
 */
public class UsernamePasswordCredentials implements Credentials {

    @Nullable
    private String username;

    @Nullable
    private String password;

    /**
     * Default constructor.
     */
    public UsernamePasswordCredentials() {
    }

    /**
     * Constructor with username and password.
     *
     * @param username the user's name
     * @param password the user's password
     */
    public UsernamePasswordCredentials(@NotNull String username, @NotNull String password) {
        this.username = username;
        this.password = password;
    }

    @Nullable
    public String getUsername() {
        return username;
    }

    public void setUsername(@NotNull String username) {
        this.username = username;
    }

    @Nullable
    public String getPassword() {
        return password;
    }

    public void setPassword(@NotNull String password) {
        this.password = password;
    }

    @Override
    public <V> void validate(V arg) throws CredentialValidationException {
        if (username == null || username.isBlank()) {
            throw new CredentialValidationException("Username cannot be null or blank");
        }
        // passwords are allowed to be empty
        // for example this is used by basic auth
        if (password == null) {
            throw new CredentialValidationException("Password cannot be null");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsernamePasswordCredentials that = (UsernamePasswordCredentials) o;
        return Objects.equals(username, that.username) && Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password);
    }

    @Override
    public String toHttpAuthorization() {
        final var result = new StringBuilder();

        if (username != null) {
            // RFC check
            if (username.indexOf(':') != -1) {
                throw new IllegalArgumentException("Username cannot contain ':'");
            }
            result.append(username);
        }

        result.append(':');

        if (password != null) {
            result.append(password);
        }

        return "Basic " + BASE64_ENCODER.encodeToString(result.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        Optional.ofNullable(getUsername()).ifPresent(username -> json.put("username", username));
        Optional.ofNullable(getPassword())
                .ifPresent(password -> json.put("password", BCRYPT_PASSWORD_ENCODER.encode(password)));
        return json;
    }
}
