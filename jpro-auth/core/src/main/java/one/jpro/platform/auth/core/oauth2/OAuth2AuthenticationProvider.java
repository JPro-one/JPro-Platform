package one.jpro.platform.auth.core.oauth2;

import com.jpro.webapi.WebAPI;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.Jwks;
import javafx.stage.Stage;
import one.jpro.platform.auth.core.authentication.*;
import one.jpro.platform.auth.core.basic.UsernamePasswordCredentials;
import one.jpro.platform.auth.core.http.HttpServer;
import one.jpro.platform.auth.core.jwt.JWTOptions;
import one.jpro.platform.auth.core.jwt.TokenCredentials;
import one.jpro.platform.auth.core.jwt.TokenExpiredException;
import one.jpro.platform.auth.core.oauth2.provider.OpenIDAuthenticationProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

/**
 * Provides an OAuth2-based {@link AuthenticationProvider} that supports various OAuth 2.0 flows (e.g., AUTH_CODE,
 * PASSWORD, CLIENT, etc.). This class handles interaction with an OAuth2 authorization server, optionally
 * starting a local HTTP server to capture callbacks for redirect-based flows.
 * <p>
 * Typical usage involves:
 * <ol>
 *   <li>Creating an instance with the desired {@link OAuth2Options}</li>
 *   <li>Directing the end-user to the authorization URL ({@link #authorizeUrl(OAuth2Credentials)}) if needed</li>
 *   <li>Using {@link #authenticate(Credentials)} to acquire a {@link User} object</li>
 * </ol>
 *
 * <p>
 * This class can also handle optional tasks such as token introspection, token refresh, token revocation,
 * user information retrieval, and logout.
 *
 * @author Besmir Beqiri
 */
