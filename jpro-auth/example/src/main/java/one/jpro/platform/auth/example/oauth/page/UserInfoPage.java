package one.jpro.platform.auth.example.oauth.page;

import javafx.beans.binding.Bindings;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import one.jpro.platform.auth.example.oauth.OAuthApp;
import one.jpro.platform.mdfx.MarkdownView;

/**
 * User info page.
 *
 * @author Besmir Beqiri
 */
public class UserInfoPage extends Page {

    public UserInfoPage(OAuthApp loginApp) {
        final var headerLabel = new Label("UserInfo metadata:");
        headerLabel.getStyleClass().add("header-label");

        MarkdownView markdownView = new MarkdownView();
        markdownView.mdStringProperty().bind(Bindings.createStringBinding(() -> {
            final var userInfo = loginApp.getUserInfo();
            return userInfo == null ? "" : loginApp.jsonToMarkdown(userInfo);
        }, loginApp.userInfoProperty()));

        final var pane = new VBox(headerLabel, markdownView);
        pane.getStyleClass().add("user-info-pane");

        getChildren().add(pane);
    }
}
