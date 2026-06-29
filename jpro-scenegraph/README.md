# JPro Scenegraph

`jpro-scenegraph` serializes a JavaFX scene graph into a compact string representation that is both
human- and AI-readable. It is useful for debugging, logging, snapshot tests, or feeding a UI tree
to an LLM.

## Dependency

```groovy
dependencies {
    implementation("one.jpro.platform:jpro-scenegraph:0.7.2")
}
```

## Usage

`SceneGraphSerializer.serialize(Node)` walks the node and its descendants and returns an indented
text tree:

```java
import one.jpro.platform.scenegraph.SceneGraphSerializer;

String tree = SceneGraphSerializer.serialize(myRootNode);
System.out.println(tree);
```

Layout containers (`VBox`, `HBox`, `StackPane`, `GridPane`, `ScrollPane`) are rendered with their
children nested by indentation; leaf controls such as `Label` and `Button` are rendered inline with
their text. The method is static and side-effect free.
