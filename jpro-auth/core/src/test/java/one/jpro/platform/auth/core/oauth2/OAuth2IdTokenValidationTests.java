package one.jpro.platform.auth.core.oauth2;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import one.jpro.platform.auth.core.authentication.User;
import one.jpro.platform.auth.core.jwt.JWTOptions;
import one.jpro.platform.auth.core.jwt.TokenExpiredException;
import one.jpro.platform.auth.core.utils.AuthUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An ID token that fails validation (signature, issuer, audience, expiry) must fail the login.
 */
public class OAuth2IdTokenValidationTests {

    private static final String ISSUER = "http://idp.invalid";
    private static final KeyPair IDP_KEY = rsaKeyPair();
    private static final KeyPair OTHER_KEY = rsaKeyPair();

    private FakeIdp idp;
    private OAuth2AuthenticationProvider provider;
    private String idToken;
    private String accessToken = accessToken();

    @BeforeEach
    void setUp() throws IOException {
        idp = new FakeIdp(request -> request.line().contains("/certs")
                ? new FakeIdp.Response(200, "{\"keys\":[" + jwk((RSAPublicKey) IDP_KEY.getPublic()) + "]}")
                : new FakeIdp.Response(200, "{\"token_type\":\"Bearer\",\"access_token\":\"" + accessToken
                + "\",\"id_token\":\"" + idToken + "\"}"));
        provider = new OAuth2AuthenticationProvider(null, new OAuth2Options()
                .setSite(ISSUER)
                .setClientId("clientId")
                .setTokenPath("/token")
                .setAuthorizationPath("/auth")
                .setJwkPath("/certs")
                .setJWTOptions(new JWTOptions().setIssuer(ISSUER))
                .setHttpClientConfigurator(idp.viaProxy()));
    }

    @AfterEach
    void tearDown() throws IOException {
        idp.close();
    }

