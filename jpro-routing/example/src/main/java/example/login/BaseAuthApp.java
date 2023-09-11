package example.login;

import atlantafx.base.theme.Styles;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import one.jpro.auth.authentication.Options;
import one.jpro.auth.authentication.User;
import one.jpro.auth.jwt.JWTAuthenticationProvider;
import one.jpro.auth.oath2.OAuth2AuthenticationProvider;
import one.jpro.auth.oath2.OAuth2Credentials;
import one.jpro.auth.oath2.provider.GoogleAuthenticationProvider;
import one.jpro.auth.oath2.provider.KeycloakAuthenticationProvider;
import one.jpro.auth.oath2.provider.MicrosoftAuthenticationProvider;
import one.jpro.routing.Filter;
import one.jpro.routing.Route;
import one.jpro.routing.RouteApp;
import org.json.JSONArray;
import org.json.JSONObject;
import simplefx.experimental.parts.FXFuture;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Base class for the authentication example application.
 *
 * @author Besmir Beqiri
 */
public abstract class BaseAuthApp extends RouteApp {

    static final String GOOGLE_CLIENT_ID = System.getenv("GOOGLE_TEST_CLIENT_ID");
    static final String GOOGLE_CLIENT_SECRET = System.getenv("GOOGLE_TEST_CLIENT_SECRET");

    static final String AZURE_CLIENT_ID = System.getenv("AZURE_TEST_CLIENT_ID");
    static final String AZURE_CLIENT_SECRET = System.getenv("AZURE_TEST_CLIENT_SECRET");
//    private static final String AZURE_TENANT = System.getenv("AZURE_TEST_CLIENT_TENANT");

    static final String AUTH_ERROR_PATH = "/auth/error";
    static final String GOOGLE_PROVIDER_PATH = "/provider/google";
    static final String MICROSOFT_PROVIDER_PATH = "/provider/microsoft";
    static final String KEYCLOAK_PROVIDER_PATH = "/provider/keycloak";

    // User property
    private ObjectProperty<User> userProperty;

    final User getUser() {
        return userProperty == null ? null : userProperty.get();
    }

    final void setUser(User value) {
        userProperty().set(value);
    }

    /**
     * The user property contains the currently logged-in user.
     *
     * @return the user property
     */
    final ObjectProperty<User> userProperty() {
        if (userProperty == null) {
            userProperty = new SimpleObjectProperty<>(this, "user");
        }
        return userProperty;
    }

    // Introspection Info property
    private ObjectProperty<JSONObject> introspectionInfoProperty;

    final JSONObject getIntrospectionInfo() {
        return introspectionInfoProperty == null ? null : introspectionInfoProperty.get();
    }

    final void setIntrospectionInfo(JSONObject value) {
        introspectionInfoProperty().set(value);
    }

    /**
     * The introspection info property contains the introspection info of the currently logged-in user.
     *
     * @return the introspection info property
     */
    final ObjectProperty<JSONObject> introspectionInfoProperty() {
        if (introspectionInfoProperty == null) {
            introspectionInfoProperty = new SimpleObjectProperty<>(this, "introspectionInfo");
        }
        return introspectionInfoProperty;
    }

    // User info property
    private ObjectProperty<JSONObject> userInfoProperty;

    final JSONObject getUserInfo() {
        return userInfoProperty == null ? null : userInfoProperty.get();
    }

    final void setUserInfo(JSONObject value) {
        userInfoProperty().set(value);
    }

    /**
     * The user info property contains the user info of the currently logged-in user.
     *
     * @return the user info property
     */
    final ObjectProperty<JSONObject> userInfoProperty() {
        if (userInfoProperty == null) {
            userInfoProperty = new SimpleObjectProperty<>(this, "userInfo");
        }
        return userInfoProperty;
    }

    // Auth provider property
    private ObjectProperty<OAuth2AuthenticationProvider> authProviderProperty;

    final OAuth2AuthenticationProvider getAuthProvider() {
        return authProviderProperty == null ? null : authProviderProperty.get();
    }

