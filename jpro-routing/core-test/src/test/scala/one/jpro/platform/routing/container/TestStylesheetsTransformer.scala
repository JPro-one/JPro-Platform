package one.jpro.platform.routing.container

import javafx.collections.FXCollections
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.StackPane
import one.jpro.jmemorybuddy.JMemoryBuddy
import one.jpro.platform.routing._
import org.junit.jupiter.api.{BeforeAll, Test}
import simplefx.all._
import simplefx.core._
import simplefx.experimental._

import scala.concurrent.Await
import scala.collection.JavaConverters._

object TestStylesheetsTransformer {
  @BeforeAll
  def setup(): Unit = {
    Await.result(simplefx.cores.initializeCore(), (second))
  }
}

class TestStylesheetsTransformer {

  /** Run a route on the FX thread, await its future, return the wrapper Node. */
  private def runRequest(route: Route, req: Request): Node = {
    val result = inFX(route.apply(req).future).await
    result.asInstanceOf[Page].realContent
  }

  // ----- Container reuse -----

  @Test
  def containerIsReusedAcrossRequests(): Unit = {
    val filter = new StylesheetsTransformer("/test.css")
    val route =
      Route.empty()
        .and(Route.get("/a", _ => Response.node(new Label("A"))))
        .and(Route.get("/b", _ => Response.node(new Label("B"))))
        .transform(filter)

    val c1 = runRequest(route, Request.fromString("http://localhost/a"))
    assert(c1 != null, "first request should produce a wrapper Node")
    assert(c1.isInstanceOf[StackPane], "StylesheetsTransformer wraps in a StackPane")

    val c2 = runRequest(route, Request.fromString("http://localhost/b", c1))
    assert(c1 eq c2, "the wrapper Node should be reused across requests")
  }

  // ----- Identity isolation between two filter instances -----

  @Test
  def twoStylesheetsTransformersHaveDistinctContainers(): Unit = {
    val a = new StylesheetsTransformer("/a.css")
    val b = new StylesheetsTransformer("/b.css")

    val routeA = Route.get("/", _ => Response.node(new Label("a"))).transform(a)
    val routeB = Route.get("/", _ => Response.node(new Label("b"))).transform(b)

    val cA = runRequest(routeA, Request.fromString("http://localhost/"))
    assert(cA != null)

    val cB = runRequest(routeB, Request.fromString("http://localhost/", cA))
    assert(cB != null)

    assert(cA ne cB, "filter B must not reuse filter A's container")
  }

  // ----- Construction-context detection (transformWhen) -----

  @Test
  def constructingStatefulTransformerInsideFilterWhenFails(): Unit = {
    val route =
      Route.get("/", _ => Response.node(new Label()))
        .transformWhen(_ => true, _ => new StylesheetsTransformer("/x.css"))

    var threw = false
    try {
      runRequest(route, Request.fromString("http://localhost/"))
    } catch {
      case _: IllegalStateException => threw = true
    }
    assert(threw, "constructing StylesheetsTransformer inside transformWhen should fail fast")
  }

  @Test
  def hoistedStatefulTransformerInsideFilterWhenIsFine(): Unit = {
    val css = new StylesheetsTransformer("/x.css")
    val route =
      Route.get("/", _ => Response.node(new Label()))
        .transformWhen(_ => true, _ => css)

    val c = runRequest(route, Request.fromString("http://localhost/"))
    assert(c != null)
  }

  @Test
  def topLevelStatefulTransformerConstructionIsFine(): Unit = {
    val css = new StylesheetsTransformer("/x.css")
    val route = Route.get("/", _ => Response.node(new Label())).transform(css)
    val c = runRequest(route, Request.fromString("http://localhost/"))
    assert(c != null)
  }

  // ----- Reactive stylesheet binding (ObservableList) -----

  @Test
  def stylesheetsReflectTheSourceObservableList(): Unit = {
    val sheets = FXCollections.observableArrayList("/initial.css")
    val css = new StylesheetsTransformer(sheets)
    val route = Route.get("/", _ => Response.node(new Label())).transform(css)

    val container = runRequest(route, Request.fromString("http://localhost/")).asInstanceOf[StackPane]
    assert(container != null)
    assert(container.getStylesheets.asScala.toList == List("/initial.css"))

    inFX { sheets.add("/extra.css") }
    assert(container.getStylesheets.asScala.toList == List("/initial.css", "/extra.css"))

    inFX { sheets.remove("/initial.css") }
    assert(container.getStylesheets.asScala.toList == List("/extra.css"))
  }

  // ----- Scala by-name (simplefx) overload -----

  /** Holder class so we can use a `@Bind var` from inside a test method. */
  private class MobileToggle {
    @Bind var isMobile: Boolean = false
  }

  @Test
  def byNameOverloadTracksReactiveDependency(): Unit = {
    val toggle = inFX { new MobileToggle }
    val filter = inFX {
      Transformers.stylesheets(
        if (toggle.isMobile) List("/mobile.css") else List("/desktop.css"))
    }
    val route = Route.get("/", _ => Response.node(new Label())).transform(filter)

    val container = runRequest(route, Request.fromString("http://localhost/")).asInstanceOf[StackPane]
    assert(container != null)
    assert(container.getStylesheets.asScala.toList == List("/desktop.css"))

    inFX { toggle.isMobile = true }
    assert(container.getStylesheets.asScala.toList == List("/mobile.css"))

    inFX { toggle.isMobile = false }
    assert(container.getStylesheets.asScala.toList == List("/desktop.css"))
  }

  // ----- Memory: container is GC-collectible when no live scene holds it -----

  /** A trivial ContainerTransformer with no bindings — isolates the WeakReference
   *  fix in `ContainerTransformer` from any filter-specific reference leaks. */
  private class TrivialContainerTransformer extends ContainerTransformer {
    override def createNode(): Node = new StackPane()
    override def setContent(c: Node, x: Node): Unit =
      c.asInstanceOf[StackPane].getChildren.setAll(x)
    override def getContent(c: Node): Node = {
      val children = c.asInstanceOf[StackPane].getChildren
      if (children.isEmpty) null else children.get(0)
    }
  }

  @Test
  def trivialContainerIsCollectibleAfterStrongRefDropped(): Unit = {
    val filter = new TrivialContainerTransformer
    val route = Route.get("/", _ => Response.node(new Label("page"))).transform(filter)

    JMemoryBuddy.memoryTest { checker =>
      val container = runRequest(route, Request.fromString("http://localhost/"))
      assert(container != null)
      checker.setAsReferenced(filter)
      checker.assertCollectable(container)
    }
  }

  @Test
  def stylesheetsFilterContainerIsCollectibleAfterStrongRefDropped(): Unit = {
    val sheets = FXCollections.observableArrayList("/test.css")
    val filter = new StylesheetsTransformer(sheets)
    val route = Route.get("/", _ => Response.node(new Label("page"))).transform(filter)

    JMemoryBuddy.memoryTest { checker =>
      val container = runRequest(route, Request.fromString("http://localhost/"))
      assert(container != null)
      checker.setAsReferenced(filter)
      checker.setAsReferenced(sheets)
      checker.assertCollectable(container)
    }
  }
}
