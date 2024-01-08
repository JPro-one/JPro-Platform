package one.jpro.platform.auth.core.basic;

import one.jpro.platform.auth.core.authentication.CredentialValidationException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static one.jpro.platform.auth.core.utils.AuthUtils.BASE64_ENCODER;
import static one.jpro.platform.auth.core.utils.AuthUtils.BCRYPT_PASSWORD_ENCODER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * UsernamePasswordCredentials tests.
 *
 * @author Besmir Beqiri
 */
public class UsernamePasswordCredentialsTests {

    @Test
    public void nullUsernameShouldMakeValidationThrowsException() {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(null, "password");

        assertThatExceptionOfType(CredentialValidationException.class)
                .isThrownBy(() -> credentials.validate(null))
                .withMessage("Username cannot be null or blank");
    }

    @Test
    public void blankUsernameShouldMakeValidationThrowsException() {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(" ", "password");

        assertThatExceptionOfType(CredentialValidationException.class)
                .isThrownBy(() -> credentials.validate(null))
                .withMessage("Username cannot be null or blank");
    }

    @Test
    public void missingPasswordShouldMakeValidationThrowsException() {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("username", null);

        assertThatExceptionOfType(CredentialValidationException.class)
                .isThrownBy(() -> credentials.validate(null))
                .withMessage("Password cannot be null");
    }

    @Test
    public void credentialsWithTheSameUsernameAndPasswordShouldBeEqual() {
        UsernamePasswordCredentials credentials1 =
                new UsernamePasswordCredentials("name@mail.com", "password");
        UsernamePasswordCredentials credentials2 =
                new UsernamePasswordCredentials("name@mail.com", "password");
        assertThat(credentials1).isEqualTo(credentials2);
    }

    @Test
    public void testHashCodeMethod() {
        UsernamePasswordCredentials credentials =
                new UsernamePasswordCredentials("some_username", "some_password");
        assertThat(credentials.hashCode()).isEqualTo(Objects.hash(credentials.getUsername(), credentials.getPassword()));
    }

    @Test
    public void testHttpAuthorizationMethod() {
        UsernamePasswordCredentials credentials =
                new UsernamePasswordCredentials("username", "password");

        String authHeader = "Basic " + BASE64_ENCODER.encodeToString((credentials.getUsername() + ":"
                + credentials.getPassword()).getBytes(StandardCharsets.UTF_8));
        assertThat(credentials.toHttpAuthorization()).isEqualTo(authHeader);
    }

    @Test
    public void toJSONMethodProvidesTheExpectedResult() {
        UsernamePasswordCredentials credentials =
                new UsernamePasswordCredentials("some_username", "some_password");

        JSONObject credentialsJSON = credentials.toJSON();
        assertThat(credentialsJSON.has("username")).isTrue();
        assertThat(credentialsJSON.has("password")).isTrue();
        assertThat(BCRYPT_PASSWORD_ENCODER.matches("some_password",
                credentialsJSON.getString("password"))).isTrue();

        JSONObject json = new JSONObject();
        json.put("username", credentials.getUsername());
        json.put("password", BCRYPT_PASSWORD_ENCODER.encode(credentials.getPassword()));

        assertThat(credentialsJSON.getString("username")).isEqualTo(json.getString("username"));
        assertThat(credentialsJSON.getString("password")).isNotEqualTo(json.getString("password"));
    }
}
