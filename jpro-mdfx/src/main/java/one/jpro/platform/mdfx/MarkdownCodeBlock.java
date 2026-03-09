package one.jpro.platform.mdfx;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.*;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;

import java.util.ArrayList;
import java.util.Collections;
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

    private static final String DEFAULT_CODE_THEME = "/one/jpro/platform/mdfx/themes/github-light-default.json";

    // CSS-styleable property: resource path to a TextMate theme JSON file
    private static final CssMetaData<MarkdownCodeBlock, String> CODE_THEME =
            new CssMetaData<>("-mdfx-code-theme", StyleConverter.getStringConverter(), DEFAULT_CODE_THEME) {
                @Override
                public boolean isSettable(MarkdownCodeBlock node) {
                    return !node.codeTheme.isBound();
                }

                @Override
                public StyleableProperty<String> getStyleableProperty(MarkdownCodeBlock node) {
                    return node.codeTheme;
                }
            };

    private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
    static {
        List<CssMetaData<? extends Styleable, ?>> list = new ArrayList<>(Control.getClassCssMetaData());
        list.add(CODE_THEME);
        STYLEABLES = Collections.unmodifiableList(list);
    }

    private final StyleableStringProperty codeTheme = new SimpleStyleableStringProperty(CODE_THEME, this, "codeTheme", DEFAULT_CODE_THEME);

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

    // -- codeTheme styleable property

    public final StringProperty codeThemeProperty() {
        return codeTheme;
    }

    public final String getCodeTheme() {
        return codeTheme.get();
    }

    public final void setCodeTheme(String theme) {
        codeTheme.set(theme);
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
        return STYLEABLES;
    }
}
