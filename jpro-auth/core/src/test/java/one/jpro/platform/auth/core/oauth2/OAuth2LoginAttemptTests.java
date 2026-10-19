package one.jpro.platform.auth.core.oauth2;

import one.jpro.platform.auth.core.authentication.User;
import one.jpro.platform.auth.core.utils.AuthUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * State, PKCE and nonce are generated per login attempt in {@code authorizeUrl} and checked in the callback.
 */
public class OAuth2LoginAttemptTests {

    private FakeIdp idp;
    private OAuth2AuthenticationProvider provider;
    private String idTokenNonce;

    @BeforeEach
    void setUp() throws IOException {
        idp = new FakeIdp(request -> new FakeIdp.Response(200, "{\"access_token\":\"x\",\"token_type\":\"Bearer\""
                + (idTokenNonce == null ? "" : ",\"id_token\":\"" + unsignedJwt(idTokenPayload(idTokenNonce)) + "\"")
                + "}"));
        provider = new OAuth2AuthenticationProvider(null, new OAuth2Options()
                .setSite("http://idp.invalid")
                .setClientId("clientId")
                .setTokenPath("/token")
                .setAuthorizationPath("/auth")
                .setVerifyToken(false)
                .setHttpClientConfigurator(idp.viaProxy()));
    }

    @AfterEach
    void tearDown() throws IOException {
        idp.close();
    }

    private static OAuth2Credentials credentials() {
        return new OAuth2Credentials()
                .setRedirectUri("http://app.invalid/callback")
                .setScopes(List.of("openid", "email"));
    }

