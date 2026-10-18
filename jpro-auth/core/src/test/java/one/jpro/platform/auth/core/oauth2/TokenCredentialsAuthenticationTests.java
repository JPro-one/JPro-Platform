package one.jpro.platform.auth.core.oauth2;

import io.jsonwebtoken.Jwts;
import one.jpro.platform.auth.core.authentication.User;
import one.jpro.platform.auth.core.jwt.TokenCredentials;
import one.jpro.platform.auth.core.oauth2.provider.GoogleAuthenticationProvider;
import one.jpro.platform.auth.core.utils.AuthUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code authenticate(TokenCredentials)} must only accept a token it could verify: a validly signed JWT, or a
 * token the identity provider reports as active for this client.
 */
public class TokenCredentialsAuthenticationTests {

    private static final String ISSUER = "http://idp.invalid";
    private static final KeyPair IDP_KEY = rsaKeyPair();
    private static final KeyPair OTHER_KEY = rsaKeyPair();

    private FakeIdp idp;
    private String introspection = "{\"active\":false}";
    private String jwks = "{\"keys\":[" + jwk((RSAPublicKey) IDP_KEY.getPublic(), "k1") + "]}";

    @BeforeEach
    void setUp() throws IOException {
        idp = new FakeIdp(request -> request.line().contains("/certs")
                ? new FakeIdp.Response(200, jwks)
                : new FakeIdp.Response(200, introspection));
    }

    @AfterEach
    void tearDown() throws IOException {
        idp.close();
    }

    private OAuth2Options options() {
        return new OAuth2Options()
                .setSite(ISSUER)
                .setClientId("clientId")
                .setClientSecret("secret")
                .setJwkPath("/certs")
                .setHttpClientConfigurator(idp.viaProxy());
    }

    private CompletableFuture<User> authenticate(OAuth2Options options, String token) {
        return new OAuth2AuthenticationProvider(null, options).authenticate(new TokenCredentials(token));
    }

    private static Throwable failure(CompletableFuture<User> result) {
        return assertThrows(ExecutionException.class, () -> result.get(10, TimeUnit.SECONDS)).getCause();
    }

    private static String jwt(KeyPair signer) {
        return jwt(signer, b -> b.audience().add("clientId").and());
    }

    private static String jwt(KeyPair signer, java.util.function.UnaryOperator<io.jsonwebtoken.JwtBuilder> change) {
        return change.apply(Jwts.builder().header().keyId("k1").and()
                        .issuer(ISSUER).subject("1").claim("email", "test@example.com")
                        .expiration(Date.from(Instant.now().plusSeconds(300))))
                .signWith(signer.getPrivate(), Jwts.SIG.RS256).compact();
    }

    @Test
    void validSignedJwtIsAccepted() throws Exception {
        User user = authenticate(options(), jwt(IDP_KEY)).get(10, TimeUnit.SECONDS);

        assertEquals("test@example.com", user.getName());
    }

    @Test
    void jwtSignedWithAnotherKeyIsRejectedWithoutIntrospection() {
        Throwable cause = failure(authenticate(options(), jwt(OTHER_KEY)));

        assertTrue(cause.getMessage().contains("Cannot verify the token"), cause.getMessage());
    }

    @Test
    void signedJwtIssuedToAnotherClientIsRejected() {
        Throwable cause = failure(authenticate(options(), jwt(IDP_KEY, b -> b.audience().add("otherClient").and())));

        assertTrue(cause.getMessage().contains("Cannot verify the token"), cause.getMessage());
    }

    @Test
    void signedJwtWithoutAudienceIsRejected() {
        failure(authenticate(options(), jwt(IDP_KEY, b -> b)));
    }

    @Test
    void signedJwtAuthorizedForThisClientIsAccepted() throws Exception {
        // Keycloak style: aud is another resource, azp is the client that requested the token
        User user = authenticate(options(), jwt(IDP_KEY, b -> b.audience().add("account").and().claim("azp", "clientId")))
                .get(10, TimeUnit.SECONDS);

        assertEquals("test@example.com", user.getName());
    }

    @Test
    void keycloakIdTokenIsRejected() {
        failure(authenticate(options(), jwt(IDP_KEY, b -> b.audience().add("clientId").and().claim("typ", "ID"))));
    }

