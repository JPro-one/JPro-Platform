package one.jpro.platform.auth.example.oauth.page;

import javafx.beans.binding.Bindings;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import one.jpro.platform.auth.example.oauth.OAuthApp;
import one.jpro.platform.mdfx.MarkdownView;

/**
 * Authorization info page.
 *
 * @author Besmir Beqiri
 */
public class AuthInfoPage extends Page {

    public AuthInfoPage(OAuthApp loginApp) {
        final var headerLabel = new Label("Authentication information:");
        headerLabel.getStyleClass().add("header-label");

        MarkdownView markdownView = new MarkdownView();
        markdownView.mdStringProperty().bind(Bindings.createStringBinding(() -> {
            final var user = loginApp.getUserSession().getUser();
            return user == null ? "" : loginApp.jsonToMarkdown(user.toJSON());
        }));

        final var pane = new VBox(headerLabel, markdownView);
        pane.getStyleClass().add("auth-info-pane");

        getChildren().add(pane);
    }
}
