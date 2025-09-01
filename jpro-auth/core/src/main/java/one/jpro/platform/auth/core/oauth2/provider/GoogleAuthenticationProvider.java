package one.jpro.platform.auth.core.oauth2.provider;

import io.jsonwebtoken.io.Encoders;
import javafx.stage.Stage;
import one.jpro.platform.auth.core.http.HttpMethod;
import one.jpro.platform.auth.core.oauth2.OAuth2API;
import one.jpro.platform.auth.core.oauth2.OAuth2AuthenticationProvider;
import one.jpro.platform.auth.core.oauth2.OAuth2Flow;
import one.jpro.platform.auth.core.oauth2.OAuth2Options;
import one.jpro.platform.auth.core.utils.AuthUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Simplified factory to create an {@link OAuth2AuthenticationProvider} for Google.
 *
 * @author Besmir Beqiri
 */
public class GoogleAuthenticationProvider extends OpenIDAuthenticationProvider {

    public static final List<String> DEFAULT_SCOPES = List.of("openid", "email", "profile");

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
                .setIntrospectionPath("https://oauth2.googleapis.com/tokeninfo")
                .setRevocationPath("https://oauth2.googleapis.com/revoke")
                .setUserInfoParams(new JSONObject().put("alt", "json"))));
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

        return new GoogleAuthenticationProvider(stage,
                new OAuth2Options(options)
                        .setSite(site)
                        .setUserInfoParams(new JSONObject().put("alt", "json")))
                .discover();
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
            final JSONObject headers = new JSONObject();

            final boolean confidentialClient = options.getClientId() != null && options.getClientSecret() != null;
            if (confidentialClient) {
                String basic = options.getClientId() + ":" + options.getClientSecret();
                headers.put("Authorization", "Basic " +
                        Base64.getEncoder().encodeToString(basic.getBytes(StandardCharsets.UTF_8)));
            }

            final String path = options.getIntrospectionPath() + "?" + tokenType + "=" + token;

            headers.put("Content-Type", "application/x-www-form-urlencoded");
            // specify preferred accepted accessToken type
            headers.put("Accept", "application/json,application/x-www-form-urlencoded;q=0.9");

            return fetch(HttpMethod.POST, path, headers, null)
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
                            return CompletableFuture.completedFuture(json);
                        }
                    });
        }
    }
}