    /** Logs in with an ID token built from a valid one, changed by {@code change}. */
    private java.util.concurrent.CompletableFuture<User> login(UnaryOperator<JwtBuilder> change, KeyPair signer) {
        OAuth2Credentials credentials = new OAuth2Credentials()
                .setRedirectUri("http://app.invalid/callback")
                .setScopes(List.of("openid", "email"));
        Map<String, Object> url = AuthUtils.queryToJson(URI.create(provider.authorizationUrl(credentials)).getRawQuery()).toMap();
        JwtBuilder token = Jwts.builder()
                .header().keyId("k1").and()
                .issuer(ISSUER)
                .audience().add("clientId").and()
                .subject("1")
                .claim("email", "test@example.com")
                .claim("nonce", url.get("nonce"))
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(300)));
        idToken = change.apply(token).signWith(signer.getPrivate(), Jwts.SIG.RS256).compact();
        return provider.authenticate(credentials, Map.of("code", "c", "state", (String) url.get("state")));
    }

    private Throwable failure(UnaryOperator<JwtBuilder> change, KeyPair signer) {
        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> login(change, signer).get(10, TimeUnit.SECONDS));
        return ex.getCause();
    }

    @Test
    void validIdTokenLogsIn() throws Exception {
        User user = login(t -> t, IDP_KEY).get(10, TimeUnit.SECONDS);

        assertEquals("test@example.com", user.getName());
    }

    @Test
    void idTokenSignedWithAnotherKeyFails() {
        Throwable cause = failure(t -> t, OTHER_KEY);

        assertTrue(cause.getMessage().startsWith("Invalid ID token"), cause.getMessage());
    }

    @Test
    void idTokenWithoutIssuerFails() {
        Throwable cause = failure(t -> t.issuer(null), IDP_KEY);

        assertTrue(cause.getMessage().contains("issuer"), cause.getMessage());
    }

    @Test
    void microsoftTenantPlaceholderInIssuerIsResolvedFromTid() throws Exception {
        provider.getOptions().getJWTOptions().setIssuer("http://idp.invalid/{tenantid}/v2.0");

        User user = login(t -> t.issuer("http://idp.invalid/tenant-1/v2.0").claim("tid", "tenant-1"), IDP_KEY)
                .get(10, TimeUnit.SECONDS);
        assertEquals("test@example.com", user.getName());

        Throwable cause = failure(t -> t.issuer("http://idp.invalid/tenant-2/v2.0").claim("tid", "tenant-1"), IDP_KEY);
        assertTrue(cause.getMessage().contains("issuer"), cause.getMessage());
    }

    @Test
    void notBeforeSlightlyInTheFutureIsToleratedAsClockSkew() throws Exception {
        User user = login(t -> t.notBefore(Date.from(Instant.now().plusSeconds(20))), IDP_KEY).get(10, TimeUnit.SECONDS);

        assertEquals("test@example.com", user.getName());
    }

    @Test
    void idTokenFromAnotherIssuerFails() {
        Throwable cause = failure(t -> t.issuer("http://evil.invalid"), IDP_KEY);

        assertTrue(cause.getMessage().contains("issuer"), cause.getMessage());
    }

    @Test
    void idTokenForAnotherClientFails() {
        Throwable cause = failure(t -> t.audience().single("otherClient"), IDP_KEY);

        assertTrue(cause.getMessage().contains("audience"), cause.getMessage());
    }

    @Test
    void expiredIdTokenFails() {
        Throwable cause = failure(t -> t.expiration(Date.from(Instant.now().minusSeconds(600))), IDP_KEY);

        assertInstanceOf(TokenExpiredException.class, cause);
    }

    @Test
    void googleLikeResponseWithOpaqueAccessTokenLogsIn() throws Exception {
        provider.getOptions().getJWTOptions().setIssuer("https://accounts.google.com");
        accessToken = "ya29.opaque-google-access-token";

        User user = login(t -> t.issuer("accounts.google.com"), IDP_KEY).get(10, TimeUnit.SECONDS);

        assertEquals("test@example.com", user.getName());
    }

    @Test
    void keycloakFromTheFluentApiChecksTheRealmIssuer() throws Exception {
        provider = new one.jpro.platform.auth.core.api.FluentKeycloakAuthAPI()
                .site("http://idp.invalid/realms/{realm}")
                .realm("test")
                .clientId("clientId")
                .clientSecret("secret")
                .configureHttpClient(idp.viaProxy())
                .create(null);
        assertEquals("http://idp.invalid/realms/test", provider.getOptions().getJWTOptions().getIssuer());

        Throwable cause = failure(t -> t.issuer("http://evil.invalid/realms/test"), IDP_KEY);
        assertTrue(cause.getMessage().contains("issuer"), cause.getMessage());

        User user = login(t -> t.issuer("http://idp.invalid/realms/test"), IDP_KEY).get(10, TimeUnit.SECONDS);
        assertEquals("test@example.com", user.getName());
    }

    @Test
    void accessTokenThatFailsVerificationDoesNotBlockTheLogin() throws Exception {
        accessToken = Jwts.builder().header().keyId("k1").and().issuer("http://other-issuer.invalid").subject("1")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(IDP_KEY.getPrivate(), Jwts.SIG.RS256).compact();

        User user = login(t -> t, IDP_KEY).get(10, TimeUnit.SECONDS);

        assertEquals("test@example.com", user.getName());
    }

    @Test
    void idTokenWithSingleStringAudienceLogsIn() throws Exception {
        User user = login(t -> t.audience().single("clientId"), IDP_KEY).get(10, TimeUnit.SECONDS);

        assertEquals("test@example.com", user.getName());
    }

    // A valid, signed access token carrying the email: a forged ID token can't fail for lack of a user name.
    private static String accessToken() {
        return Jwts.builder().header().keyId("k1").and()
                .issuer(ISSUER).subject("1").claim("email", "test@example.com")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(IDP_KEY.getPrivate(), Jwts.SIG.RS256).compact();
    }

    private static String jwk(RSAPublicKey key) {
        return "{\"kty\":\"RSA\",\"kid\":\"k1\",\"use\":\"sig\",\"alg\":\"RS256\",\"n\":\"" + base64Url(key.getModulus())
                + "\",\"e\":\"" + base64Url(key.getPublicExponent()) + "\"}";
    }

    private static String base64Url(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes[0] == 0) bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static KeyPair rsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
