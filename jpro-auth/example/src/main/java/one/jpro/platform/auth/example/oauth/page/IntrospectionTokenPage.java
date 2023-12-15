package one.jpro.platform.auth.example.oauth.page;

import javafx.beans.binding.Bindings;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import one.jpro.platform.auth.example.oauth.OAuthApp;
import one.jpro.platform.mdfx.MarkdownView;

/**
 * IntrospectionTokenPage
 *
 * @author Besmir Beqiri
 */
public class IntrospectionTokenPage extends Page {

    public IntrospectionTokenPage(OAuthApp loginApp) {
        final var headerLabel = new Label("Introspect token:");
        headerLabel.getStyleClass().add("header-label");

        MarkdownView markdownView = new MarkdownView();
        markdownView.getStylesheets().add("/one/jpro/mdfx/mdfx-default.css");
        markdownView.mdStringProperty().bind(Bindings.createStringBinding(() -> {
            final var introspectionInfo = loginApp.getIntrospectionInfo();
            return introspectionInfo == null ? "" : loginApp.jsonToMarkdown(introspectionInfo);
        }, loginApp.introspectionInfoProperty()));

        final var pane = new VBox(headerLabel, markdownView);
        pane.getStyleClass().add("user-info-pane");

        getChildren().add(pane);
    }
}
