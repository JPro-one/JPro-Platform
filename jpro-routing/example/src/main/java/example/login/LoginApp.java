package example.login;

import atlantafx.base.theme.PrimerLight;
import com.sandec.mdfx.MarkdownView;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import one.jpro.auth.AuthAPI;
import one.jpro.auth.authentication.Authentication;
import one.jpro.auth.authentication.User;
import one.jpro.auth.oath2.OAuth2AuthenticationProvider;
import one.jpro.auth.oath2.OAuth2Credentials;
import one.jpro.auth.oath2.OAuth2Options;
import one.jpro.routing.Route;
import one.jpro.routing.dev.DevFilter;
import simplefx.experimental.parts.FXFuture;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import static one.jpro.routing.LinkUtil.gotoPage;
import static one.jpro.routing.RouteUtils.getNode;
import static one.jpro.routing.RouteUtils.redirect;

/**
 * An example application to show how to use the Authorization module in general
 * combined with the Routing module and various supported authentication providers.
 *
 * @author Besmir Beqiri
 */
public class LoginApp extends BaseAuthApp {

    @Override
    public Route createRoute() {
        Optional.ofNullable(LoginApp.class.getResource("/style.css"))
                .map(URL::toExternalForm)
                .ifPresent(css -> getScene().getStylesheets().add(css));
        getScene().setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        // Google Auth provider
        final var googleAuth = AuthAPI.googleAuth()
                .clientId(GOOGLE_CLIENT_ID)
                .clientSecret(GOOGLE_CLIENT_SECRET)
                .create(getStage());

        final var googleCredentials = new OAuth2Credentials()
                .setScopes(List.of("openid", "email"))
                .setRedirectUri("/auth/google");

        // Microsoft Auth provider
        final var microsoftAuth = AuthAPI.microsoftAuth()
                .clientId(AZURE_CLIENT_ID)
                .clientSecret(AZURE_CLIENT_SECRET)
                .tenant("common")
                .create(getStage());

        final var microsoftCredentials = new OAuth2Credentials()
                .setScopes(List.of("openid", "email"))
                .setRedirectUri("/auth/microsoft");

        // Keycloak Auth provider
        final var keycloakAuth = AuthAPI.keycloakAuth()
                .site("http://localhost:8080/realms/{realm}")
                .clientId("myclient")
                .clientSecret("5Rx63jCLPmTGhdqNaDDad0mqu5m0aOoN")
                .realm("myrealm")
                .create(getStage());

        final var keycloakCredentials = new OAuth2Credentials()
                .setScopes(List.of("openid", "email"))
                .setRedirectUri("/auth/keycloak");

        return Route.empty()
                .and(getNode("/", (r) -> loginView()))
                .path("/user", Route.empty()
                        .and(getNode("/console", (r) -> signedInUserView()))
                        .and(getNode("/auth-info", (r) -> authInfoView()))
                        .and(getNode("/introspect-token", (r) -> introspectTokenView()))
                        .and(getNode("/refresh-token", (r) -> refreshTokenView()))
                        .and(getNode("/revoke-token", (r) -> loginView()))
                        .and(getNode("/user-info", (r) -> userInfoView()))
                        .and(getNode("/logout", (r) -> loginView())))
                .path("/auth", Route.empty()
                        .and(redirect("/google", "/user/console"))
                        .and(redirect("/microsoft", "/user/console"))
                        .and(redirect("/keycloak", "/user/console"))
                        .and(getNode("/error", (r) -> errorView())))
                .path("/provider", Route.empty()
                        .and(getNode("/google", (r) -> authProviderView(googleAuth, googleCredentials)))
                        .and(getNode("/microsoft", (r) -> authProviderView(microsoftAuth, microsoftCredentials)))
                        .and(getNode("/keycloak", (r) -> authProviderView(keycloakAuth, keycloakCredentials)))
                        .path("/discovery", Route.empty()
                                .and(getNode("/google", (r) -> providerDiscoveryView(googleAuth)))
                                .and(getNode("/microsoft", (r) -> providerDiscoveryView(microsoftAuth)))
                                .and(getNode("/keycloak", (r) -> providerDiscoveryView(keycloakAuth)))))
                .filter(DevFilter.create())
                .filter(oauth2(googleAuth, googleCredentials, this::setUser, this::setError))
                .filter(oauth2(microsoftAuth, microsoftCredentials, this::setUser, this::setError))
                .filter(oauth2(keycloakAuth, keycloakCredentials, this::setUser, this::setError));
    }

