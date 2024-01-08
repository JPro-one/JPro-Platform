package one.jpro.platform.auth.core.basic.provider;

import one.jpro.platform.auth.core.authentication.AuthenticationException;
import one.jpro.platform.auth.core.authentication.CredentialValidationException;
import one.jpro.platform.auth.core.authentication.User;
import one.jpro.platform.auth.core.basic.InMemoryUserManager;
import one.jpro.platform.auth.core.basic.UserManager;
import one.jpro.platform.auth.core.basic.UserNotFoundException;
import one.jpro.platform.auth.core.basic.UsernamePasswordCredentials;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static one.jpro.platform.auth.core.utils.AuthUtils.BCRYPT_PASSWORD_ENCODER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Basic authentication provider tests.
 *
 * @author Besmir Beqiri
 */
public class BasicAuthenticationProviderTests {

    private UserManager userManager;
    private BasicAuthenticationProvider basicAuthProvider;

    @BeforeEach
    public void setup() {
        userManager = new InMemoryUserManager();
        basicAuthProvider = new BasicAuthenticationProvider(userManager, Set.of("USER", "ADMIN"),
                Map.of("enabled", Boolean.TRUE));
    }

    @Test
    public void testAuthenticateWithValidCredentials() {
        // Creating user
        final UsernamePasswordCredentials credentials =
                new UsernamePasswordCredentials("validUser", "validPass");
        assertThat(userManager.createUser(credentials, null, null).join()).isNotNull();

        // Authenticating with valid credentials
        final User authenticatedUser = basicAuthProvider.authenticate(credentials).join();
        assertThat(authenticatedUser).isNotNull();

        final JSONObject userJSON = authenticatedUser.toJSON();
        assertThat(userJSON.getString(User.KEY_NAME)).isEqualTo(credentials.getUsername());
        assertThat(userJSON.getJSONArray(User.KEY_ROLES)).containsExactlyInAnyOrder("USER", "ADMIN");
        final JSONObject attributes = userJSON.getJSONObject(User.KEY_ATTRIBUTES);
        assertThat(attributes.getBoolean("enabled")).isTrue();
        final JSONObject authAttributes = attributes.getJSONObject("auth");
        assertThat(authAttributes.getString("username"))
                .isEqualTo(credentials.getUsername());
        assertThat(authAttributes.getString("password")).matches(encryptedPassword ->
                BCRYPT_PASSWORD_ENCODER.matches(credentials.getPassword(), encryptedPassword));
    }

    @Test
    public void testAuthenticateWithInvalidCredentials() {
        String username = "user";
        String password = "pass";
        String wrongPassword = "wrongPass";

        // Creating user
        assertThat(userManager.createUser(new UsernamePasswordCredentials(username, password),
                null, null).join()).isNotNull();

        // Authenticating with invalid credentials
        UsernamePasswordCredentials invalidCredentials = new UsernamePasswordCredentials(username, wrongPassword);

        assertThatThrownBy(() -> basicAuthProvider.authenticate(invalidCredentials).get())
                .hasRootCauseInstanceOf(AuthenticationException.class)
                .hasRootCauseMessage("Invalid username or password");
    }


    @Test
    public void testAuthenticateCredentialValidationException() {
        UsernamePasswordCredentials invalidFormatCredentials = new UsernamePasswordCredentials("", "");
        assertThatThrownBy(() -> basicAuthProvider.authenticate(invalidFormatCredentials).get())
                .hasCauseInstanceOf(CredentialValidationException.class);
    }

    @Test
    public void testAuthenticateUserNotFoundException() {
        final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("nonExistentUser", "pass");
        assertThatThrownBy(() -> basicAuthProvider.authenticate(credentials).get())
                .hasCauseInstanceOf(AuthenticationException.class)
                .hasRootCauseInstanceOf(UserNotFoundException.class)
                .hasMessageEndingWith("Invalid username");
    }


}
