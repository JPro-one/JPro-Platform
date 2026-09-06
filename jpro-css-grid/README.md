# JPro CSS Grid

A CSS Grid layout implementation for JavaFX. All properties are styleable via CSS.

## Installation

### Gradle

```groovy
dependencies {
    implementation("one.jpro.platform:jpro-css-grid:0.7.3")
}
```

### Maven

```xml
<dependency>
    <groupId>one.jpro.platform</groupId>
    <artifactId>jpro-css-grid</artifactId>
    <version>0.7.3</version>
</dependency>
```

**Module name:** `requires one.jpro.platform.cssgrid;`

## Quick Start

```java
import one.jpro.platform.cssgrid.*;

CssGrid grid = new CssGrid();
grid.setTemplateAreas("header header", "sidebar main", "footer footer");
grid.setTemplateColumns("200 1fr");
grid.setTemplateRows("auto 1fr auto");
grid.setGap(8);

Label header = new Label("Header");
Label sidebar = new Label("Sidebar");
Label main = new Label("Main");
Label footer = new Label("Footer");

CssGrid.setArea(header, "header");
CssGrid.setArea(sidebar, "sidebar");
CssGrid.setArea(main, "main");
CssGrid.setArea(footer, "footer");

grid.getChildren().addAll(header, sidebar, main, footer);
```

Or with line numbers and spans:

```java
CssGrid grid = new CssGrid();
grid.setTemplateColumns("repeat(3, 1fr)");
grid.setAutoRows("minmax(80, auto)");

CssGrid.setColumn(chart, 1, 3);   // lines 1 to 3 = two columns
CssGrid.setRowSpan(chart, 2);
CssGrid.setColumn(sidebar, -2, -1); // last column
```

## Container Properties

| Property | CSS Name | Values | Default |
|----------|----------|--------|---------|
| templateColumns | `grid-template-columns` | track list | `none` |
| templateRows | `grid-template-rows` | track list | `none` |
| templateAreas | `grid-template-areas` | `'a a' 'b c'` | `none` |
| autoColumns | `grid-auto-columns` | track list (cycled for implicit columns) | `auto` |
| autoRows | `grid-auto-rows` | track list (cycled for implicit rows) | `auto` |
| autoFlow | `grid-auto-flow` | `row`, `column`, `row-dense`, `column-dense` | `row` |
| justifyItems | `justify-items` | `start`, `end`, `center`, `stretch` | `stretch` |
| alignItems | `align-items` | `start`, `end`, `center`, `stretch` | `stretch` |
| justifyContent | `justify-content` | `start`, `end`, `center`, `stretch`, `space-between`, `space-around`, `space-evenly` | `stretch` |
| alignContent | `align-content` | `start`, `end`, `center`, `stretch`, `space-between`, `space-around`, `space-evenly` | `stretch` |
| rowGap | `row-gap` | number | `0` |
| columnGap | `column-gap` | number | `0` |

`setGap(double)` sets both row-gap and column-gap at once. `normal`, `flex-start` and `flex-end` are accepted as
aliases in CSS.

### Track sizes

A track list is a space-separated list of track sizes, written in CSS syntax both in Java (`setTemplateColumns(String)`)
and in stylesheets:

| Syntax | Meaning |
|--------|---------|
| `200`, `200px` | fixed size |
| `50%` | percentage of the content width/height (`auto` while that size is indefinite) |
| `1fr` | share of the free space (`1fr` = `minmax(auto, 1fr)`; use `minmax(0, 1fr)` to allow shrinking) |
| `auto` | the items' preferred size; absorbs free space when there are no `fr` tracks |
| `min-content`, `max-content` | the items' min / pref size |
| `minmax(min, max)` | clamps between two sizes, e.g. `minmax(100, 1fr)` |
| `repeat(n, ...)` | repeats the tracks `n` times |
| `repeat(auto-fill, ...)` | as many repetitions as fit into the container |
| `repeat(auto-fit, ...)` | like `auto-fill`, but empty tracks collapse to zero |

The same values are available as typed factories: `GridTrack.px(200)`, `GridTrack.fr(1)`, `GridTrack.auto()`,
`GridTrack.minmax(GridTrack.px(100), GridTrack.fr(1))`, `GridTrack.repeat(3, ...)`, `GridTrack.autoFill(...)`,
`GridTrack.autoFit(...)`, combined with `GridTrackList.of(...)` or `setTemplateColumns(GridTrack...)`.

