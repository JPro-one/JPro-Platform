package one.jpro.platform.routing.filter.container

import javafx.scene.Node
import one.jpro.platform.routing.Request
import simplefx.all._
import simplefx.core._
import simplefx.experimental._

/**
 * Marker trait for wrapper Nodes that expose `content` and `request` as
 * reactive (simplefx `@Bind`) vars. Mixing this trait into a `Node` subclass
 * lets you use it with [[ContainerFilter.fromReactiveContainer]] without
 * writing a four-method bridge yourself.
 *
 * Compare with [[Container]], which exposes the same data via JavaFX
 * `ObjectProperty` listeners. Use `Container` from Java; use
 * `ReactiveContainer` from Scala when you want simplefx DSL features like
 * `request --> { ... }` reactive subscriptions inside the container body.
 *
 * The self-type `Node` ensures the trait can only be mixed into Node
 * subclasses — `class MyContainer extends VBox with ReactiveContainer`.
 */
trait ReactiveContainer { self: Node =>
  @Bind var content: Node = null
  @Bind var request: Request = null
}
