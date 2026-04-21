package one.jpro.platform.routing.filter.container

import java.lang.ref.WeakReference
import javafx.scene.Node
import one.jpro.platform.routing.{Request, Response, Route, StatefulFilter, View}
import org.slf4j.{Logger, LoggerFactory}

/**
 * Base class for filters that wrap the matched view in a persistent
 * "container" Node. The container is created lazily and reused across
 * navigations — only its inner content slot is swapped on each request.
 *
 * Subclasses implement [[createNode]], [[setContent]], and [[getContent]];
 * [[setRequest]] is an optional hook that defaults to a no-op.
 *
 * Identity is per-instance: two `ContainerFilter` objects own two distinct
 * containers, regardless of class. Because `ContainerFilter` extends
 * [[StatefulFilter]], it must be constructed at session-startup time and
 * held as a field — never inside per-request lambdas like `filterWhen`.
 */
abstract class ContainerFilter extends StatefulFilter {

  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  // The four hooks below are the implementation API for subclasses. They're
  // declared `public` so Java subclasses can override them — Scala's
  // `protected` compiles to JVM `public` for abstract methods anyway, which
  // breaks Java overrides that try to narrow access to `protected`.

  /** Create the wrapper Node. Called at most once per filter instance. */
  def createNode(): Node

  /** Replace the wrapper's content slot with the new view's content. */
  def setContent(container: Node, content: Node): Unit

  /** Read the wrapper's current content slot. */
  def getContent(container: Node): Node

  /**
   * Notify the wrapper that a new request is being processed. Default: no-op.
   * Override if the wrapper UI needs to update from request data (e.g. show
   * the current URL).
   */
  def setRequest(container: Node, request: Request): Unit = {}

  // The container we own, held via a WeakReference so it can be GC'd when
  // nothing in the live scene graph references it. Reused across requests
  // for as long as it stays alive; recreated lazily otherwise.
  //
  // Holding a strong reference here would pin the wrapper (and its inner
  // content) for the entire session lifetime — even after the user has
  // navigated away from any subtree that uses this filter. The WeakReference
  // restores the memory profile of the previous identity-via-oldContent
  // design without bringing back the property-key identity check.
  private var containerRef: WeakReference[Node] = new WeakReference(null)

  override final def apply(route: Route): Route = { request =>
    // If our previous container is still alive, present its inner content
    // as `oldContent` to the inner route — that's what nested filters
    // expect to see. Otherwise pass the request through unchanged; a fresh
    // container will be created on the response path.
    val existing = containerRef.get()
    val request2 = if (existing != null) {
      request.mapContent(_ => getContent(existing))
    } else request

    val r = route.apply(request2)
    Response(r.future.map {
      case view: View =>
        var container = containerRef.get()
        if (container == null) {
          container = createNode()
          containerRef = new WeakReference(container)
        }
        val finalContainer = container
        view.mapContent { _ =>
          setRequest(finalContainer, request)
          setContent(finalContainer, view.realContent)
          finalContainer
        }
      case other => other
    })
  }
}

object ContainerFilter {

  /**
   * Build a `ContainerFilter` from a supplier of a Node that implements the
   * [[Container]] trait. Avoids subclassing `ContainerFilter` by hand.
   *
   * Example: `route.filter(ContainerFilter.fromContainer(MyContainer::new))`
   *
   * @throws IllegalArgumentException at first use if the supplier produces
   *         an object that is not a `javafx.scene.Node`.
   */
  def fromContainer(supplier: java.util.function.Supplier[_ <: Container]): ContainerFilter =
    new ContainerFilter {
      override def createNode(): Node = supplier.get() match {
        case n: Node => n
        case other => throw new IllegalArgumentException(
          s"ContainerFilter.fromContainer requires a Node-implementing Container, " +
          s"got: ${other.getClass.getName}")
      }
      override def setContent(c: Node, x: Node): Unit =
        c.asInstanceOf[Container].setContent(x)
      override def getContent(c: Node): Node =
        c.asInstanceOf[Container].getContent()
      override def setRequest(c: Node, r: Request): Unit =
        c.asInstanceOf[Container].setRequest(r)
    }

  /**
   * Build a `ContainerFilter` from a supplier of a Node that mixes in
   * [[ReactiveContainer]] (simplefx `@Bind` vars). The simplefx-flavoured
   * counterpart of [[fromContainer]].
   */
  def fromReactiveContainer(supplier: java.util.function.Supplier[_ <: ReactiveContainer]): ContainerFilter =
    new ContainerFilter {
      override def createNode(): Node = supplier.get().asInstanceOf[Node]
      override def setContent(c: Node, x: Node): Unit =
        c.asInstanceOf[ReactiveContainer].content = x
      override def getContent(c: Node): Node =
        c.asInstanceOf[ReactiveContainer].content
      override def setRequest(c: Node, r: Request): Unit =
        c.asInstanceOf[ReactiveContainer].request = r
    }
}
