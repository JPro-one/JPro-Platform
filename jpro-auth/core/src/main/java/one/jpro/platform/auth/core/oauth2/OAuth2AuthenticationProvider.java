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
 * Base class for creating an OAuth2 authentication provider.
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
     *
     * @param stage the JavaFX application stage
     * @param api   the OAuth2 api
     */
    public OAuth2AuthenticationProvider(@Nullable final Stage stage, @NotNull final OAuth2API api) {
        this.stage = stage;
        this.api = Objects.requireNonNull(api, "OAuth2 api cannot be null");
        this.options = api.getOptions();
        this.options.validate();

        // Create a new http server
        this.httpServer = HttpServer.create(stage);
    }

    /**
     * Creates an OAuth2 authentication provider.
     *
     * @param stage   the JavaFX application stage
     * @param options the OAuth2 options
     */
    public OAuth2AuthenticationProvider(@Nullable final Stage stage, @NotNull final OAuth2Options options) {
        this(stage, new OAuth2API(options));
    }

    /**
     * Returns the options used to configure this provider.
     *
     * @return an OAuth2 options object
     */
    @NotNull
    public final OAuth2Options getOptions() {
        return options;
    }

    /**
     * The client sends the end-user's browser to the authorization endpoint.
     * This endpoint is where the user signs in and grants access.
     * End-user interaction is required.
     *
     * @param credentials the credentials to authenticate
     * @return a {@link CompletableFuture} that will complete with the authorization URL
     * once the HTTP server is ready to handle the callback, or with an exception
     * if an error occurs during the process.
     */
    public CompletableFuture<String> authorizeUrl(@NotNull final OAuth2Credentials credentials) {
        Objects.requireNonNull(credentials, "OAuth2Credentials cannot be null");

        // Generate the authorization URL and open it in the default browser
        final String authorizeUrl = api.authorizeURL(credentials
                .setNormalizedRedirectUri(normalizeUri(credentials.getRedirectUri())));
        logger.debug("Authorize URL: {}", authorizeUrl);

        if (!WebAPI.isBrowser()) {
            if (httpServer != null) {
                // Stop any previous running local http server
                httpServer.stop();
            }

            // Create a new http server
            httpServer = HttpServer.create(stage);
        }
        return httpServer.openURL(authorizeUrl);
    }

    /**
     * Authenticate a user with the given credentials.
     *
     * @param credentials the credentials to authenticate
     * @return a future that will complete with the authenticated user
     */
    @Override
    public CompletableFuture<User> authenticate(@NotNull final Credentials credentials) {
        try {
            if (credentials instanceof UsernamePasswordCredentials usernamePasswordCredentials) {
                // validate
                usernamePasswordCredentials.validate(null);

                OAuth2Credentials oauth2Credentials = new OAuth2Credentials()
                        .setUsername(usernamePasswordCredentials.getUsername())
                        .setPassword(usernamePasswordCredentials.getPassword())
                        .setFlow(OAuth2Flow.PASSWORD);

                return authenticate(oauth2Credentials);
            }

            // if the credentials already contain a token, then validate it to confirm
            // that it can be reused, otherwise, based on the configured flow, request
            // a new token from the authority provider
            if (credentials instanceof TokenCredentials tokenCredentials) {
                tokenCredentials.validate(null);

                // credentials already contain a token, validate it
                // attempt to create a user from the credentials
                try {
                    final User newUser = createUser(new JSONObject().put("access_token", tokenCredentials.getToken()));
                    // basic validation passed
                    return CompletableFuture.completedFuture(newUser);
                } catch (TokenExpiredException | IllegalStateException ex) {
                    logger.error(ex.getMessage(), ex);
                    // Fall through to introspection if supported
                }

                // the token is not JWT format or this authentication provider is not configured to use JWTs
                // in this case we must rely on token introspection in order to know more about its state
                // attempt to create a token object from the given string representation

                // not all providers support this, so we need to check if the call is possible
                if (options.getIntrospectionPath() == null) {
                    // this provider doesn't allow introspection, this means we are not able
                    // to perform any authentication
                    return CompletableFuture.failedFuture(
                            new RuntimeException("Can't authenticate `access_token`: "
                                    + "Provider doesn't support token introspection"));
                }

                // perform the introspection in accordance to RFC7662
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

                            // attempt to create a user from the json object
                            try {
                                final User newUser = createUser(json);
                                // basic validation passed
                                return CompletableFuture.completedFuture(newUser);
                            } catch (TokenExpiredException | IllegalStateException ex) {
                                return CompletableFuture.failedFuture(ex);
                            }
                        });
            }

            // from this point, the only allowed credentials subtype is OAuth2Credentials
            OAuth2Credentials oauth2Credentials = (OAuth2Credentials) credentials;

            // Wrap the Query Parameters in a JSONObject for easy access
            final JSONObject queryParams = new JSONObject(httpServer.getQueryParams());
            logger.debug("URL query parameters: {}", queryParams);

            // Retrieve the authorization code
            if (queryParams.has("code")) {
                oauth2Credentials.setCode(queryParams.getString("code"));
                if (oauth2Credentials.getCode() == null || oauth2Credentials.getCode().isBlank()) {
                    return CompletableFuture.failedFuture(
                            new RuntimeException("Authorization code is missing"));
                }
            }

            // Retrieve scopes
            if (queryParams.has("scope")) {
                final String[] scopes = queryParams.getString("scope").split("\\+");
                oauth2Credentials.setScopes(List.of(scopes));
            }

            // Create a new JSONObject to hold the parameters
            final JSONObject params = new JSONObject();
            final OAuth2Flow flow = oauth2Credentials.getFlow() != null
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
                    // code is always required. It's the code received on the web side
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
                    // applications may need an access token to act on behalf of themselves rather than a user.
                    // in this case there are no parameters
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
     * Creates a OAuth2 authentication provider for OpenID Connect Discovery. The discovery will use the given
     * site in the configuration options and attempt to load the well-known descriptor.
     *
     * @return an {@link OpenIDAuthenticationProvider} instance.
     */
    public CompletableFuture<OpenIDAuthenticationProvider> discover() {
        return api.discover(stage, options);
    }

    /**
     * Determine the active state of an OAuth 2.0 token.
     *
     * @param user      the user
     * @param tokenType the token type to introspect
     * @return a {@link CompletableFuture} with the introspection response information in JSON format.
     */
    public CompletableFuture<JSONObject> introspect(User user, String tokenType) {
        return api.tokenIntrospection(tokenType,
                user.toJSON()
                        .getJSONObject(User.KEY_ATTRIBUTES)
                        .optJSONObject("auth")
                        .get(tokenType)
                        .toString());
    }

    /**
     * Refreshes the user's access token.
     *
     * @param user the user
     * @return a new user instance with the refreshed access token
     * @throws IllegalStateException if the user does not have a refresh token
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
                    // attempt to create a user from the json object
                    try {
                        final User newUser = createUser(json);
                        // basic validation passed
                        return CompletableFuture.completedFuture(newUser);
                    } catch (TokenExpiredException | IllegalStateException ex) {
                        return CompletableFuture.failedFuture(ex);
                    }
                });
    }

    /**
     * Revokes an obtained access or refresh token.
     * More info at <a href="https://tools.ietf.org/html/rfc7009">RFC 7009</a>.
     *
     * @param user      the user to revoke
     * @param tokenType the token type (either <code>access_token</code> or <code>refresh_token</code>)
     * @return a {@link CompletableFuture} that completes when the token is revoked.
     */
    public CompletableFuture<Void> revoke(User user, String tokenType) {
        return api.tokenRevocation(tokenType,
                user.toJSON()
                        .getJSONObject(User.KEY_ATTRIBUTES)
                        .optJSONObject("auth")
                        .get(tokenType)
                        .toString());
    }

    /**
     * Retrieve user information and other attributes for a logged-in end-user.
     *
     * @param user the user (access token) to fetch the user information.
     * @return a {@link CompletableFuture} with the user information in JSON format.
     * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html#UserInfo">OpenID Connect Core 1.0</a>
     */
    public CompletableFuture<JSONObject> userInfo(final @NotNull User user) {
        Objects.requireNonNull(user, "User must not be null");
        final JSONObject authJSON = user.toJSON()
                .getJSONObject(User.KEY_ATTRIBUTES)
                .getJSONObject("auth");

        return api.userInfo(authJSON.getString("access_token"))
                .thenCompose(json -> {
                    // validate if the subject of this token match the user subject
                    final JSONObject accessTokenJSON = authJSON.optJSONObject("accessToken");
                    if (accessTokenJSON != null && accessTokenJSON.has("sub")) {
                        final String userSub = accessTokenJSON.getString("sub");
                        if (!userSub.equals(json.getString("sub"))) {
                            return CompletableFuture.failedFuture(
                                    new AuthenticationException("User subject does not match UserInfo subject"));
                        }
                    }

                    // verify if expired
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
     * Logout the user from this OAuth2 provider.
     *
     * @param user the user to logout
     * @return a {@link CompletableFuture} that completes when the user is logged out.
     */
    public CompletableFuture<Void> logout(final @NotNull User user) {
        final JSONObject authJSON = user.toJSON().getJSONObject(User.KEY_ATTRIBUTES).getJSONObject("auth");
        final String accessToken = authJSON.getString("access_token");
        final String refreshToken = authJSON.optString("refresh_token");

        return api.logout(accessToken, refreshToken);
    }

    /**
     * Creates a {@link User} from the given JSON object containing at least the "access_token" (and optionally "id_token").
     *
     * @param json the token data JSON
     * @return a User object
     * @throws TokenExpiredException if the token is expired
     * @throws IllegalStateException if the token has invalid claims
     */
    private User createUser(@NotNull final JSONObject json)
            throws TokenExpiredException, IllegalStateException {
        Objects.requireNonNull(json, "json can not be null");

        final JSONObject userJSON = new JSONObject();
        final JSONObject authJSON = new JSONObject(json.toString());

        if (json.has("access_token")) {
            // attempt to verify the token
            final String token = json.getString("access_token");
            try {
                final JSONObject verifiedAccessToken = verifyToken(token, false);
                // Store JWT authorization
                authJSON.put("accessToken", verifiedAccessToken);

                // Set principal name
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

        if (json.has("id_token")) {
            // attempt to create a user from the json object
            final String token = json.getString("id_token");

            try {
                // verify if the user is not expired
                // this may happen if the user tokens have been issued for future use for example
                final JSONObject verifiedIdToken = verifyToken(token, true);

                // Store JWT authorization
                authJSON.put("idToken", verifiedIdToken);

                // Set principal name
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

        // TODO: Configure roles

        // Create authentication instance
        return new User(userJSON);
    }

    /**
     * Performs a token verification and basic validation.
     *
     * @param token   the token string
     * @param idToken set to <code>true</code> if this token is an id_token, otherwise <code>false</code>
     * @return a {@link JSONObject} holding the Json Web Token information related to this token.
     * @throws TokenExpiredException if the token has expired
     * @throws IllegalStateException if the basic validation fails
     */
    private JSONObject verifyToken(String token, boolean idToken)
            throws TokenExpiredException, IllegalStateException {

        final JSONObject json = new JSONObject();
        json.put("token", token);
        json.put("token_type", idToken ? "id_token" : "access_token");

        try {
            if (options.isVerifyToken()) {
                // Build a parser
                try (HttpClient httpClient = HttpClient.newHttpClient()) {
                    // Parse and verify signature/claims
                    final String webKeys = httpClient.send(
                            HttpRequest.newBuilder(URI.create(options.getJwkPath())).build(),
                            HttpResponse.BodyHandlers.ofString()).body();
                    final Map<String, ? extends Key> keyMap = Jwks.setParser().build()
                            .parse(webKeys).getKeys().stream()
                            .collect(toMap(Identifiable::getId, Jwk::toKey));
                    final JwtParser jwtParser = Jwts.parser()
                            .keyLocator(header ->
                                    keyMap.get(header.getOrDefault("kid", "").toString()))
                            .build();
                    final Jws<Claims> jws = jwtParser.parseSignedClaims(token);

                    // Header info
                    Optional.ofNullable(jws.getHeader())
                            .ifPresent(header -> json.put("header", new JSONObject(jws.getHeader())));

                    // Payload info
                    Optional.ofNullable(jws.getPayload())
                            .ifPresent(payload -> json.put("payload", new JSONObject(jws.getPayload())));

                    // Signature info
                    Optional.ofNullable(jws.getDigest())
                            .ifPresent(digest -> json.put("signature", Encoders.BASE64URL.encode(digest)));
                } catch (ExpiredJwtException e) {
                    throw new TokenExpiredException(e.getMessage(),
                            e.getClaims() != null
                                    ? e.getClaims().getExpiration().toInstant() : Instant.now());
                } catch (JwtException e) {
                    throw new IllegalStateException(e.getMessage());
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e.getMessage());
                }
            } else {
                // Decode header and claims without verifying the signature
                final String[] parts = token.split("\\.");
                if (parts.length < 2) {
                    throw new IllegalStateException("Invalid JWT token");
                }

                // Decode the JWT token header and payload
                final String headerString = new String(Decoders.BASE64URL.decode(parts[0]), StandardCharsets.UTF_8);
                json.put("header", new JSONObject(headerString));
                final String payloadString = new String(Decoders.BASE64URL.decode(parts[1]), StandardCharsets.UTF_8);
                json.put("payload", new JSONObject(payloadString));

                // Retrieve signature if available
                if (parts.length > 2) {
                    final String signature = parts[2];
                    json.put("signature", signature);
                }
            }
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException(e.getMessage(),
                    e.getClaims() != null
                            ? e.getClaims().getExpiration().toInstant() : Instant.now());
        } catch (JwtException e) {
            throw new IllegalStateException(e.getMessage());
        }

        final JWTOptions jwtOptions = options.getJWTOptions();
        final JSONObject payload = json.getJSONObject("payload");

        // validate the audience
        if (payload.has(Claims.AUDIENCE)) {
            final JSONArray audience = payload.getJSONArray(Claims.AUDIENCE);
            if (audience == null || audience.isEmpty()) {
                throw new IllegalStateException("User audience is null or empty");
            }

            if (!audience.isEmpty()) {
                if (idToken || jwtOptions.getAudience() == null) {
                    // In reference to: https://openid.net/specs/openid-connect-core-1_0.html#IDTokenValidation
                    // The Client MUST validate that the aud (audience) Claim contains its client_id value registered at
                    // the Issuer identified by the iss (issuer) Claim as an audience. The aud (audience) Claim MAY contain
                    // an array with more than one element. The ID Token MUST be rejected if the ID Token does not list the
                    // Client as a valid audience, or if it contains additional audiences not trusted by the Client.
//                    if (!audience.toString().contains(options.getClientId())) {
//                        throw new IllegalStateException("Invalid JWT audience, expected: " + options.getClientId() +
//                                ", actual: " + audience);
//                    }
                } else {
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
        }

        // validate the issuer
        if (jwtOptions.getIssuer() != null) {
            if (!jwtOptions.getIssuer().equals(payload.getString(Claims.ISSUER))) {
                throw new IllegalStateException("Invalid JWT issuer, expected: " + jwtOptions.getIssuer() +
                        ", actual: " + payload.getString(Claims.ISSUER));
            }
        }

        // validate authorised party
        if (idToken) {
            if (payload.has("azp")) {
                if (!options.getClientId().equals(payload.getString("azp"))) {
                    throw new IllegalStateException("Invalid authorised party, expected: " + options.getClientId() +
                            ", actual: " + payload.getString("azp"));
                }

                final JSONArray audience = payload.getJSONArray(Claims.AUDIENCE);
                if (audience != null && audience.length() > 1) {
                    // In reference to: https://openid.net/specs/openid-connect-core-1_0.html#IDTokenValidation
                    // If the ID Token contains multiple audiences, the Client SHOULD verify that an azp Claim is present.
                    final List<String> audList = audience.toList().stream()
                            .map(Object::toString)
                            .toList();
                    if (audList.contains(payload.getString("azp"))) {
                        throw new IllegalStateException("ID token with multiple audiences, " +
                                "doesn't contain the azp Claim value");
                    }
                }
            }
        }

        return json;
    }

    /**
     * Returns {@code true} if the given user has expired based on the "exp" property in the "auth" object.
     *
     * @param user the user
     * @return {@code true} if the user is expired; {@code false} otherwise
     */
    private boolean hasExpired(User user) {
        if (user.getAttributes().containsKey("auth")) {
            JSONObject jwtInfo = (JSONObject) user.getAttributes().get("auth");
            if (jwtInfo.has("exp")) {
                final Instant expiredAt = Instant.ofEpochMilli(jwtInfo.getLong("exp"));
                return expiredAt.isBefore(Instant.now());
            }
        }
        return false;
    }

    /**
     * Normalizes the given URI by converting a partial URI to a complete URI using the server's host and port
     * information.
     * <p>
     * This method checks if the URI starts with a '/' character and, if so, appends the server's host and port
     * information to create a complete URI. If the server's host is "localhost" and the options specify using
     * the loopback IP address, the loopback address is used instead. Depending on whether the address is local,
     * "http" or "https" is used for the scheme.</p>
     *
     * @param uri the URI string to be normalized, which may be a partial URI starting with '/'
     * @return the normalized URI, including the server's host and port if applicable
     */
    private String normalizeUri(String uri) {
        // Complete uri if is partial
        String redirectUri = uri;
        if (httpServer != null && redirectUri != null && redirectUri.charAt(0) == '/') {
            final int port = httpServer.getServerPort();
            String server = httpServer.getServerHost();
            boolean isLocalAddress = server.equals("localhost");
            if (options.isUseLoopbackIpAddress() && isLocalAddress) {
                server = InetAddress.getLoopbackAddress().getHostAddress();
            }
            if (port > 0) {
                server += ":" + port;
            }
            final String serverUrl = isLocalAddress ? "http://" + server : "https://" + server;
            redirectUri = serverUrl + redirectUri;
        }
        return redirectUri;
    }
}