    public Node loginView() {
        final var headerLabel = new Label("Authentication Module");
        headerLabel.getStyleClass().add("header-label");

        final var selectLabel = new Label("Select an authentication provider:");
        selectLabel.getStyleClass().add("header2-label");

        final var googleProviderButton = createAuthProviderButton("Google");
        googleProviderButton.setOnAction(event -> gotoPage(googleProviderButton, GOOGLE_PROVIDER_PATH));

        final var microsoftProviderButton = createAuthProviderButton("Microsoft");
        microsoftProviderButton.setOnAction(event -> gotoPage(microsoftProviderButton, MICROSOFT_PROVIDER_PATH));

        final var keycloakProviderButton = createAuthProviderButton("Keycloak");
        keycloakProviderButton.setOnAction(event -> gotoPage(keycloakProviderButton, KEYCLOAK_PROVIDER_PATH));

        final var tilePane = new TilePane(googleProviderButton, microsoftProviderButton, keycloakProviderButton);
        tilePane.getStyleClass().add("tile-pane");
        VBox.setVgrow(tilePane, Priority.ALWAYS);

        final var pane = new VBox(headerLabel, selectLabel, tilePane);
        pane.getStyleClass().add("login-pane");

        final var stackPane = new StackPane(pane);
        stackPane.getStyleClass().add("page");
        return stackPane;
    }

    public Node authProviderView(final OAuth2AuthenticationProvider authProvider,
                                 final OAuth2Credentials authCredentials) {
        final var headerLabel = new Label("Authentication Provider: " + getAuthProviderName(authProvider));
        headerLabel.getStyleClass().add("header-label");

        final var pane = new VBox(headerLabel);
        pane.getStyleClass().add("auth-provider-pane");

        final var authOptions = authProvider.getOptions();

        final var siteLabel = new Label("Site:");
        final var siteField = new TextField(authOptions.getSite());
        siteField.setEditable(false);
        pane.getChildren().addAll(siteLabel, siteField);

        Optional.ofNullable(authOptions.getTenant()).ifPresent(tenant -> {
            final var tenantLabel = new Label("Tenant:");
            final var tenantField = new TextField(tenant);
            tenantField.setEditable(false);
            pane.getChildren().addAll(tenantLabel, tenantField);
        });

        final var clientIdLabel = new Label("Client ID:");
        final var clientIdField = new TextField(authOptions.getClientId());
        clientIdField.setEditable(false);
        pane.getChildren().addAll(clientIdLabel, clientIdField);

        Optional.ofNullable(authOptions.getClientSecret()).ifPresent(clientSecret -> {
            final var clientSecretLabel = new Label("Client Secret:");
            final var clientSecretField = new TextField(clientSecret);
            clientSecretField.setEditable(false);
            pane.getChildren().addAll(clientSecretLabel, clientSecretField);
        });

        final var scopesLabel = new Label("Scopes:");
        final var scopesField = new TextField(String.join(", ", authCredentials.getScopes()));
        scopesField.setEditable(false);
        pane.getChildren().addAll(scopesLabel, scopesField);

        final var redirectUriLabel = new Label("Redirect URI:");
        final var redirectUriField = new TextField(authCredentials.getRedirectUri());
        redirectUriField.setEditable(false);
        pane.getChildren().addAll(redirectUriLabel, redirectUriField);

        Optional.ofNullable(authCredentials.getNonce()).ifPresent(nonce -> {
            final var nonceLabel = new Label("Nonce:");
            final var nonceField = new TextField(nonce);
            nonceField.setEditable(false);
            pane.getChildren().addAll(nonceLabel, nonceField);
        });

        final var signInBox = createButtonWithDescription(
                "Sign in with the selected authentication provider.", "Sign In",
                event -> authProvider.authorizeUrl(authCredentials));

        final var discoveryBox = createButtonWithDescription(
                "The OpenID Connect Discovery provides a client with configuration details.",
                "Discovery", event -> FXFuture.fromJava(authProvider.discover())
                        .map(provider -> {
                            final var options = provider.getOptions();
                            setAuthOptions(options);
                            gotoPage(headerLabel, "/provider/discovery/"
                                    + getAuthProviderName(authProvider).toLowerCase());
                            return provider;
                        })
                        .recover(throwable -> {
                            setError(throwable);
                            gotoPage(headerLabel, AUTH_ERROR_PATH);
                            return null;
                        }));
        pane.getChildren().addAll(signInBox, discoveryBox);

        return new StackPane(pane);
    }

