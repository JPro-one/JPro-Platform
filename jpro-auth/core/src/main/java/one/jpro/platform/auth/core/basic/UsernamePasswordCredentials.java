package one.jpro.platform.auth.core.basic;

import io.jsonwebtoken.io.Encoders;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import one.jpro.platform.auth.core.authentication.CredentialValidationException;
import one.jpro.platform.auth.core.authentication.Credentials;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

import static one.jpro.platform.auth.core.utils.AuthUtils.BCRYPT_PASSWORD_ENCODER;

/**
 * Username and password credentials holder.
 *
 * @author Besmir Beqiri
 */
public class UsernamePasswordCredentials implements Credentials {

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
        setUsername(username);
        setPassword(password);
    }

    // Username property
    private StringProperty username;

    @Nullable
    public String getUsername() {
        return (username == null) ? null : username.get();
    }

    public void setUsername(@NotNull String username) {
        usernameProperty().set(username);
    }

    @NotNull
    public final StringProperty usernameProperty() {
        if (username == null) {
            username = new SimpleStringProperty(this, "username");
        }
        return username;
    }

    // Password property
    private StringProperty password;

    @Nullable
    public String getPassword() {
        return (password == null) ? null : password.get();
    }

    public final void setPassword(@NotNull String password) {
        passwordProperty().set(password);
    }

    @NotNull
    public final StringProperty passwordProperty() {
        if (password == null) {
            password = new SimpleStringProperty(this, "password");
        }
        return password;
    }

    @Override
    public <V> void validate(V arg) throws CredentialValidationException {
        if (getUsername() == null || getUsername().isBlank()) {
            throw new CredentialValidationException("Username cannot be null or blank");
        }
        // passwords are allowed to be empty
        // for example this is used by basic auth
        if (getPassword() == null) {
            throw new CredentialValidationException("Password cannot be null");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsernamePasswordCredentials that = (UsernamePasswordCredentials) o;
        return Objects.equals(getUsername(), that.getUsername()) && Objects.equals(getPassword(), that.getPassword());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUsername(), getPassword());
    }

    @Override
    public String toHttpAuthorization() {
        final var result = new StringBuilder();

        final String username = getUsername();
        if (username != null) {
            // RFC check
            if (username.indexOf(':') != -1) {
                throw new IllegalArgumentException("Username cannot contain ':'");
            }
            result.append(username);
        }

        result.append(':');

        final String password = getPassword();
        if (password != null) {
            result.append(password);
        }

        return "Basic " + Encoders.BASE64URL.encode(result.toString().getBytes(StandardCharsets.UTF_8));
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