    @Test
    void signedJwtWithOnlySubjectUsesIt() throws Exception {
        String token = Jwts.builder().header().keyId("k1").and().issuer(ISSUER).subject("user-42")
                .audience().add("clientId").and().expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(IDP_KEY.getPrivate(), Jwts.SIG.RS256).compact();

        assertEquals("user-42", authenticate(options(), token).get(10, TimeUnit.SECONDS).getName());
    }

    @Test
    void jwtForAnotherClientFallsBackToIntrospection() throws Exception {
        introspection = "{\"active\":true,\"client_id\":\"clientId\",\"username\":\"alice\"}";

        User user = authenticate(options().setIntrospectionPath("/introspect"),
                jwt(IDP_KEY, b -> b.audience().add("otherClient").and())).get(10, TimeUnit.SECONDS);

        assertEquals("alice", user.getName());
    }

    @Test
    void microsoftV1AccessTokenOfTheSameTenantIsAccepted() throws Exception {
        OAuth2Options options = options();
        options.getJWTOptions().setIssuer("https://login.microsoftonline.com/{tenantid}/v2.0");

        User user = authenticate(options, jwt(IDP_KEY, b -> b.issuer("https://sts.windows.net/tenant-1/")
                .claim("tid", "tenant-1").audience().add("clientId").and())).get(10, TimeUnit.SECONDS);

        assertEquals("test@example.com", user.getName());
    }

    @Test
    void microsoftV1AccessTokenOfAnotherTenantIsRejected() {
        OAuth2Options options = options();
        options.getJWTOptions().setIssuer("https://login.microsoftonline.com/{tenantid}/v2.0");

        failure(authenticate(options, jwt(IDP_KEY, b -> b.issuer("https://sts.windows.net/tenant-2/")
                .claim("tid", "tenant-1").audience().add("clientId").and())));
    }

    @Test
    void introspectedRefreshTokenIsRejected() {
        introspection = "{\"active\":true,\"client_id\":\"clientId\",\"username\":\"alice\",\"typ\":\"Refresh\"}";

        Throwable cause = failure(authenticate(options().setIntrospectionPath("/introspect"), "opaque"));

        assertTrue(cause.getMessage().contains("access token"), cause.getMessage());
    }

    @Test
    void opaqueTokenIsRejectedWithoutIntrospection() {
        failure(authenticate(options(), "any-opaque-token"));
    }

    @Test
    void unverifiedJwtIsRejectedWhenVerificationIsOff() {
        failure(authenticate(options().setVerifyToken(false), jwt(OTHER_KEY)));
    }

    @Test
    void activeIntrospectedTokenIsAccepted() throws Exception {
        introspection = "{\"active\":true,\"client_id\":\"clientId\",\"username\":\"alice\"}";

        User user = authenticate(options().setIntrospectionPath("/introspect"), "opaque").get(10, TimeUnit.SECONDS);

        assertEquals("alice", user.getName());
    }

    @Test
    void inactiveIntrospectedTokenIsRejected() {
        introspection = "{\"active\":false}";

        Throwable cause = failure(authenticate(options().setIntrospectionPath("/introspect"), "opaque"));

        assertTrue(cause.getMessage().contains("not active"), cause.getMessage());
    }

    @Test
    void introspectionWithoutActiveIsRejected() {
        introspection = "{\"client_id\":\"clientId\",\"username\":\"alice\"}";

        Throwable cause = failure(authenticate(options().setIntrospectionPath("/introspect"), "opaque"));

        assertTrue(cause.getMessage().contains("not active"), cause.getMessage());
    }

    @Test
    void tokenIssuedToAnotherClientIsRejected() {
        introspection = "{\"active\":true,\"client_id\":\"otherClient\",\"username\":\"alice\"}";

        Throwable cause = failure(authenticate(options().setIntrospectionPath("/introspect"), "opaque"));

        assertTrue(cause.getMessage().contains("client"), cause.getMessage());
    }

    @Test
    void googleTokenInfoIsAcceptedForThisClient() throws Exception {
        introspection = "{\"azp\":\"clientId\",\"aud\":\"clientId\",\"email\":\"g@example.com\",\"expires_in\":\"3599\"}";
        GoogleAuthenticationProvider google = new GoogleAuthenticationProvider(null, options()
                .setIntrospectionPath("http://idp.invalid/tokeninfo"));

        User user = google.authenticate(new TokenCredentials("ya29.opaque")).get(10, TimeUnit.SECONDS);

        assertEquals("g@example.com", user.getName());
    }

