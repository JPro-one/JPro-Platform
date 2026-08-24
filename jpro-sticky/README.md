# JPro Sticky

`jpro-sticky` pins JavaFX nodes to the scrolling viewport, either **sticky** (scrolls with the
content until it reaches an edge, then stays pinned) or **fixed** (always pinned to the viewport).
It mirrors the CSS `position` property for nodes rendered by JPro.

The same code runs on the web and the desktop, with no platform-specific branches. On the web the pin
is a compositor effect, so scrolling stays smooth without a JavaFX layout pass per scroll event. Two
setup caveats apply; see [Usage notes](#usage-notes).

## Installation

### Gradle

```groovy
dependencies {
    implementation("one.jpro.platform:jpro-sticky:0.7.2")
}
```

### Maven

```xml
<dependency>
    <groupId>one.jpro.platform</groupId>
    <artifactId>jpro-sticky</artifactId>
    <version>0.7.2</version>
</dependency>
```

**Module name:** `requires one.jpro.platform.sticky;`

## Quick start

Pin a node with the convenience methods on `Scroll`:

```java
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import one.jpro.platform.sticky.Scroll;

Scroll.setStickyPosition(header);                     // sticky, pinned to the top edge
Scroll.setFixedPosition(fab, Pos.BOTTOM_RIGHT, 24);   // fixed bottom-right corner, 24px inset
Scroll.setFixedBar(banner, Side.BOTTOM);              // fixed full-width bar at the bottom
Scroll.setFixedFullscreen(overlay);                   // fixed, filling the viewport
Scroll.clearScrollPosition(header);                   // back to normal flow
```

The modes mirror CSS `position`:

| Mode                    | Behaviour                                                              | CSS equivalent     |
|-------------------------|-----------------------------------------------------------------------|--------------------|
| `ScrollPosition.STATIC` | Default flow positioning; scrolls with the content.                   | `position: static` |
| `ScrollPosition.STICKY` | Scrolls with the content until it reaches the edge, then stays pinned. | `position: sticky` |
| `ScrollPosition.FIXED`  | Pinned to the viewport; does not move while the page scrolls.         | `position: fixed`  |

Query the current mode with `Scroll.getScrollPosition(node)`, which returns `ScrollPosition.STATIC`
when none has been set. Applying a new mode always tears down the previous one first, so switching
sticky and fixed (or clearing) never leaks listeners.

## Sticky positioning

A sticky node rides along in normal flow until it hits its pinned edge, then holds there while its
**containing block** is still on screen. By default the containing block is the node's parent:

```java
Scroll.setStickyPosition(header);              // pin to the top, bounded by the parent
Scroll.setStickyPosition(header, Side.TOP, 8); // 8px below the top edge
```

Pass an explicit container to have the node release at that container's bottom instead. This is the
CSS pattern where a section sub-header stays pinned while its own section scrolls past, then lets go:

```java
Scroll.setStickyPosition(subHeader, Side.TOP, 48, section);
```

Sticky only pins to an edge (`Side.TOP`, `BOTTOM`, `LEFT`, `RIGHT`). Centering and stretching are
fixed-only.

## Fixed positioning

A fixed node is anchored to the viewport and never moves with the page. Reach for the convenience
methods first:

```java
Scroll.setFixedPosition(node);                        // top edge
Scroll.setFixedPosition(node, Side.BOTTOM, 16);       // 16px up from the bottom (a chip, not a bar)
Scroll.setFixedPosition(node, Pos.TOP_CENTER, 24);    // any of the 9 Pos anchors, inset from pinned edges
Scroll.setFixedBar(node, Side.BOTTOM);                // full-width bar pinned to an edge
Scroll.setFixedFullscreen(node);                      // stretched to fill the viewport
```

`setFixedPosition(node, Side, offset)` pins one edge and leaves the other axis at its natural size (a
chip). For a full-span bar use `setFixedBar`.

### The anchor model

Under the convenience methods is `ScrollAnchor`, an immutable value object that resolves the
horizontal and vertical axes **independently**. Each axis takes one of: pin start, pin end, center,
or stretch (pin both edges). Those four modes cover corners, bars, toasts, and overlays. Build one
fluently from `ScrollAnchor.of()`:

```java
import one.jpro.platform.sticky.ScrollAnchor;

ScrollAnchor.of().bottom(0);                 // bottom edge
ScrollAnchor.of().bottom(24).right(24);      // bottom-right corner
ScrollAnchor.of().top(0).left(0).right(0);   // top bar: pinned top, stretched across the width
ScrollAnchor.of().top(16).centerX();         // toast: pinned near the top, horizontally centered
ScrollAnchor.of().all(0);                     // full-viewport overlay
```

Pass the anchor to `setFixedPosition`:

```java
Scroll.setFixedPosition(toast, ScrollAnchor.of().top(16).centerX());
```

Setting two conflicting things on one axis (for example `centerX()` and `left(0)`) throws
`IllegalStateException`.

## Canonical setter

The convenience methods all delegate to one canonical setter. It is the entry point for programmatic
or data-driven callers, such as applying a deserialized `ScrollPosition`:

```java
Scroll.setScrollPosition(node, ScrollPosition.STICKY, ScrollAnchor.of().top(0));
Scroll.setScrollPosition(node, ScrollPosition.FIXED,  ScrollAnchor.of().bottom(0).right(0));

// sticky with an explicit containing block:
Scroll.setScrollPosition(subHeader, ScrollPosition.STICKY, ScrollAnchor.of().top(48), section);
```

Edge-based overloads (`setScrollPosition(node, position, Side, offset)` and the bare
`setScrollPosition(node, position)`) cover the single-edge case.

**Note:** with `STICKY`, the anchor must pin edges only. An anchor that centers or stretches an axis
throws `IllegalArgumentException` (those are fixed-only). The `setStickyPosition` convenience methods
can't hit this, since they take a `Side`.

## Observability

You can observe when a sticky node is currently **pinned** ("stuck"). Applying sticky wires this up
automatically. It is sticky-only: a fixed or static node, or a sticky node with nothing to scroll
against, always reads `false`.

**CSS channel: the `:stuck` pseudo-class.** A sticky node carries the `:stuck` JavaFX pseudo-class
while it is pinned. Restyle a stuck header in JavaFX CSS with no Java, the same way you use `:hover`
or `:focused`:

```css
.site-header:stuck {
    -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.25), 12, 0, 0, 4);
}
```

**Java channel: `stuckProperty`.** For logic and bindings, observe the read-only property:

```java
import javafx.beans.property.ReadOnlyBooleanProperty;

ReadOnlyBooleanProperty stuck = Scroll.stuckProperty(header);
stuck.addListener((obs, was, is) -> elevateOnStick(is));

boolean nowPinned = Scroll.isStuck(header);   // == stuckProperty(header).get()
```

The property is stable: the same instance is returned across clear and re-apply, so a listener
attached once survives mode swaps. Clearing the position (or switching to fixed or static) sets it
back to `false` and removes `:stuck`.

## Usage notes

The same `Scroll` calls work on the web and the desktop. Two things to know:

**Sticky needs something to scroll.** A sticky node pins against its scrolling container: the page on
the web, or an enclosing `ScrollPane` on either target. A sticky node on the desktop with no
scrolling ancestor never moves, just as `position: sticky` does on a page that doesn't scroll. Fixed
has no such condition; it always pins to the viewport.

**On the web, enable native scrolling.** Sticky and fixed track the page's own scroll, so the
hosting page must scroll natively: set `nativescrolling="true"` on `<jpro-app>` and let the body
scroll. Without it there is no page scroll to follow and nothing pins. (A node that only pins inside
a `ScrollPane` doesn't need this.)

```html
<style>
  html, body {
    max-width: 100%;
    overflow-x: hidden;
    margin: 0;
  }
</style>

<jpro-app href="/app/default" nativescrolling="true" fxHeight="true" nativeZooming="true"></jpro-app>
```

### Stacking order

When pinned nodes overlap, fixed paints above sticky. Within one mode, the node whose position you
set last paints on top. Set the JavaFX `viewOrder` property to override both; a node with a lower
`viewOrder` paints in front:

```java
Scroll.setFixedPosition(dialog, Pos.CENTER);
Scroll.setFixedFullscreen(scrim);   // set later, so by default it would cover the dialog
dialog.setViewOrder(0);             // lower viewOrder wins: dialog paints in front
scrim.setViewOrder(1);
```

### Under the hood

**Pinned nodes are reparented.** While a node is fixed (web and desktop) or page-level sticky on the
web, it is moved into an overlay, leaving a placeholder in its original layout slot. So
`node.getParent()` and scene-graph lookups see it relocated until you clear the position. A sticky
node inside a `ScrollPane` is the exception: it stays in place.

By default the overlay sits at the scene root. A node moved there loses any CSS or context scoped to
its former ancestors, such as route styles or a popup container. To keep those, register an ancestor
pane as an overlay host, and the node reparents into the nearest one above it instead:

```java
Scroll.registerOverlayHost(popupContainer);
```

**On the web, the stuck flip can trail the visuals.** `:stuck` and `stuckProperty` track the
browser-viewport sync cadence, so they update up to one sync interval after the node pins. Inside a
`ScrollPane` the flip is exact.

## Running the example

`ScrollSample` covers every mode: a sticky page header, a bounded sticky sub-header, fixed elements
at every anchor kind, a `ScrollPane` card with its own sticky header, and both observability channels
(a `:stuck` drop-shadow and `stuckProperty` logging).

Run it on the web:

```shell
./gradlew jpro-sticky:example:jproRun
```

Then open <http://localhost:8080/>.
