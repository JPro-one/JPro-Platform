package one.jpro.auth.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import one.jpro.auth.authentication.*;
import one.jpro.auth.utils.AuthUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * JSON Web Token authentication provider.
 *
 * @author Besmir Beqiri
 */
public class JWTAuthenticationProvider implements AuthenticationProvider<TokenCredentials> {

    private static final Logger log = LoggerFactory.getLogger(JWTAuthenticationProvider.class);

    private static final Base64.Decoder BASE64_DECODER = AuthUtils.BASE64_DECODER;

    @NotNull
    private final JWTAuthOptions authOptions;
    @NotNull
    private final JWTOptions options;
    @NotNull
    private final JWTAuthAPI api;

    /**
     * Default constructor.
     *
     * @param authOptions the authentication options
     */
    public JWTAuthenticationProvider(@NotNull final JWTAuthOptions authOptions) {
        this.authOptions = Objects.requireNonNull(authOptions, "JWT authentication options cannot be null");
        this.options = Objects.requireNonNull(authOptions.getJWTOptions(), "JWT options cannot be null");
        this.api = new JWTAuthAPI(authOptions);
    }

    /**
     * Retrieves the token from the given token path and authentication info.
     *
     * @param tokenPath the token path
     * @param authInfo  the authentication info
     * @return a {@link CompletableFuture} holding the token credentials
     */
    public CompletableFuture<TokenCredentials> token(@NotNull String tokenPath, @NotNull final JSONObject authInfo) {
        log.debug("Requesting token from: {}, and authentication info: {}", authOptions.getSite() + tokenPath, authInfo);
        return api.token(tokenPath, authInfo)
                .thenCompose(json -> {
                    log.info("Received token: {}", json);
                    if (json.has("token")) {
                        return CompletableFuture.completedFuture(new TokenCredentials(json.getString("token")));
                    } else {
                        return CompletableFuture.failedFuture(new AuthenticationException("Invalid JWT token"));
                    }
                });
    }

    /**
     * Authenticates the given {@link TokenCredentials} and returns a user.
     *
     * @param credentials token credentials containing the
     *                    information for authenticating the user.
     * @return a {@link CompletableFuture} holding the {@link User} object.
     */
    @Override
    @NotNull
    public CompletableFuture<User> authenticate(@NotNull final TokenCredentials credentials) {
        try {
            credentials.validate(null);
        } catch (CredentialValidationException ex) {
            log.error("JWT token validation failed", ex);
            return CompletableFuture.failedFuture(ex);
        }

        final JSONObject payload;
        try {
            final String encodedJwtPayload = JWT.decode(credentials.getToken()).getPayload();
            final String decodedJwtPayload = new String(BASE64_DECODER.decode(encodedJwtPayload));
            payload = new JSONObject(decodedJwtPayload);
        } catch (JWTDecodeException ex) {
            log.error("JWT token decoding failed", ex);
            return CompletableFuture.failedFuture(ex);
        }

        // validate audience
        if (options.getAudience() != null && payload.has("aud")) {
            JSONArray target;
            if (payload.get("aud") instanceof String) {
                target = new JSONArray().put(payload.getString("aud"));
            } else {
                target = payload.getJSONArray("aud");
            }

            if (Collections.disjoint(options.getAudience(), target.toList())) {
                return CompletableFuture.failedFuture(
                        new AuthenticationException("Invalid JWT audience, expected: "
                                + new JSONObject(options.getAudience())));
            }
        }

        // validate issuer
        if (options.getIssuer() != null && payload.has("iss")) {
            if (!options.getIssuer().equals(payload.getString("iss"))) {
                return CompletableFuture.failedFuture(
                        new AuthenticationException("Invalid JWT issuer, expected: " + options.getIssuer()));
            }
        }

        // create user
        final User user = createUser(credentials.getToken(), payload);

        // TODO: verify if token has expired

        return CompletableFuture.completedFuture(user);
    }