    @Test
    void googleTokenInfoSendsTheTokenInTheBody() throws Exception {
        introspection = "{\"azp\":\"clientId\",\"aud\":\"clientId\",\"email\":\"g@example.com\",\"expires_in\":\"3599\"}";
        GoogleAuthenticationProvider google = new GoogleAuthenticationProvider(null, options()
                .setIntrospectionPath("http://idp.invalid/tokeninfo"));

        google.authenticate(new TokenCredentials("ya29.opaque+/=")).get(10, TimeUnit.SECONDS);

        FakeIdp.Request request = idp.requests().get(idp.requests().size() - 1);
        assertEquals("POST http://idp.invalid/tokeninfo HTTP/1.1", request.line());
        assertEquals("ya29.opaque+/=", AuthUtils.queryToJson(request.body()).getString("access_token"));
    }

    @Test
    void googleTokenInfoForAnotherClientIsRejected() {
        introspection = "{\"azp\":\"otherClient\",\"aud\":\"otherClient\",\"email\":\"g@example.com\",\"expires_in\":\"3599\"}";
        GoogleAuthenticationProvider google = new GoogleAuthenticationProvider(null, options()
                .setIntrospectionPath("http://idp.invalid/tokeninfo"));

        failure(google.authenticate(new TokenCredentials("ya29.opaque")));
    }

    private long certRequests() {
        return idp.requestLines().stream().filter(line -> line.contains("/certs")).count();
    }

    @Test
    void signingKeysAreFetchedOnce() throws Exception {
        OAuth2AuthenticationProvider provider = new OAuth2AuthenticationProvider(null, options());

        provider.authenticate(new TokenCredentials(jwt(IDP_KEY))).get(10, TimeUnit.SECONDS);
        provider.authenticate(new TokenCredentials(jwt(IDP_KEY))).get(10, TimeUnit.SECONDS);

        assertEquals(1, certRequests());
    }

    @Test
    void rotatedSigningKeyIsFetched() throws Exception {
        OAuth2AuthenticationProvider provider = new OAuth2AuthenticationProvider(null, options());
        provider.authenticate(new TokenCredentials(jwt(IDP_KEY))).get(10, TimeUnit.SECONDS);
        jwks = "{\"keys\":[" + jwk((RSAPublicKey) OTHER_KEY.getPublic(), "k2") + "]}";
        provider.jwksRefetchDelay = java.time.Duration.ZERO;

        User user = provider.authenticate(new TokenCredentials(jwt(OTHER_KEY, b -> b.header().keyId("k2").and()
                .audience().add("clientId").and()))).get(10, TimeUnit.SECONDS);

        assertEquals("test@example.com", user.getName());
        assertEquals(2, certRequests());
    }

    @Test
    void unknownKeyIdDoesNotRefetchRightAway() throws Exception {
        OAuth2AuthenticationProvider provider = new OAuth2AuthenticationProvider(null, options());
        provider.authenticate(new TokenCredentials(jwt(IDP_KEY))).get(10, TimeUnit.SECONDS);

        failure(provider.authenticate(new TokenCredentials(jwt(IDP_KEY, b -> b.header().keyId("unknown").and()
                .audience().add("clientId").and()))));

        assertEquals(1, certRequests());
    }

    @Test
    void signingKeysOlderThanTheMaxAgeAreFetchedAgain() throws Exception {
        OAuth2AuthenticationProvider provider = new OAuth2AuthenticationProvider(null, options().setJwkMaxAge(0));

        provider.authenticate(new TokenCredentials(jwt(IDP_KEY))).get(10, TimeUnit.SECONDS);
        provider.authenticate(new TokenCredentials(jwt(IDP_KEY))).get(10, TimeUnit.SECONDS);

        assertEquals(2, certRequests());
    }

    private static String jwk(RSAPublicKey key, String kid) {
        return "{\"kty\":\"RSA\",\"kid\":\"" + kid + "\",\"use\":\"sig\",\"alg\":\"RS256\",\"n\":\"" + base64Url(key.getModulus())
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
