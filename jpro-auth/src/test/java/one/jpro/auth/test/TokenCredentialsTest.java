package one.jpro.auth.test;

import one.jpro.auth.authentication.CredentialValidationException;
import one.jpro.auth.authentication.User;
import one.jpro.auth.jwt.TokenCredentials;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TokenCredentials tests.
 *
 * @author Besmir Beqiri
 */
public class TokenCredentialsTest {

    private static final String TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    @Test
    public void nullTokenShouldMakeValidationThrowsException() {
        TokenCredentials credentials = new TokenCredentials((String) null);

        Exception exception = assertThrowsExactly(CredentialValidationException.class,
                () -> credentials.validate(null));
        assertEquals("token cannot be null or blank", exception.getMessage());
    }

    @Test
    public void blankTokenShouldMakeValidationThrowsException() {
        TokenCredentials credentials = new TokenCredentials(" ");

        Exception exception = assertThrowsExactly(CredentialValidationException.class,
                () -> credentials.validate(null));
        assertEquals("token cannot be null or blank", exception.getMessage());
    }

    @Test
    public void nullJSONObjectOnConstructorThrowsException() {
        Exception exception = assertThrowsExactly(IllegalStateException.class,
                () -> new TokenCredentials((JSONObject) null));
        assertEquals("json object cannot be null", exception.getMessage());
    }

    @Test
    public void emptyJSONObjectOnConstructorShouldMakeValidationThrowsException() {
        TokenCredentials credentials = new TokenCredentials(new JSONObject());

        Exception exception = assertThrowsExactly(CredentialValidationException.class,
                () -> credentials.validate(null));
        assertEquals("token cannot be null or blank", exception.getMessage());
    }

    @Test
    public void nonTokenBasedUserOnConstructorShouldMakeValidationThrowsException() {
        Exception exception = assertThrowsExactly(IllegalStateException.class,
                () -> {
                    User user = new User("Username", null, null);
                    new TokenCredentials(user);
                });
        assertEquals("json object cannot be null", exception.getMessage());
    }

    @Test
    public void credentialsWithTheSameUsernameAndPasswordShouldBeEqual() {
        TokenCredentials credentials1 = new TokenCredentials("name@mail.com");
        TokenCredentials credentials2 = new TokenCredentials("name@mail.com");
        assertEquals(credentials1, credentials2);
    }

    @Test
    public void testHashCodeMethod() {
        TokenCredentials credentials = new TokenCredentials(TOKEN);
        credentials.addScopes("email", "profile");
        assertEquals(Objects.hash(TOKEN, List.of("email", "profile")), credentials.hashCode());
    }

    @Test
    public void testHttpAuthorizationMethod() {
        TokenCredentials credentials = new TokenCredentials(TOKEN);

        assertEquals("Bearer " + TOKEN, credentials.toHttpAuthorization());
    }

    @Test
    public void toJSONMethodProvidesTheExpectedResult() {
        TokenCredentials credentials = new TokenCredentials(TOKEN);

        JSONObject json = new JSONObject();
        json.put("token", credentials.getToken());

        // test without scopes
        assertTrue(credentials.toJSON().similar(json));

        credentials.addScopes("email", "profile");
        json.put("scopes", new JSONArray(List.of("email", "profile")));

        // test with scopes
        assertTrue(credentials.toJSON().similar(json));
    }
}
