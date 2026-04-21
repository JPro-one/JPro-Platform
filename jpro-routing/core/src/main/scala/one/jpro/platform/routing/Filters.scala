package one.jpro.platform.routing

import javafx.collections.{FXCollections, ObservableList}
import javafx.scene.control.Label
import one.jpro.platform.routing.filter.container.{StyleClassFilter, StylesheetsFilter}
import simplefx.all
import simplefx.all._
import simplefx.core._
import simplefx.experimental._
import scala.collection.JavaConverters._
import java.util.function.Function
import java.util.function.BiFunction

object Filters {

  // ----- Stylesheets -----

  /**
   * Wraps the matched view in a `StackPane` whose `getStylesheets()` reflects
   * the given list. Mutating the list at runtime updates the wrapper's
   * stylesheets immediately — drive it from an `isMobile` subscription, a
   * theme switcher, etc.
   *
   * Returns a [[StatefulFilter]]; do NOT call this from inside a per-request
   * lambda (`filterWhen`, `filterWhenFuture`) — hoist it to a field on your
   * RouteApp or a Site object instead.
   */
  def stylesheets(sheets: ObservableList[String]): Filter = new StylesheetsFilter(sheets)

  /** Convenience overload for a fixed (non-reactive) list of stylesheets. */
  def stylesheets(sheets: String*): Filter = new StylesheetsFilter(FXCollections.observableArrayList(sheets: _*))

  /** Java-friendly array overload. */
  def stylesheets(sheets: Array[String]): Filter = new StylesheetsFilter(FXCollections.observableArrayList(sheets: _*))

  /**
   * Scala-only by-name overload using simplefx reactive binding. The
   * `expression` is wrapped in a `Bindable`; whenever a `@Bind` value it
   * reads changes, the wrapper's stylesheets are updated automatically.
   *
   * {{{
   *   filter(Filters.stylesheets(
   *     if (isMobile) List(MAIN_CSS, MOBILE_CSS)
   *     else          List(MAIN_CSS, DESKTOP_CSS)))
   * }}}
   */
  def stylesheets(expression: => List[String]): Filter = {
    val list = FXCollections.observableArrayList[String]()
    @Bind var listB = list.toBindable
    listB <-- expression
    new StylesheetsFilter(list)
  }

  // ----- Style classes -----

  /**
   * Wraps the matched view in a `StackPane` whose `getStyleClass()` reflects
   * the given list. Same usage and lifecycle constraints as
   * [[stylesheets]] — see its docs.
   *
   * Stack with [[stylesheets]] when you want both: each is its own filter
   * and produces its own wrapper container.
   */
  def styleClasses(classes: ObservableList[String]): Filter = new StyleClassFilter(classes)

  /** Convenience overload for a fixed (non-reactive) list of style classes. */
  def styleClasses(classes: String*): Filter = new StyleClassFilter(FXCollections.observableArrayList(classes: _*))

  /** Java-friendly array overload. */
  def styleClasses(classes: Array[String]): Filter = new StyleClassFilter(FXCollections.observableArrayList(classes: _*))

  /**
   * Scala-only by-name overload using simplefx reactive binding. Mirror of
   * [[stylesheets(expression:=>List[String])*]] but for style classes.
   */
  def styleClasses(expression: => List[String]): Filter = {
    val list = FXCollections.observableArrayList[String]()
    @Bind var listB = list.toBindable
    listB <-- expression
    new StyleClassFilter(list)
  }

  def FullscreenFilter(fullscreenValue: Boolean): Filter = { route => { request =>
      val r = route.apply(request)

      Response(r.future.map {
        case x: View =>
          new View {
            override def title: String = x.title
            override def description: String = x.description
            override def content: all.Node = x.realContent

            override def fullscreen: Boolean = fullscreenValue
          }
        case x => x
      })
    }
  }
  def title(title: String): Filter = { route => { request =>
      val r = route.apply(request)
      val _title = title

      Response(r.future.map {
        case x: View =>
          new View {
            override def title: String = _title
            override def description: String = x.description
            override def content: all.Node = x.realContent

            override def fullscreen: Boolean = x.fullscreen
          }
        case x => x
      })
    }
  }
  def description(description: String): Filter = { route => { request =>
      val r = route.apply(request)
      val _description = description

      Response(r.future.map {
        case x: View =>
          new View {
            override def title: String = x.title
            override def description: String = _description
            override def content: all.Node = x.realContent

            override def fullscreen: Boolean = x.fullscreen
          }
        case x => x
      })
    }
  }
  def titleAndDescription(title: String, description: String): Filter = { route => { request =>
      val r = route.apply(request)
      val _title = title
      val _description = description

      Response(r.future.map {
        case x: View =>
          new View {
            override def title: String = _title
            override def description: String = _description
            override def content: all.Node = x.realContent

            override def fullscreen: Boolean = x.fullscreen
          }
        case x => x
      })
    }
  }

  def errorPage(): Filter = errorPage((request, ex) => Response.node(new Label("Error: " + ex.getMessage)))
  def errorPage(biFunction: BiFunction[Request, Throwable, Response]): Filter = {
    route => { request =>
      try {
        val r = route.apply(request)
        Response.fromFuture(r.future.map(x => Response.fromResult(x)).exceptionally { ex =>
          biFunction.apply(request, ex)
        })
      } catch {
        case ex: Throwable =>
          biFunction.apply(request, ex)
      }
    }
  }

  def notFoundPage(): Filter = {
    notFoundPage(_ => Response.node(new NotFoundPage()))
  }
  def notFoundPage(function: Route): Filter = { route =>
    route.and(function)
  }

}