    public Node providerDiscoveryView(final OAuth2AuthenticationProvider authProvider) {
        final var headerLabel = new Label("OpenID Connect Discovery: " + getAuthProviderName(authProvider));
        headerLabel.getStyleClass().add("header-label");

        MarkdownView providerDiscoveryView = new MarkdownView();
        providerDiscoveryView.getStylesheets().add("/com/sandec/mdfx/mdfx-default.css");
        providerDiscoveryView.mdStringProperty().bind(Bindings.createStringBinding(() -> {
            final var authOptions = getAuthOptions();
            return authOptions == null ? "" : jsonToMarkdown(authOptions.toJSON());
        }, authOptionsProperty()));

        final var pane = new VBox(headerLabel, providerDiscoveryView);
        pane.getStyleClass().add("openid-provider-discovery-pane");

        return new StackPane(pane);
    }

    public Node signedInUserView() {
        final var headerLabel = new Label("Not signed in.");
        headerLabel.getStyleClass().add("header-label");

        final var authProvider = getAuthProvider();
        if (authProvider == null) {
            return new StackPane(headerLabel);
        }

        headerLabel.setText("Signed in user: " + (getUser() == null ? "" : getUser().getName()));

        final var authInfoBox = createButtonWithDescription(
                "Show authentication information about this user.", "Auth Info",
                event -> gotoPage(headerLabel, "/user/auth-info"));

        final var introspectTokenBox = createButtonWithDescription(
                "Introspect the access token.", "Introspect Token",
                event -> FXFuture.fromJava(authProvider.introspect(getUser(), "access_token"))
                        .map(json -> {
                            setIntrospectionInfo(json);
                            gotoPage(headerLabel, "/user/introspect-token");
                            return json;
                        })
                        .recover(throwable -> {
                            setError(throwable);
                            gotoPage(headerLabel, AUTH_ERROR_PATH);
                            return null;
                        }));
        introspectTokenBox.setDisable(true);

        Optional.ofNullable(getAuthProvider())
                .map(OAuth2AuthenticationProvider::getOptions)
                .map(OAuth2Options::getIntrospectionPath)
                .filter(introspectPath -> !introspectPath.isBlank())
                .ifPresent(refreshToken -> introspectTokenBox.setDisable(false));

        final var refreshTokenBox = createButtonWithDescription(
                "Use refresh token to get a new access token.", "Refresh Token",
                event -> FXFuture.fromJava(authProvider.refresh(getUser()))
                        .map(newUser -> {
                            setUser(newUser);
                            gotoPage(headerLabel, "/user/refresh-token");
                            return newUser;
                        })
                        .recover(throwable -> {
                            setError(throwable);
                            gotoPage(headerLabel, AUTH_ERROR_PATH);
                            return null;
                        }));
        refreshTokenBox.setDisable(true);

        // if the user has a refresh token, enable the refresh token button
        Optional.ofNullable(getUser()).map(Authentication::toJSON)
                .map(json -> json.getJSONObject(User.KEY_ATTRIBUTES))
                .map(json -> json.getJSONObject("auth"))
                .map(json -> json.optString("refresh_token"))
                .filter(refreshToken -> !refreshToken.isBlank())
                .ifPresent(refreshToken -> refreshTokenBox.setDisable(false));

        final var revokeTokenBox = createButtonWithDescription(
                "Revoke the access token.", "Revoke Token",
                event -> {
                    final var user = getUser();
                    if (user == null) {
                        setError(new IllegalStateException("User is not signed in."));
                        gotoPage(headerLabel, AUTH_ERROR_PATH);
                        return;
                    }

                    FXFuture.fromJava(authProvider.revoke(user, "access_token"))
                            .map(unused -> {
                                // the result can be ignored
                                gotoPage(headerLabel, "/user/revoke-token");
                                return unused;
                            })
                            .recover(throwable -> {
                                setError(throwable);
                                gotoPage(headerLabel, AUTH_ERROR_PATH);
                                return null;
                            });
                });
        revokeTokenBox.setDisable(true);

        Optional.ofNullable(getAuthProvider())
                .map(OAuth2AuthenticationProvider::getOptions)
                .map(OAuth2Options::getRevocationPath)
                .filter(revocationPath -> !revocationPath.isBlank())
                .ifPresent(refreshToken -> revokeTokenBox.setDisable(false));

        final var userInfoBox = createButtonWithDescription(
                "Get more user information from the provider.", "User Info",
                event -> {
                    final var user = getUser();
                    if (user == null) {
                        setError(new IllegalStateException("User is not signed in."));
                        gotoPage(headerLabel, AUTH_ERROR_PATH);
                        return;
                    }

                    FXFuture.fromJava(authProvider.userInfo(user))
                            .map(json -> {
                                // User information comes in JSON format.
                                setUserInfo(json);
                                // Go to the user info page.
                                gotoPage(headerLabel, "/user/user-info");
                                return json;
                            })
                            .recover(throwable -> {
                                setError(throwable);
                                gotoPage(headerLabel, AUTH_ERROR_PATH);
                                return null;
                            });
                });
        userInfoBox.setDisable(true);

        Optional.ofNullable(getAuthProvider())
                .map(OAuth2AuthenticationProvider::getOptions)
                .map(OAuth2Options::getUserInfoPath)
                .filter(userInfopath -> !userInfopath.isBlank())
                .ifPresent(refreshToken -> userInfoBox.setDisable(false));

        final var logoutBox = createButtonWithDescription(
                "Sign out from the provider.", "Sign Out",
                event -> {
                    final var user = getUser();
                    if (user == null) {
                        setError(new IllegalStateException("User is not signed in."));
                        gotoPage(headerLabel, AUTH_ERROR_PATH);
                        return;
                    }

                    FXFuture.fromJava(authProvider.logout(user))
                            .map(unused -> {
                                // the result can be ignored
                                setUser(null);
                                gotoPage(headerLabel, "/user/logout");
                                return unused;
                            })
                            .recover(throwable -> {
                                setError(throwable);
                                gotoPage(headerLabel, AUTH_ERROR_PATH);
                                return null;
                            });
                });
        logoutBox.setDisable(true);

        Optional.ofNullable(getAuthProvider())
                .map(OAuth2AuthenticationProvider::getOptions)
                .map(OAuth2Options::getLogoutPath)
                .filter(logoutPath -> !logoutPath.isBlank())
                .ifPresent(logoutPath -> logoutBox.setDisable(false));

        final var pane = new VBox(headerLabel, authInfoBox, introspectTokenBox, refreshTokenBox,
                revokeTokenBox, userInfoBox, logoutBox);
        pane.getStyleClass().add("signed-in-user-pane");

        return new StackPane(pane);
    }

