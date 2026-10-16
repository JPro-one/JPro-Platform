package one.jpro.platform.auth.core.oauth2;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link OAuth2AuthenticationProvider#authenticate(OAuth2Credentials, Map)}.
 */
public class OAuth2CallbackTests {

    private FakeIdp idp;
    private OAuth2AuthenticationProvider provider;

    @BeforeEach
    void setUp() throws IOException {
        idp = new FakeIdp(request -> new FakeIdp.Response(400, "{\"error\":\"invalid_grant\"}"));
        provider = new OAuth2AuthenticationProvider(null, new OAuth2Options()
                .setSite("http://idp.invalid")
                .setClientId("clientId")
                .setTokenPath("/token")
                .setHttpClientConfigurator(idp.viaProxy()));
    }

    @AfterEach
    void tearDown() throws IOException {
        idp.close();
    }

    private static OAuth2Credentials credentials() {
        return new OAuth2Credentials().setRedirectUri("http://app.invalid/callback");
    }

    private Throwable failure(OAuth2Credentials credentials, Map<String, String> callbackParams) {
        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> provider.authenticate(credentials, callbackParams).get(10, TimeUnit.SECONDS));
        return ex.getCause();
    }

    @Test
    void codeFromCallbackIsExchangedWithoutChangingCredentials() {
        OAuth2Credentials credentials = credentials().setState("s1");

        failure(credentials, Map.of("code", "callback-code", "state", "s1"));

        assertEquals(1, idp.requests().size(), idp.requestLines()::toString);
        FakeIdp.Request request = idp.requests().get(0);
        assertEquals("POST http://idp.invalid/token HTTP/1.1", request.line());
        assertTrue(request.body().contains("code=callback-code"), request.body());
        assertNull(credentials.getCode());
    }

    @Test
    void matchingStateIsAccepted() {
        failure(credentials().setState("s1"), Map.of("code", "c", "state", "s1"));

        assertEquals(1, idp.requests().size());
    }

    @Test
    void stateMismatchFailsWithoutTokenRequest() {
        Throwable cause = failure(credentials().setState("s1"), Map.of("code", "c", "state", "s2"));

        assertTrue(cause.getMessage().contains("state"), cause.getMessage());
        assertTrue(idp.requests().isEmpty());
    }

    @Test
    void missingStateFailsWhenExpected() {
        Throwable cause = failure(credentials().setState("s1"), Map.of("code", "c"));

        assertTrue(cause.getMessage().contains("state"), cause.getMessage());
        assertTrue(idp.requests().isEmpty());
    }

    @Test
    void idpErrorIsReported() {
        Throwable cause = failure(credentials(),
                Map.of("error", "access_denied", "error_description", "User cancelled"));

        assertEquals("access_denied: User cancelled", cause.getMessage());
        assertTrue(idp.requests().isEmpty());
    }

    @Test
    void scopesAreSplitOnSpaceAndPlus() {
        OAuth2Credentials credentials = credentials();

        assertEquals(List.of("openid", "email", "profile"),
                OAuth2AuthenticationProvider.applyCallbackParams(credentials,
                        Map.of("code", "c", "scope", "openid email+profile")).getScopes());
    }
}
