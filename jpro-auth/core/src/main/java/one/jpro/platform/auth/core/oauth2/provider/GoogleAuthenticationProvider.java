package one.jpro.platform.auth.core.oauth2.provider;

import javafx.stage.Stage;
import one.jpro.platform.auth.core.http.HttpMethod;
import one.jpro.platform.auth.core.oauth2.OAuth2API;
import one.jpro.platform.auth.core.oauth2.OAuth2AuthenticationProvider;
import one.jpro.platform.auth.core.oauth2.OAuth2Flow;
import one.jpro.platform.auth.core.jwt.JWTOptions;
import one.jpro.platform.auth.core.oauth2.OAuth2Options;
import one.jpro.platform.auth.core.utils.AuthUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Simplified factory to create an {@link OAuth2AuthenticationProvider} for Google.
 *
 * @author Besmir Beqiri
 */
public class GoogleAuthenticationProvider extends OpenIDAuthenticationProvider {

    public static final List<String> DEFAULT_SCOPES = List.of("openid", "email", "profile");
    private static final String TOKEN_INFO = "https://oauth2.googleapis.com/tokeninfo";

    /**
     * Create an {@link OAuth2AuthenticationProvider} for Google.
     *
     * @param stage   the JavaFX application stage
     * @param options custom OAuth2 options
     */
    public GoogleAuthenticationProvider(@Nullable final Stage stage, @NotNull final OAuth2Options options) {
        super(stage, new GoogleOAuth2API(options));
    }

    /**
     * Create an {@link OAuth2AuthenticationProvider} for Google.
     *
     * @param stage        the JavaFX application stage
     * @param clientId     the client id given to you by Google
     * @param clientSecret the client secret given to you by Google
     */
    public GoogleAuthenticationProvider(@Nullable final Stage stage,
                                        @NotNull final String clientId,
                                        @NotNull final String clientSecret) {
        super(stage, new GoogleOAuth2API(new OAuth2Options()
                .setFlow(OAuth2Flow.AUTH_CODE)
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setSupportedScopes(DEFAULT_SCOPES)
                .setSite("https://accounts.google.com")
                .setTokenPath("https://oauth2.googleapis.com/token")
                .setAuthorizationPath("/o/oauth2/v2/auth")
                .setUserInfoPath("https://www.googleapis.com/oauth2/v1/userinfo")
                .setJwkPath("https://www.googleapis.com/oauth2/v3/certs")
                .setIntrospectionPath(TOKEN_INFO)
                .setRevocationPath("https://oauth2.googleapis.com/revoke")
                .setUserInfoParams(new JSONObject().put("alt", "json"))
                .setJWTOptions(new JWTOptions().setIssuer("https://accounts.google.com"))));
    }

    /**
     * Create an {@link OAuth2AuthenticationProvider} for OpenID Connect Discovery. The discovery will use the default
     * site in the configuration options and attempt to load the well-known descriptor. If a site is provided, then
     * it will be used to do the lookup.
     *
     * @param stage   the JavaFX application stage
     * @param options custom OAuth2 options
     * @return a future with the instantiated {@link OpenIDAuthenticationProvider}
     */
    public static CompletableFuture<OpenIDAuthenticationProvider> discover(@Nullable final Stage stage,
                                                                           @NotNull final OAuth2Options options) {
        final String site = options.getSite() == null ? "https://accounts.google.com" : options.getSite();
        final String tokenInfo = options.getIntrospectionPath() == null ? TOKEN_INFO : options.getIntrospectionPath();

        return new GoogleAuthenticationProvider(stage,
                new OAuth2Options(options)
                        .setSite(site)
                        .setUserInfoParams(new JSONObject().put("alt", "json")))
                .discover()
                .thenApply(provider -> {
                    // Google's discovery document names no introspection endpoint
                    if (provider.getOptions().getIntrospectionPath() == null) {
                        provider.getOptions().setIntrospectionPath(tokenInfo);
                    }
                    return provider;
                });
    }

    /**
     * Inner class to handle OAuth2 API specific to Google.
     */
    private static class GoogleOAuth2API extends OAuth2API {

        public GoogleOAuth2API(@NotNull final OAuth2Options options) {
            super(options);
        }

        @Override
        public CompletableFuture<JSONObject> tokenIntrospection(String tokenType, String token) {
            final JSONObject headers = new JSONObject()
                    .put("Content-Type", "application/x-www-form-urlencoded")
                    .put("Accept", "application/json,application/x-www-form-urlencoded;q=0.9");
            final String payload = AuthUtils.jsonToQuery(new JSONObject().put(tokenType, token));

            return fetch(HttpMethod.POST, options.getIntrospectionPath(), headers, payload)
                    .thenCompose(response -> {
                        if (response.body() == null || response.body().isEmpty()) {
                            return CompletableFuture.failedFuture(new RuntimeException("No Body"));
                        }

                        JSONObject json;
                        if (AuthUtils.containsValue(response.headers(), "application/json")) {
                            json = new JSONObject(response.body());
                        } else if (AuthUtils.containsValue(response.headers(), "application/x-www-form-urlencoded") ||
                                AuthUtils.containsValue(response.headers(), "text/plain")) {
                            json = AuthUtils.queryToJson(response.body());
                        } else return CompletableFuture.failedFuture(
                                new RuntimeException("Cannot handle accessToken type: "
                                        + response.headers().allValues("Content-Type")));

                        if (json == null || json.has("error")) {
                            return CompletableFuture.failedFuture(
                                    new RuntimeException(AuthUtils.extractErrorDescription(json)));
                        } else {
                            AuthUtils.processNonStandardHeaders(json, response, options.getScopeSeparator());
                            // tokeninfo is not RFC 7662: a successful response describes a valid token
                            if (!json.has("active")) json.put("active", json.optLong("expires_in", 0) > 0);
                            if (!json.has("client_id") && json.has("azp")) json.put("client_id", json.getString("azp"));
                            return CompletableFuture.completedFuture(json);
                        }
                    });
        }
    }
}
