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
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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

        // Generate the authorization URL
        final String authorizeUrl = api.authorizeURL(
                credentials.setNormalizedRedirectUri(normalizeUri(credentials.getRedirectUri()))
        );
        logger.debug("Authorize URL: {}", authorizeUrl);

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
     * Authenticates a user based on the given {@link Credentials}. Depending on the type of
     * credentials, different OAuth2 flows or validations may be triggered:
     * <ul>
     *   <li>{@link UsernamePasswordCredentials} triggers the Password flow.</li>
     *   <li>{@link TokenCredentials} attempts to validate an existing token, optionally
     *   performing token introspection if the token is not a valid JWT or is expired.</li>
     *   <li>{@link OAuth2Credentials} triggers the configured OAuth2 flow (e.g., AUTH_CODE,
     *   PASSWORD, CLIENT, etc.).</li>
     * </ul>
     *
     * @param credentials the credentials to authenticate
     * @return a future that completes with the authenticated {@link User}, or completes exceptionally if
     * authentication fails
     * @throws ClassCastException            if {@code credentials} is not a recognized subtype
     * @throws CredentialValidationException if the credentials fail validation
     * @throws RuntimeException              if the flow is unsupported, or if token introspection is unavailable
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

            // Handle existing tokens (TokenCredentials)
            if (credentials instanceof TokenCredentials tokenCredentials) {
                tokenCredentials.validate(null);

                // Attempt to create a user from the token if valid
                try {
                    final User newUser = createUser(new JSONObject().put("access_token", tokenCredentials.getToken()));
                    // Basic validation passed
                    return CompletableFuture.completedFuture(newUser);
                } catch (TokenExpiredException | IllegalStateException ex) {
                    logger.error(ex.getMessage(), ex);
                    // Fall through to introspection if supported
                }

                // If token is not a valid JWT or is expired, try introspection
                if (options.getIntrospectionPath() == null) {
                    return CompletableFuture.failedFuture(
                            new RuntimeException("Can't authenticate `access_token`: no introspection support"));
                }

                // Perform the introspection in accordance to RFC7662
                return api.tokenIntrospection("access_token", tokenCredentials.getToken())
                        .thenCompose(json -> {
                            // RFC7662 dictates that there is a boolean active field,
                            // however token info implementation may not return this
                            if (json.has("active") && json.getBoolean("active")) {
                                return CompletableFuture.failedFuture(new RuntimeException("Inactive Token"));
                            }

                            // validate client_id
                            if (json.has("client_id")) {
                                final String clientId = options.getClientId();
                                if (clientId != null && !clientId.equals(json.getString("client_id"))) {
                                    // client identifier for the OAuth2 client that requested this token
                                    logger.info("Introspect `client_id` doesn't match configured `client_id`");
                                }
                            }
                            try {
                                return CompletableFuture.completedFuture(createUser(json));
                            } catch (TokenExpiredException | IllegalStateException ex) {
                                return CompletableFuture.failedFuture(ex);
                            }
                        });
            }

            // From this point, the only allowed credentials subtype is OAuth2Credentials
            OAuth2Credentials oauth2Credentials = (OAuth2Credentials) credentials;

            // Grab query parameters from the local HTTP server if present
            final JSONObject queryParams = new JSONObject(httpServer.getQueryParams());
            logger.debug("URL query parameters: {}", queryParams);

            // Retrieve the authorization code
            if (queryParams.has("code")) {
                oauth2Credentials.setCode(queryParams.getString("code"));
                if (oauth2Credentials.getCode().isBlank()) {
                    return CompletableFuture.failedFuture(new RuntimeException("Authorization code is missing"));
                }
            }

            // Retrieve scopes
            if (queryParams.has("scope")) {
                final String[] scopes = queryParams.getString("scope").split("\\+");
                oauth2Credentials.setScopes(List.of(scopes));
            }

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

        } catch (ClassCastException | CredentialValidationException ex) {
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
        if (json.has("access_token")) {
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
                logger.error("Cannot decode/verify access token:", ex);
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
            } catch (JwtException | IllegalStateException ex) {
                logger.error("Cannot decode/verify id token:", ex);
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
                // Fetch JWKS and verify signature
                try (HttpClient httpClient = HttpClient.newHttpClient()) {
                    final String webKeys = httpClient.send(
                            HttpRequest.newBuilder(URI.create(options.getJwkPath())).build(),
                            HttpResponse.BodyHandlers.ofString()).body();

                    final Map<String, ? extends Key> keyMap = Jwks.setParser().build()
                            .parse(webKeys).getKeys().stream()
                            .collect(toMap(Identifiable::getId, Jwk::toKey));

                    final JwtParser jwtParser = Jwts.parser()
                            .keyLocator(header -> keyMap.get(header.getOrDefault("kid", "").toString()))
                            .build();

                    final Jws<Claims> jws = jwtParser.parseSignedClaims(token);
                    Optional.ofNullable(jws.getHeader())
                            .ifPresent(header -> json.put("header", new JSONObject(header)));
                    Optional.ofNullable(jws.getPayload())
                            .ifPresent(payload -> json.put("payload", new JSONObject(payload)));
                    Optional.ofNullable(jws.getDigest())
                            .ifPresent(digest -> json.put("signature", Encoders.BASE64URL.encode(digest)));
                }
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
        } catch (JwtException | IOException | InterruptedException e) {
            throw new IllegalStateException(e.getMessage());
        }

        // Basic checks against JWTOptions
        final JWTOptions jwtOptions = options.getJWTOptions();
        final JSONObject payload = json.getJSONObject("payload");

        // Validate audience
        if (payload.has(Claims.AUDIENCE)) {
            final JSONArray audience = payload.getJSONArray(Claims.AUDIENCE);
            if (audience.isEmpty()) {
                throw new IllegalStateException("User audience is null or empty");
            }
            // If we have known audiences in JWTOptions, ensure they're covered
            if (!idToken && jwtOptions.getAudience() != null) {
                final List<String> audList = audience.toList().stream()
                        .map(Object::toString)
                        .toList();
                for (String aud : jwtOptions.getAudience()) {
                    if (!audList.contains(aud)) {
                        throw new IllegalStateException("Invalid JWT audience, expected: " + aud +
                                ", actual: " + audience);
                    }
                }
            }
        }

        // Validate issuer
        if (jwtOptions.getIssuer() != null) {
            if (!jwtOptions.getIssuer().equals(payload.getString(Claims.ISSUER))) {
                throw new IllegalStateException("Invalid JWT issuer, expected: " + jwtOptions.getIssuer() +
                        ", actual: " + payload.getString(Claims.ISSUER));
            }
        }

        // Validate authorized party (azp) if this is an ID token
        if (idToken) {
            if (payload.has("azp")) {
                if (!options.getClientId().equals(payload.getString("azp"))) {
                    throw new IllegalStateException("Invalid authorized party, expected: " + options.getClientId() +
                            ", actual: " + payload.getString("azp"));
                }
                // If multiple audiences exist, ensure azp is present among them
                final JSONArray audience = payload.optJSONArray(Claims.AUDIENCE);
                if (audience != null && audience.length() > 1) {
                    final List<String> audList = audience.toList().stream()
                            .map(Object::toString)
                            .toList();
                    if (!audList.contains(payload.getString("azp"))) {
                        throw new IllegalStateException("ID token with multiple audiences does not contain 'azp' claim value");
                    }
                }
            }
        }

        return json;
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
