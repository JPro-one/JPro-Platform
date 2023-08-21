package one.jpro.auth.test;

import one.jpro.auth.authentication.CredentialValidationException;
import one.jpro.auth.oath2.OAuth2Credentials;
import one.jpro.auth.oath2.OAuth2Flow;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OAuth2Credentials tests.
 *
 * @author Besmir Beqiri
 */
public class OAuth2CredentialsTest {

    @Test
    public void checkFields() {
        OAuth2Credentials credentials = new OAuth2Credentials()
                .setFlow(OAuth2Flow.AUTH_CODE)
                .setCode("NHSUYBrqQAAAF0wXgQZ")
                .setRedirectUri("http://localhost:8080/callback")
                .setCodeVerifier("some_code_verifier")
                .setJwt(new JSONObject().put("token", "some_token"))
                .setUsername("some_username")
                .setPassword("some_password")
                .setAssertion("some_assertion")
                .setScopes(List.of("scope1", "scope2"))
                .setNonce("12345");

        assertEquals(OAuth2Flow.AUTH_CODE, credentials.getFlow());
        assertEquals("NHSUYBrqQAAAF0wXgQZ", credentials.getCode());
        assertEquals("some_code_verifier", credentials.getCodeVerifier());
        assertEquals("http://localhost:8080/callback", credentials.getRedirectUri());
        assertEquals("some_token", credentials.getJwt().getString("token"));
        assertEquals("some_username", credentials.getUsername());
        assertEquals("some_password", credentials.getPassword());
        assertEquals("some_assertion", credentials.getAssertion());
        assertEquals(List.of("scope1", "scope2"), credentials.getScopes());
        assertEquals("12345", credentials.getNonce());
    }

    @Test
    public void nullOAuth2FlowShouldMakeValidationThrowsException() {
        OAuth2Credentials credentials = new OAuth2Credentials();

        Exception exception = assertThrowsExactly(CredentialValidationException.class,
                () -> credentials.validate(null));
        assertEquals("flow cannot be null", exception.getMessage());
    }

    @Test
    public void missingCodeWithAuthCodeFlowShouldMakeValidationThrowsException() {
        OAuth2Credentials credentials = new OAuth2Credentials();

        Exception exception = assertThrowsExactly(CredentialValidationException.class,
                () -> credentials.validate(OAuth2Flow.AUTH_CODE));
        assertEquals("code cannot be null or blank", exception.getMessage());

        credentials.setCode(" ");
        exception = assertThrowsExactly(CredentialValidationException.class,
                () -> credentials.validate(OAuth2Flow.AUTH_CODE));
        assertEquals("code cannot be null or blank", exception.getMessage());
    }

    @Test
    public void missingRedirectUriWithAuthCodeFlowShouldMakeValidationThrowsException() {
        OAuth2Credentials credentials = new OAuth2Credentials();
        credentials.setCode("some_code");
        credentials.setRedirectUri(" ");

        Exception exception = assertThrowsExactly(CredentialValidationException.class,
                () -> credentials.validate(OAuth2Flow.AUTH_CODE));
        assertEquals("redirectUri cannot be blank", exception.getMessage());
    }

    @Test
    public void missingUsernameWithPasswordFlowShouldMakeValidationThrowsException() {
        OAuth2Credentials credentials = new OAuth2Credentials();

        Exception exception = assertThrowsExactly(CredentialValidationException.class,
                () -> credentials.validate(OAuth2Flow.PASSWORD));
        assertEquals("username cannot be null or blank", exception.getMessage());

        credentials.setUsername(" ");
        exception = assertThrowsExactly(CredentialValidationException.class,
                () -> credentials.validate(OAuth2Flow.PASSWORD));
        assertEquals("username cannot be null or blank", exception.getMessage());
    }

    @Test
    public void missingPasswordWithPasswordFlowShouldMakeValidationThrowsException() {
        OAuth2Credentials credentials = new OAuth2Credentials();
        credentials.setUsername("some_username");

        Exception exception = assertThrowsExactly(CredentialValidationException.class,
                () -> credentials.validate(OAuth2Flow.PASSWORD));
        assertEquals("password cannot be null or blank", exception.getMessage());

        credentials.setPassword(" ");
        exception = assertThrowsExactly(CredentialValidationException.class,
                () -> credentials.validate(OAuth2Flow.PASSWORD));
        assertEquals("password cannot be null or blank", exception.getMessage());
    }

    @Test
    public void toJSONMethodProvidesTheExpectedResult() {
        OAuth2Credentials credentials = new OAuth2Credentials()
                .setFlow(OAuth2Flow.PASSWORD)
                .setCode("NHSUYBrqQAAAF0wXgQZ")
                .setRedirectUri("http://localhost:8080/callback")
                .setCodeVerifier("some_code_verifier")
                .setJwt(new JSONObject().put("token", "some_token"))
                .setUsername("some_username")
                .setPassword("some_password")
                .setAssertion("some_assertion")
                .setScopes(List.of("scope1", "scope2"))
                .setNonce("12345");

        JSONObject json = new JSONObject();
        json.put("flow", "password");
        json.put("code", "NHSUYBrqQAAAF0wXgQZ");
        json.put("redirect_uri", "http://localhost:8080/callback");
        json.put("code_verifier", "some_code_verifier");
        json.put("jwt", new JSONObject().put("token", "some_token"));
        json.put("username", "some_username");
        json.put("password", "some_password");
        json.put("assertion", "some_assertion");
        json.put("scopes", List.of("scope1", "scope2"));
        json.put("nonce", "12345");

        assertTrue(credentials.toJSON().similar(json));
    }
}
