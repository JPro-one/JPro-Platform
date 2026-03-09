package one.jpro.platform.mdfx;

import javafx.scene.control.SkinBase;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import tm4javafx.richtext.StyleHelper;
import tm4javafx.richtext.StyleProvider;
import tm4javafx.richtext.TextFlowModel;
import tm4javafx.richtext.ThemeSettings;
import tm4java.grammar.IGrammarSource;
import tm4java.theme.IThemeSource;

import java.util.HashMap;
import java.util.Map;

/**
 * Skin for {@link MarkdownCodeBlock} that renders syntax-highlighted code
 * using tm4javafx's TextFlowModel and StyleProvider.
 */
public class MarkdownCodeBlockSkin extends SkinBase<MarkdownCodeBlock> {

    private static final Map<String, String> LANGUAGE_TO_GRAMMAR = new HashMap<>();

    static {
        LANGUAGE_TO_GRAMMAR.put("java", "grammars/java.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("javascript", "grammars/javascript.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("js", "grammars/javascript.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("python", "grammars/python.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("py", "grammars/python.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("css", "grammars/css.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("html", "grammars/html.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("xml", "grammars/xml.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("json", "grammars/json.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("yaml", "grammars/yaml.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("yml", "grammars/yaml.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("typescript", "grammars/typescript.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("ts", "grammars/typescript.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("c", "grammars/c.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("cpp", "grammars/cpp.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("c++", "grammars/cpp.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("go", "grammars/go.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("golang", "grammars/go.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("rust", "grammars/rust.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("rs", "grammars/rust.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("ruby", "grammars/ruby.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("rb", "grammars/ruby.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("swift", "grammars/swift.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("sql", "grammars/sql.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("bash", "grammars/shellscript.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("sh", "grammars/shellscript.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("shell", "grammars/shellscript.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("groovy", "grammars/groovy.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("dart", "grammars/dart.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("dockerfile", "grammars/dockerfile.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("docker", "grammars/dockerfile.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("markdown", "grammars/markdown.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("md", "grammars/markdown.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("php", "grammars/php.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("kotlin", "grammars/java.tmLanguage.json");
        LANGUAGE_TO_GRAMMAR.put("kt", "grammars/java.tmLanguage.json");
    }

    private final TextFlow textFlow;
    private TextFlowModel textFlowModel;
    private StyleProvider styleProvider;

    public MarkdownCodeBlockSkin(MarkdownCodeBlock control) {
        super(control);

        textFlow = new TextFlow();
        textFlow.getStyleClass().add("code-text-flow");
        getChildren().add(textFlow);

        control.codeProperty().addListener((_, _, _) -> updateContent());
        control.languageProperty().addListener((_, _, _) -> updateContent());

        updateContent();
    }

    private void updateContent() {
        MarkdownCodeBlock control = getSkinnable();
        String code = control.getCode();
        String language = control.getLanguage();

        if (code == null || code.isEmpty()) {
            textFlow.getChildren().clear();
            textFlow.setStyle("");
            return;
        }

        String grammarResource = language != null ? LANGUAGE_TO_GRAMMAR.get(language.toLowerCase()) : null;

        if (grammarResource != null) {
            highlightWithTextMate(code, grammarResource);
        } else {
            showPlainText(code);
        }
    }

    private void highlightWithTextMate(String code, String grammarResource) {
        try {
            styleProvider = new StyleProvider();
            styleProvider.setGrammar(
                    IGrammarSource.fromResource(MarkdownCodeBlock.class, grammarResource)
            );
            styleProvider.setTheme(
                    IThemeSource.fromResource(MarkdownCodeBlock.class, "themes/github-light-default.json")
            );

            textFlowModel = new TextFlowModel();
            textFlowModel.setTextFlow(textFlow);
            textFlowModel.setStyleProvider(styleProvider);
            textFlowModel.setText(code);

            ThemeSettings settings = styleProvider.getThemeSettings();
            StyleHelper.applyThemeSettings(textFlow, settings);
        } catch (Exception e) {
            showPlainText(code);
        }
    }

    private void showPlainText(String code) {
        textFlow.getChildren().clear();
        textFlow.setStyle("");
        Text text = new Text(code);
        text.getStyleClass().add("code-plain-text");
        textFlow.getChildren().add(text);
    }

    @Override
    public void dispose() {
        if (textFlowModel != null) {
            textFlowModel.setTextFlow(null);
        }
        super.dispose();
    }
}