    public Node authInfoView() {
        final var headerLabel = new Label("Authentication information:");
        headerLabel.getStyleClass().add("header-label");

        MarkdownView markdownView = new MarkdownView();
        markdownView.getStylesheets().add("/com/sandec/mdfx/mdfx-default.css");
        markdownView.mdStringProperty().bind(Bindings.createStringBinding(() -> {
            final var user = getUser();
            return user == null ? "" : jsonToMarkdown(user.toJSON());
        }, userProperty()));

        final var pane = new VBox(headerLabel, markdownView);
        pane.getStyleClass().add("auth-info-pane");

        return new StackPane(pane);
    }

    public Node introspectTokenView() {
        final var headerLabel = new Label("Introspect token:");
        headerLabel.getStyleClass().add("header-label");

        MarkdownView markdownView = new MarkdownView();
        markdownView.getStylesheets().add("/com/sandec/mdfx/mdfx-default.css");
        markdownView.mdStringProperty().bind(Bindings.createStringBinding(() -> {
            final var introspectionInfo = getIntrospectionInfo();
            return introspectionInfo == null ? "" : jsonToMarkdown(introspectionInfo);
        }, introspectionInfoProperty()));

        final var pane = new VBox(headerLabel, markdownView);
        pane.getStyleClass().add("user-info-pane");

        return new StackPane(pane);
    }

