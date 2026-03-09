# JPro Dynamic CSS

Apply CSS strings to JavaFX scenes and parents at runtime. Each call replaces the previous CSS for that target — temp files are managed automatically. CSS is applied when the node is visible and cleaned up when it's hidden or detached.

## Installation

### Gradle

```groovy
dependencies {
    implementation("one.jpro.platform:jpro-dynamic-css:0.6.0-SNAPSHOT")
}
```

### Maven

```xml
<dependency>
    <groupId>one.jpro.platform</groupId>
    <artifactId>jpro-dynamic-css</artifactId>
    <version>0.6.0-SNAPSHOT</version>
</dependency>
```

**Module name:** `requires one.jpro.platform.dynamic.css;`

## Java API — DynamicCSSUtil

```java
import one.jpro.platform.css.DynamicCSSUtil;

// Apply CSS to a Scene or Parent
DynamicCSSUtil.setCssString(scene, ".button { -fx-background-color: red; }");
DynamicCSSUtil.setCssString(myVBox, ".label { -fx-font-size: 18; }");

// Update — previous CSS is automatically removed
DynamicCSSUtil.setCssString(scene, ".button { -fx-background-color: blue; }");

// Clear by passing null or empty string
DynamicCSSUtil.setCssString(scene, null);

// Access as a bindable StringProperty
StringProperty cssProp = DynamicCSSUtil.cssStringProperty(myVBox);
cssProp.bind(someOtherProperty);
```

Each `Scene` or `Parent` tracks its own CSS independently.

## Scala API — DynamicCSS (SimpleFX)

Adds a reactive `cssString` property to `Parent` and `Scene`:

```scala
import one.jpro.platform.css.DynamicCSS._

myPane.cssString = ".label { -fx-font-size: 24; }"

// Access the underlying StringProperty
val prop: StringProperty = myPane.cssStringProperty
```
