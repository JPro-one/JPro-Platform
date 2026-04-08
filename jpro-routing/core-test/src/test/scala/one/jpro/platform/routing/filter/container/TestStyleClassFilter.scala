package one.jpro.platform.routing.filter.container

import javafx.collections.FXCollections
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.StackPane
import one.jpro.platform.routing._
import org.junit.jupiter.api.{BeforeAll, Test}
import simplefx.all._
import simplefx.core._
import simplefx.experimental._

import scala.concurrent.Await
import scala.collection.JavaConverters._

object TestStyleClassFilter {
  @BeforeAll
  def setup(): Unit = {
    Await.result(simplefx.cores.initializeCore(), (second))
  }
}

class TestStyleClassFilter {

  private def runRequest(route: Route, req: Request): Node = {
    val result = inFX(route.apply(req).future).await
    result.asInstanceOf[View].realContent
  }

  @Test
  def styleClassesAreAppliedToWrapper(): Unit = {
    val filter = new StyleClassFilter("light", "background")
    val route = Route.get("/", _ => Response.node(new Label())).filter(filter)
    val container = runRequest(route, Request.fromString("http://localhost/")).asInstanceOf[StackPane]

    assert(container.getStyleClass.asScala.toList.contains("light"))
    assert(container.getStyleClass.asScala.toList.contains("background"))
  }

  @Test
  def styleClassesReflectTheSourceObservableList(): Unit = {
    val classes = FXCollections.observableArrayList("light")
    val filter = new StyleClassFilter(classes)
    val route = Route.get("/", _ => Response.node(new Label())).filter(filter)
    val container = runRequest(route, Request.fromString("http://localhost/")).asInstanceOf[StackPane]

    assert(container.getStyleClass.asScala.toList == List("light"))

    inFX { classes.add("mobile") }
    assert(container.getStyleClass.asScala.toList == List("light", "mobile"))

    inFX { classes.remove("mobile") }
    assert(container.getStyleClass.asScala.toList == List("light"))
  }

  /** Holder class so we can use a `@Bind var` from inside a test method. */
  private class MobileToggle {
    @Bind var isMobile: Boolean = false
  }

  @Test
  def byNameOverloadTracksReactiveDependency(): Unit = {
    val toggle = inFX { new MobileToggle }
    val filter = inFX {
      Filters.styleClasses(
        if (toggle.isMobile) List("light", "mobile") else List("light"))
    }
    val route = Route.get("/", _ => Response.node(new Label())).filter(filter)
    val container = runRequest(route, Request.fromString("http://localhost/")).asInstanceOf[StackPane]

    assert(container.getStyleClass.asScala.toList == List("light"))

    inFX { toggle.isMobile = true }
    assert(container.getStyleClass.asScala.toList == List("light", "mobile"))

    inFX { toggle.isMobile = false }
    assert(container.getStyleClass.asScala.toList == List("light"))
  }

  @Test
  def stackingStylesheetsAndStyleClasses(): Unit = {
    val sheetsFilter = new StylesheetsFilter("/main.css")
    val classesFilter = new StyleClassFilter("light")
    val route =
      Route.get("/", _ => Response.node(new Label("page")))
        .filter(sheetsFilter)
        .filter(classesFilter)

    // The outer filter (last one in the chain) wraps the result, so the
    // returned wrapper is the StyleClassFilter's container; its child is
    // the StylesheetsFilter's container.
    val outer = runRequest(route, Request.fromString("http://localhost/")).asInstanceOf[StackPane]
    assert(outer.getStyleClass.asScala.toList.contains("light"))

    val inner = outer.getChildren.get(0).asInstanceOf[StackPane]
    assert(inner.getStylesheets.asScala.toList == List("/main.css"))
  }
}