    private User createUser(@NotNull final String token, @NotNull final JSONObject payload) {
        Objects.requireNonNull(token, "token can not be null");
        Objects.requireNonNull(payload, "payload can not be null");

        // Store the JWT metadata
        final JSONObject jwtJSON = new JSONObject().put("token", token).put("tokenType", "access_token");

        // "amr": OPTIONAL. Authentication Methods References. JSON array of strings that are identifiers for
        // authentication methods used in the authentication. For instance, values might indicate that both password
        // and OTP authentication methods were used. The definition of particular values to be used in the amr Claim
        // is beyond the scope of this specification. Parties using this claim will need to agree upon the meanings
        // of the values used, which may be context-specific. The amr value is an array of case-sensitive strings.
        if (payload.has("amr")) {
            jwtJSON.put("amr", payload.getJSONArray("amr"));
        }

        // "sub": REQUIRED. Subject Identifier. A locally unique and never reassigned identifier within the Issuer
        // for the End-User, which is intended to be consumed by the Client,
        // e.g., 24400320 or AItOawmwtWwcT0k51BayewNvutrJUqsvl6qs7A4.
        // It MUST NOT exceed 255 ASCII characters in length. The sub value is a case-sensitive string.
        if (payload.has("sub")) {
            jwtJSON.put("sub", payload.getString("sub"));
        }

        // "exp": REQUIRED. Expiration time on or after which the ID Token MUST NOT be accepted for processing.
        // The processing of this parameter requires that the current date/time MUST be before the expiration
        // date/time listed in the value. Implementers MAY provide for some small leeway, usually no more than
        // a few minutes, to account for clock skew. Its value is a JSON number representing the number of seconds
        // from 1970-01-01T0:0:0Z as measured in UTC until the date/time. See RFC 3339 [RFC3339] for details regarding
        // date/times in general and UTC in particular.
        if (payload.has("exp")) {
            jwtJSON.put("exp", payload.getLong("exp"));
        }

        // "iat": REQUIRED. Time at which the JWT was issued. Its value is a JSON number representing the number
        // of seconds from 1970-01-01T0:0:0Z as measured in UTC until the date/time.
        if (payload.has("iat")) {
            jwtJSON.put("iat", payload.getLong("iat"));
        }

        // "nbf": OPTIONAL. Time before which the JWT MUST NOT be accepted for processing. The processing of
        // the nbf claim requires that the current date/time MUST be after or equal to the not-before date/time
        // listed in the nbf claim. Implementers MAY provide for some small leeway, usually no more than a few
        // minutes, to account for clock skew. Its value is a JSON number representing the number of seconds from
        // 1970-01-01T0:0:0Z as measured in UTC until the date/time. See RFC 3339 [RFC3339] for details regarding
        // date/times in general and UTC in particular.
        if (payload.has("nbf")) {
            jwtJSON.put("nbf", payload.getLong("nbf"));
        }

        // Retrieve the user's name
        final JSONObject userJSON = new JSONObject();
        if (payload.has("name")) {
            userJSON.put(Authentication.KEY_NAME, payload.getString("name"));
        } else if (payload.has("username")) {
            userJSON.put(Authentication.KEY_NAME, payload.getString("username"));
        } else if (payload.has("email")) {
            userJSON.put(Authentication.KEY_NAME, payload.getString("email"));
        }

        // Retrieve the user's roles/permissions
        if (payload.has("roles")) {
            userJSON.put(Authentication.KEY_ROLES, payload.getJSONArray("roles"));
        } else if (payload.has("permissions")) {
            userJSON.put(Authentication.KEY_ROLES, payload.getJSONArray("permissions"));
        } else if (payload.has("perms")) {
            userJSON.put(Authentication.KEY_ROLES, payload.getJSONArray("perms"));
        }

        // Store the JWT metadata in the user's attributes
        userJSON.put(Authentication.KEY_ATTRIBUTES, new JSONObject().put("auth", jwtJSON));

        // Create authentication instance
        return Authentication.create(userJSON);
    }
}
