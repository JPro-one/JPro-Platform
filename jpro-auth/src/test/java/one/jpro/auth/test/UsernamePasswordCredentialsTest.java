package one.jpro.auth.test;

import one.jpro.auth.authentication.CredentialValidationException;
import one.jpro.auth.authentication.UsernamePasswordCredentials;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static one.jpro.auth.utils.AuthUtils.BASE64_ENCODER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UsernamePasswordCredentials tests.
 *
 * @author Besmir Beqiri
 */
public class UsernamePasswordCredentialsTest {

    @Test
    public void nullUsernameShouldMakeValidationThrowsException() {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(null, "password");

        Exception exception = Assertions.assertThrowsExactly(CredentialValidationException.class,
                () -> credentials.validate(null));
        assertEquals("username cannot be null or blank", exception.getMessage());
    }

    @Test
    public void blankUsernameShouldMakeValidationThrowsException() {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(" ", "password");

        Exception exception = Assertions.assertThrowsExactly(CredentialValidationException.class,
                () -> credentials.validate(null));
        assertEquals("username cannot be null or blank", exception.getMessage());
    }

    @Test
    public void missingPasswordShouldMakeValidationThrowsException() {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("username", null);

        Exception exception = Assertions.assertThrowsExactly(CredentialValidationException.class,
                () -> credentials.validate(null));
        assertEquals("password cannot be null", exception.getMessage());
    }

    @Test
    public void credentialsWithTheSameUsernameAndPasswordShouldBeEqual() {
        UsernamePasswordCredentials credentials1 =
                new UsernamePasswordCredentials("name@mail.com", "password");
        UsernamePasswordCredentials credentials2 =
                new UsernamePasswordCredentials("name@mail.com", "password");
        assertEquals(credentials1, credentials2);
    }

    @Test
    public void testHashCodeMethod() {
        UsernamePasswordCredentials credentials =
                new UsernamePasswordCredentials("some_username", "some_password");
        assertEquals(Objects.hash(credentials.getUsername(), credentials.getPassword()), credentials.hashCode());
    }

    @Test
    public void testHttpAuthorizationMethod() {
        UsernamePasswordCredentials credentials =
                new UsernamePasswordCredentials("username", "password");

        String authHeader = "Basic " + BASE64_ENCODER.encodeToString((credentials.getUsername() + ":"
                + credentials.getPassword()).getBytes(StandardCharsets.UTF_8));
        assertEquals(authHeader, credentials.toHttpAuthorization());
    }

    @Test
    public void toJSONMethodProvidesTheExpectedResult() {
        UsernamePasswordCredentials credentials =
                new UsernamePasswordCredentials("some_username", "some_password");

        JSONObject json = new JSONObject();
        json.put("username", credentials.getUsername());
        json.put("password", credentials.getPassword());

        assertTrue(credentials.toJSON().similar(json));
    }
}