    private static Map<String, String> query(String url) {
        return AuthUtils.queryToJson(URI.create(url).getRawQuery()).toMap().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));
    }

    private Throwable failure(OAuth2Credentials credentials, Map<String, String> callback) {
        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> provider.authenticate(credentials, callback).get(10, TimeUnit.SECONDS));
        return ex.getCause();
    }

    private static String idTokenPayload(String nonce) {
        long now = System.currentTimeMillis() / 1000;
        return "{\"iss\":\"http://idp.invalid\",\"aud\":[\"clientId\"],\"sub\":\"1\",\"email\":\"test@example.com\",\"iat\":" + now
                + ",\"exp\":" + (now + 300) + ",\"nonce\":\"" + nonce + "\"}";
    }

    private static String unsignedJwt(String payload) {
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        return b64.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8)) + "."
                + b64.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + ".";
    }

    private static String s256(String verifier) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    @Test
    void authorizationUrlCarriesStatePkceAndNonce() {
        Map<String, String> url = query(provider.authorizationUrl(credentials()));

        assertTrue(url.get("state").length() >= 43, url.toString());
        assertTrue(url.get("nonce").length() >= 43, url.toString());
        assertEquals("S256", url.get("code_challenge_method"));
        assertNotNull(url.get("code_challenge"));
    }

    @Test
    void eachAttemptGetsFreshValues() {
        Map<String, String> first = query(provider.authorizationUrl(credentials()));
        Map<String, String> second = query(provider.authorizationUrl(credentials()));

        assertNotEquals(first.get("state"), second.get("state"));
        assertNotEquals(first.get("nonce"), second.get("nonce"));
        assertNotEquals(first.get("code_challenge"), second.get("code_challenge"));
    }

    @Test
    void callbackSendsTheVerifierOfItsAttemptAndChecksTheNonce() throws Exception {
        OAuth2Credentials credentials = credentials();
        Map<String, String> url = query(provider.authorizationUrl(credentials));
        idTokenNonce = url.get("nonce");

        User user = provider.authenticate(credentials, Map.of("code", "c", "state", url.get("state")))
                .get(10, TimeUnit.SECONDS);

        assertNotNull(user);
        String body = idp.requests().get(0).body();
        String verifier = AuthUtils.queryToJson(body).getString("code_verifier");
        assertEquals(url.get("code_challenge"), s256(verifier));
    }

    @Test
    void codeSetOnTheCredentialsIsExchangedWithoutCallback() throws Exception {
        idTokenNonce = "unused";
        provider.authenticate(credentials().setCode("own-code"), Map.of()).get(10, TimeUnit.SECONDS);

        assertEquals("own-code", AuthUtils.queryToJson(idp.requests().get(0).body()).getString("code"));
    }

    @Test
    void verifierSetOnTheCredentialsWinsOverTheAttempt() throws Exception {
        Map<String, String> url = query(provider.authorizationUrl(credentials()));
        idTokenNonce = url.get("nonce");

        provider.authenticate(credentials().setCodeVerifier("own-verifier"),
                Map.of("code", "c", "state", url.get("state"))).get(10, TimeUnit.SECONDS);

        assertEquals("own-verifier", AuthUtils.queryToJson(idp.requests().get(0).body()).getString("code_verifier"));
    }

    @Test
    void authorizationUrlLeavesTheCredentialsUnchanged() {
        OAuth2Credentials credentials = new OAuth2Credentials().setRedirectUri("/callback")
                .setScopes(List.of("openid"));
        String before = credentials.toJSON().toString();

        provider.authorizationUrl(credentials);

        assertEquals(before, credentials.toJSON().toString());
    }

    @Test
    void loginLeavesTheCredentialsUnchanged() throws Exception {
        OAuth2Credentials credentials = new OAuth2Credentials().setFlow(OAuth2Flow.PASSWORD)
                .setUsername("test").setPassword("secret");
        idTokenNonce = "unused";

        User user = provider.authenticate(credentials).get(10, TimeUnit.SECONDS);

        assertEquals("test@example.com", user.getName());
        assertEquals("test", credentials.getUsername());
    }

    @Test
    void wrongNonceInIdTokenFails() {
        OAuth2Credentials credentials = credentials();
        Map<String, String> url = query(provider.authorizationUrl(credentials));
        idTokenNonce = "replayed";

        Throwable cause = failure(credentials, Map.of("code", "c", "state", url.get("state")));

        assertTrue(cause.getMessage().contains("nonce"), cause.getMessage());
    }

    @Test
    void unknownStateFailsWithoutTokenRequest() {
        provider.authorizationUrl(credentials());

        Throwable cause = failure(credentials(), Map.of("code", "c", "state", "forged"));

        assertTrue(cause.getMessage().contains("state"), cause.getMessage());
        assertTrue(idp.requests().isEmpty());
    }

    @Test
    void codeWithoutStateFailsWithoutTokenRequest() {
        Throwable cause = failure(credentials(), Map.of("code", "c"));

        assertTrue(cause.getMessage().contains("state"), cause.getMessage());
        assertTrue(idp.requests().isEmpty());
    }

    @Test
    void anAttemptCanOnlyBeUsedOnce() throws Exception {
        OAuth2Credentials credentials = credentials();
        Map<String, String> url = query(provider.authorizationUrl(credentials));
        idTokenNonce = url.get("nonce");
        Map<String, String> callback = Map.of("code", "c", "state", url.get("state"));
        provider.authenticate(credentials, callback).get(10, TimeUnit.SECONDS);

        Throwable cause = failure(credentials, callback);

        assertTrue(cause.getMessage().contains("state"), cause.getMessage());
        assertEquals(1, idp.requests().size());
    }

    @Test
    void stateAndVerifierSetByTheAppAreKept() throws Exception {
        OAuth2Credentials credentials = credentials().setState("app-state").setCodeVerifier("app-verifier-" + "x".repeat(40));
        Map<String, String> url = query(provider.authorizationUrl(credentials));
        idTokenNonce = url.get("nonce");

        assertEquals("app-state", url.get("state"));
        assertEquals(s256(credentials.getCodeVerifier()), url.get("code_challenge"));
        provider.authenticate(credentials, Map.of("code", "c", "state", "app-state")).get(10, TimeUnit.SECONDS);
        assertEquals(credentials.getCodeVerifier(),
                AuthUtils.queryToJson(idp.requests().get(0).body()).getString("code_verifier"));
    }

    @Test
    void withoutOpenidScopeNoNonceIsSentOrChecked() throws Exception {
        OAuth2Credentials credentials = new OAuth2Credentials().setRedirectUri("http://app.invalid/callback");
        Map<String, String> url = query(provider.authorizationUrl(credentials));
        idTokenNonce = "anything";

        assertNull(url.get("nonce"));
        assertNotNull(provider.authenticate(credentials, Map.of("code", "c", "state", url.get("state")))
                .get(10, TimeUnit.SECONDS));
    }

    @Test
    void loginAttemptSurvivesTheCookieEncoding() {
        var attempt = new OAuth2AuthenticationProvider.LoginAttempt("verifier.with-dots", "nonce", 1234L);

        assertEquals(attempt, OAuth2AuthenticationProvider.LoginAttempt.decode(attempt.encode()));
        assertEquals(new OAuth2AuthenticationProvider.LoginAttempt("v", null, 5L),
                OAuth2AuthenticationProvider.LoginAttempt.decode(
                        new OAuth2AuthenticationProvider.LoginAttempt("v", null, 5L).encode()));
        assertNull(OAuth2AuthenticationProvider.LoginAttempt.decode("garbage"));
        assertNull(OAuth2AuthenticationProvider.LoginAttempt.decode("a.b.notanumber"));
    }
}