public class OAuth2AuthenticationProvider implements AuthenticationProvider<Credentials> {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationProvider.class);

    @Nullable
    private final Stage stage;
    @NotNull
    private final OAuth2API api;
    @NotNull
    private final OAuth2Options options;

    private HttpServer httpServer;

    // an unknown key ID refetches the key set at most this often
    Duration jwksRefetchDelay = Duration.ofSeconds(10);
    private Map<String, Key> jwks;
    private Instant jwksFetchedAt;

    /**
     * Creates an OAuth2 authentication provider.
     * <p>
     * This constructor sets up the internal {@link HttpServer} to handle redirect-based OAuth2 flows,
     * especially if {@link #authorizeUrl(OAuth2Credentials)} is used to open a browser flow.
     *
     * @param stage the JavaFX application stage (maybe {@code null} if you are running in a non-JavaFX environment)
     * @param api   the OAuth2 API utility, which must not be {@code null}
     * @throws NullPointerException     if {@code api} is {@code null}
     * @throws IllegalArgumentException if any mandatory option in {@link OAuth2Options} is invalid
     */
    public OAuth2AuthenticationProvider(@Nullable final Stage stage, @NotNull final OAuth2API api) {
        this.stage = stage;
        this.api = Objects.requireNonNull(api, "OAuth2 api cannot be null");
        this.options = api.getOptions();
        this.options.validate();

        // Create a new HTTP server for handling local callbacks
        this.httpServer = HttpServer.create(stage);
    }

    /**
     * Creates an OAuth2 authentication provider using the specified {@link OAuth2Options}.
     *
     * @param stage   the JavaFX application stage (maybe {@code null})
     * @param options the OAuth2 options used to configure OAuth2 interaction
     * @throws NullPointerException     if {@code options} is {@code null}
     * @throws IllegalArgumentException if any mandatory option in {@link OAuth2Options} is invalid
     */
    public OAuth2AuthenticationProvider(@Nullable final Stage stage, @NotNull final OAuth2Options options) {
        this(stage, new OAuth2API(options));
    }

    /**
     * Returns the {@link OAuth2Options} used by this provider.
     *
     * @return the OAuth2 configuration options
     */
    @NotNull
    public final OAuth2Options getOptions() {
        return options;
    }

    /**
     * Generates an authorization URL based on the provided {@link OAuth2Credentials},
     * then prepares (and possibly starts) a local HTTP server to capture the OAuth2
     * authorization callback. The returned {@link CompletableFuture} completes with
     * the authorization URL once the server is ready.
     * <p>
     * You typically call this method when using {@link OAuth2Flow#AUTH_CODE} or other
     * flows requiring user interaction in a web browser. If not running in a browser
     * environment (i.e., {@link WebAPI#isBrowser()} is false), the method also ensures
     * a fresh local HTTP server is created to handle the callback.
     *
     * @param credentials the OAuth2 credentials containing the flow configuration; must not be {@code null}
     * @return a future that completes with the authorization URL once the local server is ready, or completes
     * exceptionally if server setup fails
     * @throws NullPointerException if {@code credentials} is {@code null}
     */
    public CompletableFuture<String> authorizeUrl(@NotNull final OAuth2Credentials credentials) {
        Objects.requireNonNull(credentials, "OAuth2Credentials cannot be null");

        final String authorizeUrl = authorizationUrl(credentials);

        // Ensure the local HTTP server is running in a non-browser environment
        if (!WebAPI.isBrowser()) {
            if (httpServer != null) {
                httpServer.stop();
            }
            httpServer = HttpServer.create(stage);
        }

        // Return a future that completes with the URL once the server is ready
        return httpServer.openURL(authorizeUrl);
    }

    /**
     * Builds the authorization URL. For the authorization code flow, a fresh {@code state}, PKCE code verifier and
     * (with the {@code openid} scope) {@code nonce} are generated for this login attempt, unless the credentials
     * set them, and remembered until the callback: in memory and, in the browser, in a short-lived cookie,
     * since the redirect back can start a new session.
     */
    String authorizationUrl(@NotNull final OAuth2Credentials credentials) {
        credentials.setNormalizedRedirectUri(normalizeUri(credentials.getRedirectUri()));
        final OAuth2Credentials request = new OAuth2Credentials(credentials);
        final OAuth2Flow flow = request.getFlow() != null ? request.getFlow() : options.getFlow();
        if (flow == OAuth2Flow.AUTH_CODE) {
            if (request.getState() == null) request.setState(randomToken());
            if (request.getCodeVerifier() == null) request.setCodeVerifier(randomToken());
            if (request.getNonce() == null && request.getScopes() != null && request.getScopes().contains("openid")) {
                request.setNonce(randomToken());
            }
            rememberLoginAttempt(request.getState(), new LoginAttempt(request.getCodeVerifier(), request.getNonce()));
        }
        final String authorizeUrl = api.authorizeURL(request);
        logger.debug("Authorize URL: {}", authorizeUrl);
        return authorizeUrl;
    }

    /**
     * Authenticates a user based on the given {@link Credentials}. Depending on the type of
     * credentials, different OAuth2 flows or validations may be triggered:
     * <ul>
     *   <li>{@link UsernamePasswordCredentials} triggers the Password flow.</li>
     *   <li>{@link TokenCredentials} accepts an existing access token only if it is a JWT with a valid signature,
     *   issuer and expiry issued to this client, or the introspection endpoint reports it as an active access
     *   token of this client.</li>
     *   <li>{@link OAuth2Credentials} triggers the configured OAuth2 flow (e.g., AUTH_CODE,
     *   PASSWORD, CLIENT, etc.).</li>
     * </ul>
     *
     * @param credentials the credentials to authenticate
     * @return a future that completes with the authenticated {@link User}, or completes exceptionally if
     * authentication fails
     * @throws ClassCastException            if {@code credentials} is not a recognized subtype
     * @throws CredentialValidationException if the credentials fail validation
     * @throws RuntimeException              if the flow is unsupported
     * @see OAuth2Credentials
     */
    @Override
    public CompletableFuture<User> authenticate(@NotNull final Credentials credentials) {
        try {
            // Handle UsernamePasswordCredentials as OAuth2 Password flow
            if (credentials instanceof UsernamePasswordCredentials usernamePasswordCredentials) {
                usernamePasswordCredentials.validate(null);
                final OAuth2Credentials oauth2Credentials = new OAuth2Credentials()
                        .setUsername(usernamePasswordCredentials.getUsername())
                        .setPassword(usernamePasswordCredentials.getPassword())
                        .setFlow(OAuth2Flow.PASSWORD);
                return authenticate(oauth2Credentials);
            }

            // Handle existing tokens (TokenCredentials): accepted only if verified by signature or introspection
            if (credentials instanceof TokenCredentials tokenCredentials) {
                tokenCredentials.validate(null);
                final String token = tokenCredentials.getToken();

                if (options.isVerifyToken() && token.split("\\.", -1).length == 3) {
                    try {
                        final User user = userFromVerifiedAccessToken(token);
                        if (user != null) {
                            return CompletableFuture.completedFuture(user);
                        }
                    } catch (AuthenticationException | IllegalStateException | JSONException ex) {
                        logger.debug("Token not verified as JWT, trying introspection: {}", ex.getMessage());
                    }
                }

                if (options.getIntrospectionPath() == null) {
                    return CompletableFuture.failedFuture(new AuthenticationException(
                            "Cannot verify the token: it is no verifiable JWT and there is no introspection endpoint"));
                }

                // RFC 7662: the token is only valid if the provider reports it as active
                return api.tokenIntrospection("access_token", token)
                        .thenCompose(json -> {
                            if (!json.optBoolean("active", false)) {
                                return CompletableFuture.failedFuture(
                                        new AuthenticationException("The token is not active"));
                            }
                            final String clientId = json.optString("client_id", null);
                            if (clientId != null && !clientId.equals(options.getClientId())) {
                                return CompletableFuture.failedFuture(
                                        new AuthenticationException("The token was issued to another client"));
                            }
                            final String typ = json.optString("typ", null);
                            if (typ != null && !typ.equalsIgnoreCase("Bearer")) {
                                return CompletableFuture.failedFuture(
                                        new AuthenticationException("The token is not an access token: " + typ));
                            }
                            final String name = Optional.ofNullable(json.optString("username", null))
                                    .or(() -> Optional.ofNullable(json.optString("email", null)))
                                    .orElse(json.optString("sub", null));
                            if (name == null) {
                                return CompletableFuture.failedFuture(new AuthenticationException(
                                        "The introspection response names no user (username, email or sub)"));
                            }
                            return CompletableFuture.completedFuture(new User(name, Map.of("auth",
                                    new JSONObject().put("access_token", token).put("introspection", json))));
                        });
            }

            // From this point, the only allowed credentials subtype is OAuth2Credentials
            OAuth2Credentials oauth2Credentials = (OAuth2Credentials) credentials;

            // Grab query parameters from the local HTTP server if present
            final Map<String, String> queryParams = httpServer.getQueryParams();
            logger.debug("URL query parameters: {}", queryParams);
            applyCallbackParams(oauth2Credentials, queryParams);

            return exchangeToken(withLoginAttempt(oauth2Credentials, queryParams));
        } catch (ClassCastException | CredentialValidationException | AuthenticationException ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    /**
     * Authenticates with the parameters of the OAuth2 redirect callback, e.g. the query parameters of the
     * routing request or of the {@link WebAPI.View} the identity provider redirected to. Unlike
     * {@link #authenticate(Credentials)}, the session URL is not consulted and the given credentials
     * are not modified.
     * <p>
     * The callback's {@code state} must belong to a login started with {@link #authorizeUrl(OAuth2Credentials)}
     * (or match the {@code state} of the credentials); its PKCE code verifier is sent with the token request
     * and its {@code nonce} must match the ID token. An {@code error} in the callback fails the authentication
     * with its description.
     *
     * @param credentials    the credentials used for the authorization request
     * @param callbackParams the query parameters of the redirect callback
     * @return a future that completes with the authenticated {@link User}, or completes exceptionally if
     * authentication fails
     */
    public CompletableFuture<User> authenticate(@NotNull final OAuth2Credentials credentials,
                                                @NotNull final Map<String, String> callbackParams) {
        Objects.requireNonNull(credentials, "OAuth2Credentials cannot be null");
        Objects.requireNonNull(callbackParams, "callbackParams cannot be null");
        try {
            final OAuth2Credentials callbackCredentials = applyCallbackParams(new OAuth2Credentials(credentials), callbackParams);
            return exchangeToken(withLoginAttempt(callbackCredentials, callbackParams));
        } catch (CredentialValidationException | AuthenticationException ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    static OAuth2Credentials applyCallbackParams(@NotNull final OAuth2Credentials credentials,
                                                 @NotNull final Map<String, String> callbackParams) {
        final String error = callbackParams.get("error");
        if (error != null) {
            final String description = callbackParams.get("error_description");
            throw new AuthenticationException(description == null ? error : error + ": " + description);
        }

        final String code = callbackParams.get("code");
        if (code == null) {
            return credentials;
        }
        if (code.isBlank()) {
            throw new AuthenticationException("Authorization code is missing");
        }
        if (credentials.getState() != null && !credentials.getState().equals(callbackParams.get("state"))) {
            throw new AuthenticationException("The state of the callback does not match the authorization request");
        }
        credentials.setCode(code);

        final String scope = callbackParams.get("scope");
        if (scope != null) {
            credentials.setScopes(List.of(scope.trim().split("[+ ]+")));
        }
        return credentials;
    }

    /**
     * Returns the credentials for exchanging the callback's code, with the code verifier and nonce of the login
     * attempt its {@code state} belongs to.
     */
    private OAuth2Credentials withLoginAttempt(@NotNull final OAuth2Credentials credentials,
                                               @NotNull final Map<String, String> callbackParams) {
        if (callbackParams.get("code") == null) {
            return credentials;
        }
        final String state = callbackParams.get("state");
        final LoginAttempt attempt = state == null ? null : takeLoginAttempt(state);
        if (attempt == null) {
            if (credentials.getState() != null) {
                return credentials; // the app manages the state itself; applyCallbackParams compared it
            }
            throw new AuthenticationException("The callback does not belong to a login started here: "
                    + "its state is missing, unknown or expired");
        }
        final OAuth2Credentials result = new OAuth2Credentials(credentials);
        if (result.getCodeVerifier() == null) result.setCodeVerifier(attempt.codeVerifier());
        if (result.getNonce() == null) result.setNonce(attempt.nonce());
        return result;
    }

    record LoginAttempt(String codeVerifier, String nonce, long createdMillis) {

        LoginAttempt(String codeVerifier, String nonce) {
            this(codeVerifier, nonce, System.currentTimeMillis());
        }

        String encode() {
            final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
            return encoder.encodeToString(codeVerifier.getBytes(StandardCharsets.UTF_8)) + "."
                    + (nonce == null ? "" : encoder.encodeToString(nonce.getBytes(StandardCharsets.UTF_8))) + "."
                    + createdMillis;
        }

        @Nullable
        static LoginAttempt decode(String value) {
            final String[] parts = value.split("\\.", -1);
            if (parts.length != 3) return null;
            final Base64.Decoder decoder = Base64.getUrlDecoder();
            try {
                return new LoginAttempt(new String(decoder.decode(parts[0]), StandardCharsets.UTF_8),
                        parts[1].isEmpty() ? null : new String(decoder.decode(parts[1]), StandardCharsets.UTF_8),
                        Long.parseLong(parts[2]));
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
    }

    private static final String LOGIN_ATTEMPT_COOKIE_PREFIX = "jpro_oauth_";
    private static final int LOGIN_ATTEMPT_MAX_AGE_SECONDS = 600;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();

    private void rememberLoginAttempt(@NotNull final String state, @NotNull final LoginAttempt attempt) {
        final long expired = System.currentTimeMillis() - LOGIN_ATTEMPT_MAX_AGE_SECONDS * 1000L;
        loginAttempts.values().removeIf(a -> a.createdMillis() < expired);
        loginAttempts.put(state, attempt);

        final WebAPI webAPI = browserWebAPI();
        if (webAPI != null) {
            webAPI.executeScript(loginAttemptCookieScript(state, attempt.encode(), LOGIN_ATTEMPT_MAX_AGE_SECONDS));
        }
    }

    // On https, Secure and the __Host- prefix keep the cookie on this host and off plain http.
    private static String loginAttemptCookieScript(String state, String value, int maxAgeSeconds) {
        return "(function() { var https = location.protocol === 'https:';"
                + " document.cookie = (https ? '__Host-' : '') + '" + loginAttemptCookie(state) + "=" + value
                + "; Max-Age=" + maxAgeSeconds + "; Path=/; SameSite=Lax' + (https ? '; Secure' : ''); })();";
    }

    private LoginAttempt takeLoginAttempt(@NotNull final String state) {
        LoginAttempt attempt = loginAttempts.remove(state);
        final WebAPI webAPI = browserWebAPI();
        if (webAPI != null) {
            final String name = loginAttemptCookie(state);
            final String cookie = Objects.requireNonNullElse(
                    webAPI.getCookies().get("__Host-" + name), webAPI.getCookies().get(name));
            webAPI.executeScript(loginAttemptCookieScript(state, "", 0));
            if (attempt == null && cookie != null) attempt = LoginAttempt.decode(cookie);
        }
        if (attempt != null && attempt.createdMillis() < System.currentTimeMillis() - LOGIN_ATTEMPT_MAX_AGE_SECONDS * 1000L) {
            return null;
        }
        return attempt;
    }

    @Nullable
    private WebAPI browserWebAPI() {
        return WebAPI.isBrowser() && stage != null ? WebAPI.getWebAPI(stage) : null;
    }

    // The state may come from the app, so the cookie name uses its hash.
    private static String loginAttemptCookie(@NotNull final String state) {
        return LOGIN_ATTEMPT_COOKIE_PREFIX + OAuth2API.codeChallenge(state);
    }

    private static String randomToken() {
        final byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Nullable
    private static String idTokenNonce(@NotNull final String idToken) {
        final String[] parts = idToken.split("\\.");
        if (parts.length < 2) return null;
        try {
            final JSONObject payload = new JSONObject(new String(Decoders.BASE64URL.decode(parts[1]), StandardCharsets.UTF_8));
            return payload.optString("nonce", null);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private CompletableFuture<User> exchangeToken(@NotNull final OAuth2Credentials oauth2Credentials) {
        try {
            // Validate flow and set appropriate parameters
            final JSONObject params = new JSONObject();
            final OAuth2Flow flow = (oauth2Credentials.getFlow() != null)
                    ? oauth2Credentials.getFlow()
                    : options.getFlow();

            // Validate credentials
            oauth2Credentials.validate(flow);

            if (options.getSupportedGrantTypes() != null
                    && !options.getSupportedGrantTypes().isEmpty()
                    && !options.getSupportedGrantTypes().contains(flow.getGrantType())) {
                return CompletableFuture.failedFuture(
                        new RuntimeException("Provided flow is not supported by provider"));
            }

            switch (flow) {
                case AUTH_CODE:
                    // code & redirect_uri & code_verifier
                    params.put("code", oauth2Credentials.getCode());
                    // must be identical to the redirect URI provided in the original link
                    if (oauth2Credentials.getRedirectUri() != null) {
                        params.put("redirect_uri", normalizeUri(oauth2Credentials.getRedirectUri()));
                    }
                    // the plaintext string that was previously hashed to create the code_challenge
                    if (oauth2Credentials.getCodeVerifier() != null) {
                        params.put("code_verifier", oauth2Credentials.getCodeVerifier());
                    }
                    break;

                case PASSWORD:
                    params.put("username", oauth2Credentials.getUsername())
                            .put("password", oauth2Credentials.getPassword());
                    if (oauth2Credentials.getScopes() != null) {
                        params.put("scope", String.join(options.getScopeSeparator(), oauth2Credentials.getScopes()));
                    }
                    break;

                case CLIENT:
                    // No specific params required for client_credentials, aside from optional scopes
                    if (oauth2Credentials.getScopes() != null) {
                        params.put("scope", String.join(options.getScopeSeparator(), oauth2Credentials.getScopes()));
                    }
                    break;

                case AUTH_JWT:
                    if (oauth2Credentials.getAssertion() != null) {
                        params.put("assertion", oauth2Credentials.getAssertion());
                    }
                    if (oauth2Credentials.getScopes() != null) {
                        params.put("scope", String.join(options.getScopeSeparator(), oauth2Credentials.getScopes()));
                    }
                    break;

                default:
                    return CompletableFuture.failedFuture(
                            new RuntimeException("Current flow does not allow acquiring a token by the replay party"));
            }

            // Exchange for an access token
            return api.token(flow.getGrantType(), params)
                    .thenCompose(json -> {
                        final String expectedNonce = oauth2Credentials.getNonce();
                        if (flow == OAuth2Flow.AUTH_CODE && expectedNonce != null && json.has("id_token")
                                && !expectedNonce.equals(idTokenNonce(json.getString("id_token")))) {
                            return CompletableFuture.failedFuture(new AuthenticationException(
                                    "The nonce of the ID token does not match the authorization request"));
                        }
                        // attempt to create a user from the json object
                        try {
                            final User newUser = createUser(json);
                            oauth2Credentials.setUsername(newUser.getName());
                            // basic validation passed
                            return CompletableFuture.completedFuture(newUser);
                        } catch (TokenExpiredException | IllegalStateException ex) {
                            return CompletableFuture.failedFuture(ex);
                        }
                    });

        } catch (CredentialValidationException ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    /**
     * Initiates OpenID Connect Discovery to dynamically obtain configuration data (e.g., token endpoint,
     * authorization endpoint, user info endpoint). The {@link CompletableFuture} completes with a new
     * {@link OpenIDAuthenticationProvider} upon success or completes exceptionally on failure.
     *
     * @return a future that completes with an {@link OpenIDAuthenticationProvider} once discovery succeeds
     */
    public CompletableFuture<OpenIDAuthenticationProvider> discover() {
        return api.discover(stage, options);
    }

    /**
     * Performs token introspection for the specified {@code tokenType} in the user's {@code auth} data.
     * <p>
     * The introspection response is returned as a {@link JSONObject}. For more details, see
     * <a href="https://tools.ietf.org/html/rfc7662">RFC 7662</a>.
     *
     * @param user      the user whose token is to be introspected
     * @param tokenType the token type to introspect (e.g., {@code "access_token"} or {@code "refresh_token"})
     * @return a future that completes with the introspection response as JSON
     * @throws NullPointerException if {@code user} is null
     */
    public CompletableFuture<JSONObject> introspect(User user, String tokenType) {
        return api.tokenIntrospection(tokenType, user.toJSON()
                .getJSONObject(User.KEY_ATTRIBUTES)
                .optJSONObject("auth")
                .get(tokenType)
                .toString());
    }

    /**
     * Refreshes an existing user session using its refresh token. Upon success, returns a new
     * {@link User} instance with updated tokens.
     *
     * @param user the user whose token will be refreshed
     * @return a future that completes with the new user instance containing updated tokens
     * @throws IllegalStateException if the user has no valid refresh token
     */
    public CompletableFuture<User> refresh(User user) throws IllegalStateException {
        final String refreshToken = user.toJSON()
                .getJSONObject(User.KEY_ATTRIBUTES)
                .optJSONObject("auth")
                .optString("refresh_token");

        if (refreshToken == null || refreshToken.isBlank()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("refresh_token is null or missing"));
        }

        return api.token("refresh_token", new JSONObject().put("refresh_token", refreshToken))
                .thenCompose(json -> {
                    try {
                        return CompletableFuture.completedFuture(createUser(json));
                    } catch (TokenExpiredException | IllegalStateException ex) {
                        return CompletableFuture.failedFuture(ex);
                    }
                });
    }

    /**
     * Revokes the given user's {@code access_token} or {@code refresh_token}.
     * <p>
     * See <a href="https://tools.ietf.org/html/rfc7009">RFC 7009</a> for details on token revocation.
     *
     * @param user      the user for whom to revoke the token
     * @param tokenType the token type to revoke (either {@code "access_token"} or {@code "refresh_token"})
     * @return a future that completes when the token is successfully revoked
     */
    public CompletableFuture<Void> revoke(User user, String tokenType) {
        return api.tokenRevocation(tokenType, user.toJSON()
                .getJSONObject(User.KEY_ATTRIBUTES)
                .optJSONObject("auth")
                .get(tokenType)
                .toString());
    }

    /**
     * Retrieves detailed user information (e.g., profile data) from an OpenID Connect or OAuth2
     * {@code userInfo} endpoint using the user's {@code access_token}.
     * <p>
     * This method also checks to ensure that the returned {@code sub} (subject) matches the one
     * in the token's claims if present. If the endpoint returns a {@code token} field, it will
     * attempt to verify that token as well.
     *
     * @param user the user whose information is to be fetched (must have an {@code access_token})
     * @return a future that completes with the user information in JSON format,
     * or completes exceptionally if validation fails
     * @throws NullPointerException    if {@code user} is {@code null}
     * @throws AuthenticationException if the subject in the UserInfo response does not match
     * @throws TokenExpiredException   if the {@code token} field has an expired token
     * @throws IllegalStateException   if the {@code token} field is invalid in any way
     */
    public CompletableFuture<JSONObject> userInfo(final @NotNull User user) {
        Objects.requireNonNull(user, "User must not be null");

        final JSONObject authJSON = user.toJSON()
                .getJSONObject(User.KEY_ATTRIBUTES)
                .getJSONObject("auth");

        return api.userInfo(authJSON.getString("access_token"))
                .thenCompose(json -> {
                    // Validate sub in access token
                    final JSONObject accessTokenJSON = authJSON.optJSONObject("accessToken");
                    if (accessTokenJSON != null && accessTokenJSON.has("sub")) {
                        final String userSub = accessTokenJSON.getString("sub");
                        if (!userSub.equals(json.getString("sub"))) {
                            return CompletableFuture.failedFuture(
                                    new AuthenticationException("User subject does not match UserInfo subject"));
                        }
                    }

                    // If the endpoint returned a 'token' field, attempt verification
                    if (json.has("token")) {
                        try {
                            verifyToken(json.getString("token"), false);
                        } catch (TokenExpiredException | IllegalStateException ex) {
                            return CompletableFuture.failedFuture(ex);
                        }
                    }

                    return CompletableFuture.completedFuture(json);
                });
    }

    /**
     * Logs out the specified user by invalidating (or otherwise marking as invalid) its access token
     * (and optionally refresh token) on the authorization server.
     *
     * @param user the user to logout
     * @return a future that completes when the user is logged out
     */
    public CompletableFuture<Void> logout(final @NotNull User user) {
        final JSONObject authJSON = user.toJSON().getJSONObject(User.KEY_ATTRIBUTES).getJSONObject("auth");
        final String accessToken = authJSON.getString("access_token");
        final String refreshToken = authJSON.optString("refresh_token");

        return api.logout(accessToken, refreshToken);
    }

    /**
     * Internal helper method that constructs a new {@link User} from token data in JSON format. The JSON
     * must contain at least {@code access_token}, and may optionally include {@code id_token}.
     * <p>
     * Both tokens are verified (signature and claims) if token verification is enabled. If a token
     * is expired or invalid, a corresponding exception is thrown.
     *
     * @param json the JSON object containing token data
     * @return a new {@link User} object representing the logged-in user
     * @throws TokenExpiredException if either the {@code access_token} or {@code id_token} is expired
     * @throws IllegalStateException if the token has invalid claims or signature
     */
    private User createUser(@NotNull final JSONObject json)
            throws TokenExpiredException, IllegalStateException {
        Objects.requireNonNull(json, "json can not be null");

        final JSONObject userJSON = new JSONObject();
        final JSONObject authJSON = new JSONObject(json.toString());

        // Attempt to verify or decode the access token
        // Many providers (e.g. Google) issue opaque access tokens; only a JWT can be verified
        if (json.has("access_token") && json.getString("access_token").split("\\.", -1).length == 3) {
            // attempt to verify the token
            final String token = json.getString("access_token");
            try {
                final JSONObject verifiedAccessToken = verifyToken(token, false);

                // Store JWT authorization
                authJSON.put("accessToken", verifiedAccessToken);

                // Derive a username from token claims (payload) if available
                final JSONObject payload = verifiedAccessToken.getJSONObject("payload");
                if (payload.has("name")) {
                    userJSON.put(Authentication.KEY_NAME, payload.getString("name"));
                } else if (payload.has("email")) {
                    userJSON.put(Authentication.KEY_NAME, payload.getString("email"));
                }

                authJSON.put("claimToken", "accessToken");
            } catch (JwtException | IllegalStateException ex) {
                logger.warn("Cannot verify the access token, the user is identified by the ID token: {}", ex.getMessage());
            }
        }

        // Attempt to verify or decode the ID token
        if (json.has("id_token")) {
            final String token = json.getString("id_token");
            try {
                final JSONObject verifiedIdToken = verifyToken(token, true);

                // Store JWT authorization
                authJSON.put("idToken", verifiedIdToken);

                // Derive a username from id token claims if available
                final JSONObject payload = verifiedIdToken.getJSONObject("payload");
                if (payload.has("name")) {
                    userJSON.put(Authentication.KEY_NAME, payload.getString("name"));
                } else if (payload.has("email")) {
                    userJSON.put(Authentication.KEY_NAME, payload.getString("email"));
                }
            } catch (JwtException | IllegalStateException | JSONException ex) {
                // the ID token is what identifies the user, so an invalid one must not log in
                throw new IllegalStateException("Invalid ID token: " + ex.getMessage(), ex);
            }
        }

        userJSON.put(Authentication.KEY_ATTRIBUTES, new JSONObject().put("auth", authJSON));

        // TODO: Additional user data, roles, or attributes could be populated here

        // Create authentication instance
        return new User(userJSON);
    }

    /**
     * Verifies and decodes a JWT token if token verification is enabled in the {@link OAuth2Options}.
     * Otherwise, decodes the token without checking the signature. Basic validation checks (e.g.,
     * issuer, audience) are performed based on {@link JWTOptions} and whether the token is an
     * {@code id_token}.
     *
     * @param token   the raw JWT token string
     * @param idToken {@code true} if the token is an ID token, which enables additional OIDC validations
     * @return a JSON object containing {@code header}, {@code payload}, and {@code signature}
     * @throws TokenExpiredException if the token has expired
     * @throws IllegalStateException if the token is invalid for any reason (bad signature, wrong issuer, etc.)
     */
    private JSONObject verifyToken(String token, boolean idToken)
            throws TokenExpiredException, IllegalStateException {
        final JSONObject json = new JSONObject();
        json.put("token", token);
        json.put("token_type", idToken ? "id_token" : "access_token");

        try {
            if (options.isVerifyToken()) {
                final JWTOptions parserOptions = options.getJWTOptions();
                final JwtParser jwtParser = Jwts.parser()
                        .keyLocator(header -> signingKey(header.getOrDefault("kid", "").toString()))
                        .clockSkewSeconds(parserOptions == null ? 0 : parserOptions.getLeeway())
                        .build();

                final Jws<Claims> jws = jwtParser.parseSignedClaims(token);
                Optional.ofNullable(jws.getHeader())
                        .ifPresent(header -> json.put("header", new JSONObject(header)));
                Optional.ofNullable(jws.getPayload())
                        .ifPresent(payload -> json.put("payload", new JSONObject(payload)));
                Optional.ofNullable(jws.getDigest())
                        .ifPresent(digest -> json.put("signature", Encoders.BASE64URL.encode(digest)));
            } else {
                // Decode without verifying signature
                final String[] parts = token.split("\\.");
                if (parts.length < 2) {
                    throw new IllegalStateException("Invalid JWT token");
                }
                final String headerString = new String(Decoders.BASE64URL.decode(parts[0]), StandardCharsets.UTF_8);
                json.put("header", new JSONObject(headerString));

                final String payloadString = new String(Decoders.BASE64URL.decode(parts[1]), StandardCharsets.UTF_8);
                json.put("payload", new JSONObject(payloadString));

                if (parts.length > 2) {
                    json.put("signature", parts[2]);
                }
            }
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException(e.getMessage(), e.getClaims() != null
                            ? e.getClaims().getExpiration().toInstant()
                            : Instant.now());
        } catch (JwtException e) {
            throw new IllegalStateException(e);
        }

        // Basic checks against JWTOptions
        final JWTOptions jwtOptions = options.getJWTOptions();
        final JSONObject payload = json.getJSONObject("payload");

        // Validate audience (a single string or an array)
        final List<String> audList = audience(payload);
        if (payload.has(Claims.AUDIENCE)) {
            if (audList.isEmpty()) {
                throw new IllegalStateException("User audience is null or empty");
            }
            // If we have known audiences in JWTOptions, ensure they're covered
            if (!idToken && jwtOptions.getAudience() != null) {
                for (String aud : jwtOptions.getAudience()) {
                    if (!audList.contains(aud)) {
                        throw new IllegalStateException("Invalid JWT audience, expected: " + aud +
                                ", actual: " + audList);
                    }
                }
            }
        }

        // Validate issuer
        if (jwtOptions.getIssuer() != null && !isExpectedIssuer(jwtOptions.getIssuer(), payload)) {
            throw new IllegalStateException("Invalid JWT issuer, expected: " + jwtOptions.getIssuer() +
                    ", actual: " + payload.optString(Claims.ISSUER, null));
        }

        // OIDC: an ID token must be issued to this client
        if (idToken && (options.getClientId() == null || !audList.contains(options.getClientId()))) {
            throw new IllegalStateException("Invalid ID token audience, expected: " + options.getClientId() +
                    ", actual: " + audList);
        }

        // Validate authorized party (azp) if this is an ID token
        if (idToken) {
            if (payload.has("azp")) {
                if (!options.getClientId().equals(payload.getString("azp"))) {
                    throw new IllegalStateException("Invalid authorized party, expected: " + options.getClientId() +
                            ", actual: " + payload.getString("azp"));
                }
                // If multiple audiences exist, ensure azp is present among them
                if (audList.size() > 1) {
                    if (!audList.contains(payload.getString("azp"))) {
                        throw new IllegalStateException("ID token with multiple audiences does not contain 'azp' claim value");
                    }
                }
            }
        }

        return json;
    }

    /**
     * Returns the signing key with the given ID from the provider's key set. The key set is cached; it is fetched
     * again when older than {@link OAuth2Options#getJwkMaxAge()} or, for an unknown key ID, after key rotation.
     */
    private synchronized Key signingKey(String kid) {
        final Instant now = Instant.now();
        final boolean expired = jwks != null && options.getJwkMaxAge() >= 0
                && !jwksFetchedAt.plusSeconds(options.getJwkMaxAge()).isAfter(now);
        final boolean unknownKid = jwks != null && !jwks.containsKey(kid)
                && !now.isBefore(jwksFetchedAt.plus(jwksRefetchDelay));
        if (jwks == null || expired || unknownKid) {
            try {
                final String webKeys = api.getHttpClient().send(
                        HttpRequest.newBuilder(URI.create(options.getJwkPath())).timeout(Duration.ofSeconds(10)).build(),
                        HttpResponse.BodyHandlers.ofString()).body();
                jwks = Jwks.setParser().build().parse(webKeys).getKeys().stream()
                        .collect(toMap(Identifiable::getId, Jwk::toKey));
                jwksFetchedAt = now;
            } catch (IOException e) {
                throw new IllegalStateException("Cannot load the signing keys from " + options.getJwkPath(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while loading the signing keys", e);
            }
        }
        return jwks.get(kid);
    }

    /**
     * Returns the user of a JWT access token whose signature, issuer and expiry are valid and which was issued to
     * this client ({@code aud}, {@code azp} or {@code client_id}), or {@code null} if it can't be used.
     */
    @Nullable
    private User userFromVerifiedAccessToken(@NotNull final String token) {
        final JSONObject verified = verifyToken(token, false);
        final JSONObject payload = verified.getJSONObject("payload");
        if ("ID".equalsIgnoreCase(payload.optString("typ", ""))) {
            return null; // Keycloak marks its ID tokens; they are no bearer tokens
        }
        if (!isIssuedToThisClient(payload)) {
            return null;
        }
        final String name = Stream.of("name", "email", "preferred_username", "sub")
                .map(claim -> payload.optString(claim, null))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
        if (name == null) {
            return null;
        }
        return new User(name, Map.of("auth", new JSONObject()
                .put("access_token", token)
                .put("accessToken", verified)
                .put("claimToken", "accessToken")));
    }

    private boolean isIssuedToThisClient(@NotNull final JSONObject payload) {
        final List<String> audience = audience(payload);
        final JWTOptions jwtOptions = options.getJWTOptions();
        if (jwtOptions != null && jwtOptions.getAudience() != null
                && audience.stream().anyMatch(jwtOptions.getAudience()::contains)) {
            return true;
        }
        final String clientId = options.getClientId();
        return clientId != null && (audience.contains(clientId)
                || clientId.equals(payload.optString("azp", null))
                || clientId.equals(payload.optString("client_id", null)));
    }

    private static final Pattern MICROSOFT_V2_ISSUER =
            Pattern.compile("https://login\\.microsoftonline\\.com/([^/{}]+)/v2\\.0");

    private static boolean isExpectedIssuer(@NotNull final String expected, @NotNull final JSONObject payload) {
        final String actual = payload.optString(Claims.ISSUER, null);
        if (actual == null) return false;
        // Microsoft's multi-tenant discovery documents the issuer with a placeholder for the user's tenant
        final String resolved = expected.contains("{tenantid}") && payload.has("tid")
                ? expected.replace("{tenantid}", payload.getString("tid")) : expected;
        if (resolved.equals(actual)) return true;
        // Microsoft v1 tokens (e.g. for the app's own API) name the same tenant as https://sts.windows.net/<tenant>/
        final Matcher microsoft = MICROSOFT_V2_ISSUER.matcher(resolved);
        if (microsoft.matches() && actual.equals("https://sts.windows.net/" + microsoft.group(1) + "/")) return true;
        // Google documents both forms of its issuer
        return resolved.equals("https://accounts.google.com") && actual.equals("accounts.google.com");
    }

    private static List<String> audience(@NotNull final JSONObject payload) {
        final Object audience = payload.opt(Claims.AUDIENCE);
        if (audience instanceof JSONArray array) {
            return array.toList().stream().map(Object::toString).toList();
        }
        return audience == null ? List.of() : List.of(audience.toString());
    }

    /**
     * Checks if the given user has expired based on the "exp" field inside the user's "auth" JSON data.
     *
     * @param user the user to check
     * @return {@code true} if the user has an expiration time in the past; {@code false} otherwise
     */
    @SuppressWarnings("unused")
    private boolean hasExpired(User user) {
        if (user.getAttributes().containsKey("auth")) {
            JSONObject jwtInfo = (JSONObject) user.getAttributes().get("auth");
            if (jwtInfo.has(Claims.EXPIRATION)) {
                final Instant expiredAt = Instant.ofEpochMilli(jwtInfo.getLong(Claims.EXPIRATION));
                return expiredAt.isBefore(Instant.now());
            }
        }
        return false;
    }

    /**
     * Normalizes the given URI string to ensure it is a complete URI. If the URI starts with '/',
     * the server's host and port from the internal {@link HttpServer} are prepended. When running
     * locally with {@code localhost} and {@link OAuth2Options#isUseLoopbackIpAddress()} is true,
     * the loopback IP address is used instead. In most cases, {@code http} is used for local
     * addresses and {@code https} otherwise.
     *
     * @param uri the URI string (may be a partial URI, e.g., "/callback")
     * @return the normalized absolute URI
     */
    private String normalizeUri(String uri) {
        if (httpServer != null && uri != null && uri.charAt(0) == '/') {
            final int port = httpServer.getServerPort();
            String server = httpServer.getServerHost();
            boolean isLocalAddress = server.equals("localhost");

            if (options.isUseLoopbackIpAddress() && isLocalAddress) {
                server = InetAddress.getLoopbackAddress().getHostAddress();
            }
            if (port > 0) {
                server += ":" + port;
            }
            // If truly local, we can use http; otherwise https might be necessary
            final String serverUrl = isLocalAddress ? "http://" + server : "https://" + server;
            return serverUrl + uri;
        }
        return uri;
    }
}
