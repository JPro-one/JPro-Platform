
## JPro MDFX

### Introduction


JPro MDFX is a simple markdown-renderer for JavaFX.
It's based on [flexmark-java](https://github.com/vsch/flexmark-java).
It is used to render the [documentation for jpro](https://www.jpro.one/?page=docs/current/1.1/) at [jpro.one](https://www.jpro.one/).


Usage:
```
import one.jpro.platform.mdfx.MarkdownView;

MarkdownView mdfx = new MarkdownView("your-markdown");
```

Simple Application:
[Source Code](https://github.com/jpro-one/markdown-javafx-renderer/blob/master/example/src/main/java/com/sandec/mdfx/ExampleMDFX.java)

Feature Overview:
[Reference-Markdown-File](https://github.com/jpro-one/markdown-javafx-renderer/blob/master/example/src/main/resources/com/sandec/mdfx/sample.md)


You can personalize the looking of your markdown via css.
[Minimal default-file](https://github.com/jpro-one/markdown-javafx-renderer/blob/master/src/main/resources/com/sandec/mdfx/mdfx-default.css)
To use your own css, you have to overwrite the `getDefaultStylehsheets`.
```java
protected List<String> getDefaultStylehsheets() {
    return List.of("/your-css.css");
}
```
Alternatively, you can also return an empty list, and add your css via the `getStylesheets` of the Scene.