Item content contributes to track sizes through the usual JavaFX `minWidth`/`prefWidth` and `minHeight`/`prefHeight`.
Rows are sized after columns, so a row's height reflects each item's `prefHeight(width)` at its resolved width.

### Named areas

`setTemplateAreas("header header", "sidebar main")` defines the explicit grid and the areas `header`, `sidebar` and
`main`. Every area also provides the line names `<name>-start` and `<name>-end`. Missing column or row tracks are
sized by `grid-auto-columns` / `grid-auto-rows`.

### CSS Example

JavaFX's CSS parser does not understand `fr`, `%` sizes, `minmax()`, `repeat()`, `span`, `/` or two-word keywords
such as `row dense`, so values containing them are written as quoted strings. Plain pixel numbers and single keywords
(`auto`, `min-content`, `center`, `row-dense`, ...) can stay unquoted.

```css
.my-grid {
    grid-template-columns: "200 1fr 2fr";
    grid-template-rows: "auto 1fr auto";
    grid-template-areas: "'header header header' 'sidebar main main' 'footer footer footer'";
    grid-auto-rows: "minmax(60, auto)";
    grid-auto-flow: row;
    justify-items: stretch;
    align-content: start;
    row-gap: 8;
    column-gap: 16;
}
```

## Child Constraints

Set via static methods on `CssGrid`:

| Constraint | Method | Description |
|------------|--------|-------------|
| grid-column | `CssGrid.setColumn(node, start)` / `setColumn(node, start, end)` | 1-based column lines; negative numbers count from the end of the explicit grid (`-1` is the last line) |
| grid-column: span n | `CssGrid.setColumnSpan(node, span)` | auto-placed, spanning `span` columns |
| grid-row | `CssGrid.setRow(node, start)` / `setRow(node, start, end)` | 1-based row lines |
| grid-row: span n | `CssGrid.setRowSpan(node, span)` | auto-placed, spanning `span` rows |
| grid-area | `CssGrid.setArea(node, name)` / `setArea(node, rowStart, colStart, rowEnd, colEnd)` | named area or explicit lines |
| longhands | `CssGrid.setColumnStart(node, GridLine)`, `setColumnEnd`, `setRowStart`, `setRowEnd` | `GridLine.AUTO`, `GridLine.at(n)`, `GridLine.span(n)`, `GridLine.named("main-start")` |
| justify-self | `CssGrid.setJustifySelf(node, value)` | overrides justify-items; `null` inherits |
| align-self | `CssGrid.setAlignSelf(node, value)` | overrides align-items; `null` inherits |
| order | `CssGrid.setOrder(node, value)` | placement order (stable sort), default `0` |
| margin | `CssGrid.setMargin(node, insets)` | per-child margin inside its area |

Items are placed by the CSS auto-placement algorithm: items with definite positions first, then items locked to a row
(or column in column flow), then the rest in order. An unknown area or line name refers to the first line after the
explicit grid, as in CSS. Items with `justify-self`/`align-self` other than `stretch` take
their preferred size within the area; stretched items respect their max size.

## GridItem — CSS-Styleable Child Wrapper

`GridItem` extends `StackPane` and exposes the child constraints as CSS-styleable properties:

```java
GridItem item = new GridItem(myButton);
item.getStyleClass().add("sidebar");
grid.getChildren().add(item);
```

```css
.sidebar {
    grid-area: sidebar;
}
.chart {
    grid-column: "1 / 3";
    grid-row: "span 2";
    justify-self: center;
    align-self: start;
    order: -1;
}
.footer {
    grid-column-start: 1;
    grid-column-end: -1;
}
```

| Property | CSS Name | Default |
|----------|----------|---------|
| columnStart / columnEnd | `grid-column-start` / `grid-column-end` | `auto` |
| rowStart / rowEnd | `grid-row-start` / `grid-row-end` | `auto` |
| – | `grid-column`, `grid-row`, `grid-area` (shorthands) | – |
| justifySelf | `justify-self` | `null` (inherit) |
| alignSelf | `align-self` | `null` (inherit) |
| order | `order` | `0` |

The Java setters `setColumn`, `setColumnSpan`, `setRow`, `setRowSpan` and `setArea` mirror the static `CssGrid`
methods. A longhand declared next to a shorthand always wins, regardless of declaration order. As with any JavaFX CSS
property, author stylesheets and inline styles override values set from Java.

## Running the Example

Interactive app with a control panel, preset layouts, and a live CSS editor:

```shell
./gradlew jpro-css-grid:example:run
```
