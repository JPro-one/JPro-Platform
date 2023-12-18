package one.jpro.platform.auth.example.oauth.page;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import one.jpro.platform.auth.core.authentication.Authentication;
import one.jpro.platform.auth.core.authentication.User;
import one.jpro.platform.auth.core.oauth2.OAuth2AuthenticationProvider;
import one.jpro.platform.auth.core.oauth2.OAuth2Options;
import one.jpro.platform.auth.example.oauth.OAuthApp;
import simplefx.experimental.parts.FXFuture;

import java.util.Optional;

import static one.jpro.platform.auth.example.oauth.OAuthApp.AUTH_ERROR_PATH;
import static one.jpro.platform.routing.LinkUtil.gotoPage;

/**
 * Signed in user page.
 *
 * @author Besmir Beqiri
 */
public class SignedInUserPage extends Page {

    public SignedInUserPage(OAuthApp loginApp) {
        final var headerLabel = new Label("Not signed in.");
        headerLabel.getStyleClass().add("header-label");

        final var authProvider = loginApp.getAuthProvider();
        if (authProvider == null) {
            getChildren().add(headerLabel);
        } else {
            headerLabel.setText("Signed in user: " + (loginApp.getUser() == null ? "" : loginApp.getUser().getName()));

            final var authInfoBox = loginApp.createButtonWithDescription(
                    "Show authentication information about this user.", "Auth Info",
                    event -> gotoPage(headerLabel, "/user/auth-info"));

            final var introspectTokenBox = loginApp.createButtonWithDescription(
                    "Introspect the access token.", "Introspect Token",
                    event -> FXFuture.fromJava(authProvider.introspect(loginApp.getUser(), "access_token"))
                            .map(json -> {
                                loginApp.setIntrospectionInfo(json);
                                gotoPage(headerLabel, "/user/introspect-token");
                                return json;
                            })
                            .exceptionally(throwable -> {
                                loginApp.setError(throwable);
                                gotoPage(headerLabel, AUTH_ERROR_PATH);
                                return null;
                            }));
            introspectTokenBox.setDisable(true);

            Optional.ofNullable(loginApp.getAuthProvider())
                    .map(OAuth2AuthenticationProvider::getOptions)
                    .map(OAuth2Options::getIntrospectionPath)
                    .filter(introspectPath -> !introspectPath.isBlank())
                    .ifPresent(refreshToken -> introspectTokenBox.setDisable(false));

            final var refreshTokenBox = loginApp.createButtonWithDescription(
                    "Use refresh token to get a new access token.", "Refresh Token",
                    event -> FXFuture.fromJava(authProvider.refresh(loginApp.getUser()))
                            .map(newUser -> {
                                loginApp.setUser(newUser);
                                gotoPage(headerLabel, "/user/refresh-token");
                                return newUser;
                            })
                            .exceptionally(throwable -> {
                                loginApp.setError(throwable);
                                gotoPage(headerLabel, AUTH_ERROR_PATH);
                                return null;
                            }));
            refreshTokenBox.setDisable(true);

            // if the user has a refresh token, enable the refresh token button
            Optional.ofNullable(loginApp.getUser()).map(Authentication::toJSON)
                    .map(json -> json.getJSONObject(User.KEY_ATTRIBUTES))
                    .map(json -> json.getJSONObject("auth"))
                    .map(json -> json.optString("refresh_token"))
                    .filter(refreshToken -> !refreshToken.isBlank())
                    .ifPresent(refreshToken -> refreshTokenBox.setDisable(false));

            final var revokeTokenBox = loginApp.createButtonWithDescription(
                    "Revoke the access token.", "Revoke Token",
                    event -> {
                        final var user = loginApp.getUser();
                        if (user == null) {
                            loginApp.setError(new IllegalStateException("User is not signed in."));
                            gotoPage(headerLabel, AUTH_ERROR_PATH);
                            return;
                        }

                        FXFuture.fromJava(authProvider.revoke(user, "access_token"))
                                .map(unused -> {
                                    // the result can be ignored
                                    gotoPage(headerLabel, "/user/revoke-token");
                                    return unused;
                                })
                                .exceptionally(throwable -> {
                                    loginApp.setError(throwable);
                                    gotoPage(headerLabel, AUTH_ERROR_PATH);
                                    return null;
                                });
                    });
            revokeTokenBox.setDisable(true);

            Optional.ofNullable(loginApp.getAuthProvider())
                    .map(OAuth2AuthenticationProvider::getOptions)
                    .map(OAuth2Options::getRevocationPath)
                    .filter(revocationPath -> !revocationPath.isBlank())
                    .ifPresent(refreshToken -> revokeTokenBox.setDisable(false));

            final var userInfoBox = loginApp.createButtonWithDescription(
                    "Get more user information from the provider.", "User Info",
                    event -> {
                        final var user = loginApp.getUser();
                        if (user == null) {
                            loginApp.setError(new IllegalStateException("User is not signed in."));
                            gotoPage(headerLabel, AUTH_ERROR_PATH);
                            return;
                        }

                        FXFuture.fromJava(authProvider.userInfo(user))
                                .map(json -> {
                                    // User information comes in JSON format.
                                    loginApp.setUserInfo(json);
                                    // Go to the user info page.
                                    gotoPage(headerLabel, "/user/user-info");
                                    return json;
                                })
                                .exceptionally(throwable -> {
                                    loginApp.setError(throwable);
                                    gotoPage(headerLabel, AUTH_ERROR_PATH);
                                    return null;
                                });
                    });
            userInfoBox.setDisable(true);

            Optional.ofNullable(loginApp.getAuthProvider())
                    .map(OAuth2AuthenticationProvider::getOptions)
                    .map(OAuth2Options::getUserInfoPath)
                    .filter(userInfopath -> !userInfopath.isBlank())
                    .ifPresent(refreshToken -> userInfoBox.setDisable(false));

            final var logoutBox = loginApp.createButtonWithDescription(
                    "Sign out from the provider.", "Sign Out",
                    event -> {
                        final var user = loginApp.getUser();
                        if (user == null) {
                            loginApp.setError(new IllegalStateException("User is not signed in."));
                            gotoPage(headerLabel, AUTH_ERROR_PATH);
                            return;
                        }

                        FXFuture.fromJava(authProvider.logout(user))
                                .map(unused -> {
                                    // the result can be ignored
                                    loginApp.setUser(null);
                                    gotoPage(headerLabel, "/user/logout");
                                    return unused;
                                })
                                .exceptionally(throwable -> {
                                    loginApp.setError(throwable);
                                    gotoPage(headerLabel, AUTH_ERROR_PATH);
                                    return null;
                                });
                    });
            logoutBox.setDisable(true);

            Optional.ofNullable(loginApp.getAuthProvider())
                    .map(OAuth2AuthenticationProvider::getOptions)
                    .map(OAuth2Options::getLogoutPath)
                    .filter(logoutPath -> !logoutPath.isBlank())
                    .ifPresent(logoutPath -> logoutBox.setDisable(false));

            final var pane = new VBox(headerLabel, authInfoBox, introspectTokenBox, refreshTokenBox,
                    revokeTokenBox, userInfoBox, logoutBox);
            pane.getStyleClass().add("signed-in-user-pane");

            getChildren().add(pane);
        }
    }
}