    final void setAuthProvider(OAuth2AuthenticationProvider value) {
        authProviderProperty().set(value);
    }

    /**
     * The auth provider property contains the authentication provider.
     *
     * @return the auth provider property
     */
    final ObjectProperty<OAuth2AuthenticationProvider> authProviderProperty() {
        if (authProviderProperty == null) {
            authProviderProperty = new SimpleObjectProperty<>(this, "authProvider");
        }
        return authProviderProperty;
    }

    // Auth options property
    private ObjectProperty<Options> authOptions;

    final Options getAuthOptions() {
        return authOptions == null ? null : authOptions.get();
    }

    final void setAuthOptions(Options value) {
        authOptionsProperty().set(value);
    }

    /**
     * The option's property contains the configuration for the authentication provider.
     *
     * @return the option's property
     */
    final ObjectProperty<Options> authOptionsProperty() {
        if (authOptions == null) {
            authOptions = new SimpleObjectProperty<>(this, "providerOptions");
        }
        return authOptions;
    }

    // Error property
    private ObjectProperty<Throwable> errorProperty;

    final Throwable getError() {
        return errorProperty == null ? null : errorProperty.get();
    }

    final void setError(Throwable value) {
        errorProperty().set(value);
    }

    /**
     * The error property contains the last error that occurred.
     *
     * @return the error property
     */
    final ObjectProperty<Throwable> errorProperty() {
        if (errorProperty == null) {
            errorProperty = new SimpleObjectProperty<>(this, "error");
        }
        return errorProperty;
    }

