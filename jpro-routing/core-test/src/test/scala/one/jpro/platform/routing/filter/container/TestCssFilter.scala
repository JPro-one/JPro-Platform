package one.jpro.platform.routing.filter.container

import javafx.collections.FXCollections
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.StackPane
import one.jpro.jmemorybuddy.JMemoryBuddy
import one.jpro.platform.routing._
import org.junit.jupiter.api.{BeforeAll, Test}
import simplefx.core._

import scala.concurrent.Await
import scala.collection.JavaConverters._

object TestCssFilter {
  @BeforeAll
  def setup(): Unit = {
    Await.result(simplefx.cores.initializeCore(), (second))
  }
}

class TestCssFilter {

  /** Run a route on the FX thread, await its future, return the wrapper Node. */
  private def runRequest(route: Route, req: Request): Node = {
    val result = inFX(route.apply(req).future).await
    result.asInstanceOf[View].realContent
  }

  // ----- Container reuse -----

  @Test
  def containerIsReusedAcrossRequests(): Unit = {
    val filter = new CssFilter("/test.css")
    val route =
      Route.empty()
        .and(Route.get("/a", _ => Response.node(new Label("A"))))
        .and(Route.get("/b", _ => Response.node(new Label("B"))))
        .filter(filter)

    val c1 = runRequest(route, Request.fromString("http://localhost/a"))
    assert(c1 != null, "first request should produce a wrapper Node")
    assert(c1.isInstanceOf[StackPane], "CssFilter wraps in a StackPane")

    // Feed c1 as oldContent for the next request.
    val c2 = runRequest(route, Request.fromString("http://localhost/b", c1))

    assert(c1 eq c2, "the wrapper Node should be reused across requests")
  }

  // ----- Identity isolation between two filter instances -----

  @Test
  def twoCssFiltersHaveDistinctContainers(): Unit = {
    val a = new CssFilter("/a.css")
    val b = new CssFilter("/b.css")

    val routeA = Route.get("/", _ => Response.node(new Label("a"))).filter(a)
    val routeB = Route.get("/", _ => Response.node(new Label("b"))).filter(b)

    val cA = runRequest(routeA, Request.fromString("http://localhost/"))
    assert(cA != null)

    // Pass A's container as oldContent into B — B must NOT reuse it.
    val cB = runRequest(routeB, Request.fromString("http://localhost/", cA))
    assert(cB != null)

    assert(cA ne cB, "filter B must not reuse filter A's container")
  }

  // ----- Construction-context detection (filterWhen) -----

  @Test
  def constructingStatefulFilterInsideFilterWhenFails(): Unit = {
    val route =
      Route.get("/", _ => Response.node(new Label()))
        .filterWhen(_ => true, _ => new CssFilter("/x.css"))

    var threw = false
    try {
      runRequest(route, Request.fromString("http://localhost/"))
    } catch {
      case _: IllegalStateException => threw = true
    }
    assert(threw, "constructing CssFilter inside filterWhen should fail fast")
  }

  @Test
  def hoistedStatefulFilterInsideFilterWhenIsFine(): Unit = {
    val css = new CssFilter("/x.css")
    val route =
      Route.get("/", _ => Response.node(new Label()))
        .filterWhen(_ => true, _ => css)

    val c = runRequest(route, Request.fromString("http://localhost/"))
    assert(c != null)
  }

  @Test
  def topLevelStatefulFilterConstructionIsFine(): Unit = {
    val css = new CssFilter("/x.css")
    val route = Route.get("/", _ => Response.node(new Label())).filter(css)
    val c = runRequest(route, Request.fromString("http://localhost/"))
    assert(c != null)
  }

  // ----- Reactive stylesheet binding -----

  @Test
  def stylesheetsReflectTheSourceObservableList(): Unit = {
    val sheets = FXCollections.observableArrayList("/initial.css")
    val css = new CssFilter(sheets)
    val route = Route.get("/", _ => Response.node(new Label())).filter(css)

    val container = runRequest(route, Request.fromString("http://localhost/")).asInstanceOf[StackPane]
    assert(container != null)
    assert(container.getStylesheets.asScala.toList == List("/initial.css"))

    // Mutate the source list — the container should pick it up.
    inFX {
      sheets.add("/extra.css")
    }
    assert(container.getStylesheets.asScala.toList == List("/initial.css", "/extra.css"))

    inFX {
      sheets.remove("/initial.css")
    }
    assert(container.getStylesheets.asScala.toList == List("/extra.css"))
  }

  // ----- Memory: container is GC-collectible when no live scene holds it -----

  /**
   * A trivial ContainerFilter with no bindings or other side-state — used to
   * isolate the WeakReference fix in `ContainerFilter` from any
   * filter-specific reference leaks (e.g. listener pinning in `CssFilter`).
   */
  private class TrivialContainerFilter extends ContainerFilter {
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
    // The filter is alive (held by `filter` here in the test). Without
    // the WeakReference fix in ContainerFilter, the filter would hold the
    // wrapper Node strongly via a private field, defeating GC. With the
    // fix, only the in-test `container` local pins it; once that's marked
    // collectable, JMemoryBuddy.memoryTest forces GC and asserts collection.
    val filter = new TrivialContainerFilter
    val route = Route.get("/", _ => Response.node(new Label("page"))).filter(filter)

    JMemoryBuddy.memoryTest { checker =>
      val container = runRequest(route, Request.fromString("http://localhost/"))
      assert(container != null)
      checker.setAsReferenced(filter) // we still hold the filter, that's fine
      checker.assertCollectable(container)
    }
  }

  @Test
  def cssFilterContainerIsCollectibleAfterStrongRefDropped(): Unit = {
    // Same test as above but with CssFilter — exercises the
    // WeakListChangeListener path in CssFilter that prevents the source
    // ObservableList from pinning the wrapper.
    val sheets = FXCollections.observableArrayList("/test.css")
    val filter = new CssFilter(sheets)
    val route = Route.get("/", _ => Response.node(new Label("page"))).filter(filter)

    JMemoryBuddy.memoryTest { checker =>
      val container = runRequest(route, Request.fromString("http://localhost/"))
      assert(container != null)
      checker.setAsReferenced(filter)
      checker.setAsReferenced(sheets)
      checker.assertCollectable(container)
    }
  }
}