    public Node refreshTokenView() {
        final var headerLabel = new Label("Authentication information\n" +
                "after refreshing the access token:");
        headerLabel.getStyleClass().add("header-label");

        MarkdownView markdownView = new MarkdownView();
        markdownView.getStylesheets().add("/com/sandec/mdfx/mdfx-default.css");
        markdownView.mdStringProperty().bind(Bindings.createStringBinding(() -> {
            final var user = getUser();
            return user == null ? "" : jsonToMarkdown(user.toJSON());
        }, userProperty()));

        final var pane = new VBox(headerLabel, markdownView);
        pane.getStyleClass().add("auth-info-pane");

        return new StackPane(pane);
    }

    public Node userInfoView() {
        final var headerLabel = new Label("UserInfo metadata:");
        headerLabel.getStyleClass().add("header-label");

        MarkdownView markdownView = new MarkdownView();
        markdownView.getStylesheets().add("/com/sandec/mdfx/mdfx-default.css");
        markdownView.mdStringProperty().bind(Bindings.createStringBinding(() -> {
            final var userInfo = getUserInfo();
            return userInfo == null ? "" : jsonToMarkdown(userInfo);
        }, userInfoProperty()));

        final var pane = new VBox(headerLabel, markdownView);
        pane.getStyleClass().add("user-info-pane");

        return new StackPane(pane);
    }

    public Node errorView() {
        final var headerLabel = new Label("Something unexpected happen:");
        headerLabel.getStyleClass().add("header-label");

        final var errorLabel = new Label();
        errorLabel.setWrapText(true);
        errorLabel.getStyleClass().add("error-label");
        errorLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            final Throwable throwable = getError();
            return throwable == null ? "" : throwable.getMessage();
        }, errorProperty()));

        final var errorTextArea = new TextArea();
        errorTextArea.getStyleClass().add("error-text-area");
        VBox.setVgrow(errorTextArea, Priority.ALWAYS);
        errorTextArea.textProperty().bind(Bindings.createStringBinding(() -> {
            final Throwable throwable = getError();
            if (throwable == null) {
                return "";
            } else {
                final StringWriter sw = new StringWriter();
                final PrintWriter pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);
                return sw.toString();
            }
        }, errorProperty()));

        final var pane = new VBox(headerLabel, errorLabel, errorTextArea);
        pane.getStyleClass().add("error-pane");

        final var stackPane = new StackPane(pane);
        stackPane.getStyleClass().add("page");
        return stackPane;
    }
}