    /**
     * Create a button for the given provider.
     *
     * @param text the provider name
     * @return a button node
     */
    Button createAuthProviderButton(String text) {
        ImageView iconView = new ImageView();
        iconView.setFitWidth(56);
        iconView.setFitHeight(56);
        Optional.ofNullable(getClass().getResourceAsStream("/images/" + text + "_Logo.png"))
                .map(inputStream -> new Image(inputStream, 0, 0, true, true))
                .ifPresent(iconView::setImage);

        Button loginButton = new Button("Login with\n" + text, iconView);
        loginButton.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT, "login-button");
        return loginButton;
    }

    HBox createButtonWithDescription(String description, String buttonText, EventHandler<ActionEvent> action) {
        final var descriptionLabel = new Label(description);
        final var spacer = new Region();
        spacer.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        final var button = new Button(buttonText);
        if (buttonText.equalsIgnoreCase("Sign In")) {
            button.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        } else if (buttonText.equalsIgnoreCase("Sign Out")) {
            button.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.DANGER);
        }
        button.setOnAction(action);
        final var hbox = new HBox(descriptionLabel, spacer, button);
        hbox.getStyleClass().add("description-box");
        return hbox;
    }

    /**
     * Convert a JSON object to Markdown format.
     *
     * @param json the JSON object
     * @return a formatted string
     */
    String jsonToMarkdown(JSONObject json) {
        return jsonToMarkdown(json, 0);
    }

    String jsonToMarkdown(JSONObject json, int level) {
        StringBuilder sb = new StringBuilder("\n");
        for (String key : json.keySet()) {
            final Object value = json.get(key);
            if (value instanceof JSONObject) {
                sb.append(" ".repeat(level * 4)).append("- ").append('`').append(key).append('`').append(": ")
                        .append(jsonToMarkdown((JSONObject) value, level + 1)).append("\n");
            } else if (value instanceof JSONArray) {
                sb.append(" ".repeat(level * 4)).append("- ").append('`').append(key).append('`').append(": ")
                        .append(jsonToMarkdown((JSONArray) value, level + 1)).append("\n");
            } else {
                sb.append(" ".repeat(level * 4)).append("- ").append('`').append(key).append('`').append(": ")
                        .append(value).append("\n");
            }
        }
        return sb.toString();
    }

    private String jsonToMarkdown(JSONArray jsonArray, int level) {
        StringBuilder sb = new StringBuilder();
        Iterator<Object> iterator = jsonArray.iterator();
        while (iterator.hasNext()) {
            Object object = iterator.next();
            if (object instanceof JSONObject) {
                sb.append('\n').append(jsonToMarkdown((JSONObject) object, level + 1));
            } else if (object instanceof JSONArray) {
                sb.append('\n').append(jsonToMarkdown((JSONArray) object, level + 1));
            } else {
                sb.append(object);
                if (iterator.hasNext()) {
                    sb.append(", ");
                }
            }
        }
        return sb.toString();
    }

    String getAuthProviderName(OAuth2AuthenticationProvider authProvider) {
        String result = "Unknown";
        if (authProvider instanceof GoogleAuthenticationProvider) {
            result = "Google";
        } else if (authProvider instanceof MicrosoftAuthenticationProvider) {
            result = "Microsoft";
        } else if (authProvider instanceof KeycloakAuthenticationProvider) {
            result = "Keycloak";
        }
        return result;
    }

    /**
     * Creates {@link Route} filter from a given {@link OAuth2AuthenticationProvider},
     * {@link OAuth2Credentials} and an operation a given user if the authentication
     * is successful.
     *
     * @param authProvider  the OAuth2 authentication provider
     * @param credentials   the OAuth2 credentials
     * @param userConsumer  operation on the given user argument
     * @param errorConsumer operation on the given error argument
     * @return a {@link Filter} object
     */
    public Filter oauth2(OAuth2AuthenticationProvider authProvider,
                         OAuth2Credentials credentials,
                         Consumer<? super User> userConsumer,
                         Consumer<? super Throwable> errorConsumer) {
        Objects.requireNonNull(authProvider, "auth provider can not be null");
        Objects.requireNonNull(credentials, "credentials can not be null");
        Objects.requireNonNull(userConsumer, "user consumer can not be null");
        Objects.requireNonNull(errorConsumer, "error consumer cannot be null");
        return (route) -> (request) -> {
            if (request.path().equals(credentials.getRedirectUri())) {
                return FXFuture.fromJava(authProvider.authenticate(credentials))
                        .flatMap(user -> {
                            setAuthProvider(authProvider);
                            userConsumer.accept(user);
                            return route.apply(request);
                        })
                        .flatRecover(ex -> {
                            errorConsumer.accept(ex);
                            return route.apply(request);
                        });
            } else {
                return route.apply(request);
            }
        };
    }

    /**
     * Creates {@link Route} filter from a given {@link OAuth2AuthenticationProvider},
     * {@link OAuth2Credentials} and an operation a given user if the authentication
     * is successful.
     *
     * @param authProvider  the JWT authentication provider
     * @param tokenPath     the token path
     * @param credentials   a JSON object with the authentication information
     * @param userConsumer  operation on the given user argument
     * @param errorConsumer operation on the given error argument
     * @return a {@link Filter} object
     */
    public Filter jwt(JWTAuthenticationProvider authProvider,
                      String tokenPath,
                      JSONObject credentials,
                      Consumer<? super User> userConsumer,
                      Consumer<? super Throwable> errorConsumer) {
        Objects.requireNonNull(authProvider, "auth provider cannot be null");
        Objects.requireNonNull(tokenPath, "token path cannot be null");
        Objects.requireNonNull(credentials, "credentials cannot be null");
        Objects.requireNonNull(userConsumer, "user consumer cannot be null");
        Objects.requireNonNull(errorConsumer, "error consumer cannot be null");
        return (route) -> (request) -> {
            if (request.path().equals("/jwt/token")) {
                return FXFuture.fromJava(authProvider.token(tokenPath, credentials)
                                .thenCompose(authProvider::authenticate))
                        .flatMap(user -> {
                            userConsumer.accept(user);
                            return route.apply(request);
                        })
                        .flatRecover(ex -> {
                            errorConsumer.accept(ex);
                            return route.apply(request);
                        });
            } else {
                return route.apply(request);
            }
        };
    }
}
