package one.jpro.platform.mdfx;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;

import java.util.List;

/**
 * A custom control that displays syntax-highlighted code using TextMate grammars.
 */
public class MarkdownCodeBlock extends Control {

    private static final String DEFAULT_STYLE_CLASS = "markdown-code-block";
    private static final String USER_AGENT_STYLESHEET =
            MarkdownCodeBlock.class.getResource("markdown-code-block.css").toExternalForm();

    private final StringProperty code = new SimpleStringProperty(this, "code", "");
    private final StringProperty language = new SimpleStringProperty(this, "language", "");

    public MarkdownCodeBlock() {
        getStyleClass().add(DEFAULT_STYLE_CLASS);
    }

    public MarkdownCodeBlock(String code, String language) {
        this();
        setCode(code);
        setLanguage(language);
    }

    // -- code property

    public final StringProperty codeProperty() {
        return code;
    }

    public final String getCode() {
        return code.get();
    }

    public final void setCode(String code) {
        this.code.set(code);
    }

    // -- language property

    public final StringProperty languageProperty() {
        return language;
    }

    public final String getLanguage() {
        return language.get();
    }

    public final void setLanguage(String language) {
        this.language.set(language);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new MarkdownCodeBlockSkin(this);
    }

    @Override
    public String getUserAgentStylesheet() {
        return USER_AGENT_STYLESHEET;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return super.getControlCssMetaData();
    }
}
