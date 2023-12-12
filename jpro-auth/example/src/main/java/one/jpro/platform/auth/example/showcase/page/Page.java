package one.jpro.platform.auth.example.showcase.page;

import javafx.scene.layout.StackPane;

/**
 * Base page class.
 *
 * @author Besmir Beqiri
 */
public class Page extends StackPane {

    private static final String DEFAULT_STYLE_CLASS = "page";

    public Page() {
        getStyleClass().add(DEFAULT_STYLE_CLASS);
    }
}
