# JPro FlexBox

A CSS FlexBox layout implementation for JavaFX. All properties are styleable via CSS.

## Installation

### Gradle

```groovy
dependencies {
    implementation("one.jpro.platform:jpro-flexbox:0.6.0-SNAPSHOT")
}
```

### Maven

```xml
<dependency>
    <groupId>one.jpro.platform</groupId>
    <artifactId>jpro-flexbox</artifactId>
    <version>0.6.0-SNAPSHOT</version>
</dependency>
```

**Module name:** `requires jpro.platform.jpro.flexbox;`

## Quick Start

```java
import one.jpro.platform.flexbox.*;

FlexBox flexBox = new FlexBox();
flexBox.setDirection(FlexDirection.ROW);
flexBox.setWrap(FlexWrap.WRAP);
flexBox.setJustifyContent(FlexJustifyContent.SPACE_BETWEEN);
flexBox.setAlignItems(FlexAlignItems.CENTER);
flexBox.setGap(12);

Button btn1 = new Button("Home");
Button btn2 = new Button("About");
Button btn3 = new Button("Contact");

FlexBox.setGrow(btn2, 1);  // btn2 takes remaining space

flexBox.getChildren().addAll(btn1, btn2, btn3);
```

## Container Properties

| Property | CSS Name | Values | Default |
|----------|----------|--------|---------|
| direction | `flex-direction` | `row`, `row-reverse`, `column`, `column-reverse` | `row` |
| wrap | `flex-wrap` | `nowrap`, `wrap`, `wrap-reverse` | `nowrap` |
| justifyContent | `justify-content` | `flex-start`, `flex-end`, `center`, `space-between`, `space-around`, `space-evenly` | `flex-start` |
| alignItems | `align-items` | `flex-start`, `flex-end`, `center`, `stretch`, `baseline` | `stretch` |
| alignContent | `align-content` | `flex-start`, `flex-end`, `center`, `stretch`, `space-between`, `space-around`, `space-evenly` | `stretch` |
| rowGap | `row-gap` | number | `0` |
| columnGap | `column-gap` | number | `0` |

`setGap(double)` sets both row-gap and column-gap at once.

### CSS Example

```css
.my-flexbox {
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: center;
    align-items: stretch;
    row-gap: 8;
    column-gap: 16;
}
```

## Child Constraints

Set via static methods on `FlexBox`:

| Constraint | Method | Default | Description |
|------------|--------|---------|-------------|
| flex-grow | `FlexBox.setGrow(node, value)` | `0` | How much the item grows to fill free space |
| flex-shrink | `FlexBox.setShrink(node, value)` | `1` | How much the item shrinks when space is insufficient |
| flex-basis | `FlexBox.setBasis(node, value)` | `-1` (auto) | Initial main size. `-1` = use prefWidth/prefHeight |
| align-self | `FlexBox.setAlignSelf(node, value)` | `null` (inherit) | Overrides align-items for this child |
| order | `FlexBox.setOrder(node, value)` | `0` | Controls visual ordering (stable sort) |
| margin | `FlexBox.setMargin(node, insets)` | `Insets.EMPTY` | Per-child margin |

## FlexItem — CSS-Styleable Child Wrapper

`FlexItem` extends `StackPane` and exposes child constraints as CSS-styleable properties:

```java
FlexItem item = new FlexItem(myButton);
item.getStyleClass().add("sidebar");
flexBox.getChildren().add(item);
```

```css
.sidebar {
    flex-grow: 0;
    flex-shrink: 0;
    flex-basis: 200;
    order: -1;
    align-self: stretch;
}
```

| Property | CSS Name | Default |
|----------|----------|---------|
| flexGrow | `flex-grow` | `0` |
| flexShrink | `flex-shrink` | `1` |
| flexBasis | `flex-basis` | `-1` (auto) |
| order | `order` | `0` |
| alignSelf | `align-self` | `null` |

## Running the Example

Interactive app with a control panel, preset layouts, and a live CSS editor:

```shell
./gradlew jpro-flexbox:example:run
```
