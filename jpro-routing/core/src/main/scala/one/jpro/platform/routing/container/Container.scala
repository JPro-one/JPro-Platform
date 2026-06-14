package one.jpro.platform.routing.container

import javafx.beans.property.ObjectProperty
import one.jpro.platform.routing.Request

/**
 * A persistent wrapper around the changing page content (e.g. a menu, header or footer that
 * stays in place while the routed page changes). Implement this on a `Node` and apply it with
 * `ContainerTransformer.fromContainer(...)`. The router sets the current page via `contentProperty`
 * and the current request via `requestProperty`; the container reads them to render itself.
 */
trait Container {

  /** The property holding the current page content; the router updates it on navigation. */
  def contentProperty(): ObjectProperty[javafx.scene.Node]
  /** Returns the current page content. */
  def getContent(): javafx.scene.Node = contentProperty().get()
  /** Sets the current page content (normally called by the router). */
  def setContent(x: javafx.scene.Node): Unit = contentProperty().set(x)

  /** The property holding the current request; the router updates it on navigation. */
  def requestProperty(): ObjectProperty[Request]
  /** Returns the current request. */
  def getRequest(): Request = requestProperty().get()
  /** Sets the current request (normally called by the router). */
  def setRequest(x: Request): Unit = requestProperty().set(x)

}
