package one.jpro.platform.routing

import javafx.collections.{FXCollections, ObservableList}
import javafx.scene.control.Label
import one.jpro.platform.routing.container.{StyleClassTransformer, StylesheetsTransformer}
import simplefx.all
import simplefx.all._
import simplefx.core._
import simplefx.experimental._
import scala.collection.JavaConverters._
import java.util.function.Function
import java.util.function.BiFunction

object Transformers {

  // ----- Stylesheets -----

  /**
   * Wraps the matched page in a `StackPane` whose `getStylesheets()` reflects
   * the given list. Mutating the list at runtime updates the wrapper's
   * stylesheets immediately — drive it from an `isMobile` subscription, a
   * theme switcher, etc.
   *
   * Returns a [[StatefulTransformer]]; do NOT call this from inside a per-request
   * lambda (`transformWhen`, `transformWhenFuture`) — hoist it to a field on your
   * RouteApp or a Site object instead.
   */
  def stylesheets(sheets: ObservableList[String]): Transformer = new StylesheetsTransformer(sheets)

  /** Convenience overload for a fixed (non-reactive) list of stylesheets. */
  def stylesheets(sheets: String*): Transformer = new StylesheetsTransformer(FXCollections.observableArrayList(sheets: _*))

  /** Java-friendly array overload. */
  def stylesheets(sheets: Array[String]): Transformer = new StylesheetsTransformer(FXCollections.observableArrayList(sheets: _*))

  /**
   * Scala-only by-name overload using simplefx reactive binding. The
   * `expression` is wrapped in a `Bindable`; whenever a `@Bind` value it
   * reads changes, the wrapper's stylesheets are updated automatically.
   *
   * {{{
   *   filter(Transformers.stylesheets(
   *     if (isMobile) List(MAIN_CSS, MOBILE_CSS)
   *     else          List(MAIN_CSS, DESKTOP_CSS)))
   * }}}
   */
  def stylesheets(expression: => List[String]): Transformer = {
    val list = FXCollections.observableArrayList[String]()
    @Bind var listB = list.toBindable
    listB <-- expression
    new StylesheetsTransformer(list)
  }

  // ----- Style classes -----

  /**
   * Wraps the matched page in a `StackPane` whose `getStyleClass()` reflects
   * the given list. Same usage and lifecycle constraints as
   * [[stylesheets]] — see its docs.
   *
   * Stack with [[stylesheets]] when you want both: each is its own filter
   * and produces its own wrapper container.
   */
  def styleClasses(classes: ObservableList[String]): Transformer = new StyleClassTransformer(classes)

  /** Convenience overload for a fixed (non-reactive) list of style classes. */
  def styleClasses(classes: String*): Transformer = new StyleClassTransformer(FXCollections.observableArrayList(classes: _*))

  /** Java-friendly array overload. */
  def styleClasses(classes: Array[String]): Transformer = new StyleClassTransformer(FXCollections.observableArrayList(classes: _*))

  /**
   * Scala-only by-name overload using simplefx reactive binding. Mirror of
   * [[stylesheets(expression:=>List[String])*]] but for style classes.
   */
  def styleClasses(expression: => List[String]): Transformer = {
    val list = FXCollections.observableArrayList[String]()
    @Bind var listB = list.toBindable
    listB <-- expression
    new StyleClassTransformer(list)
  }

  // ----- Page transforms -----

  /**
   * Applies {@code f} to the page produced by the route, leaving redirects and
   * empty responses untouched. The building block for page-decorating filters
   * such as {@link #fullscreen}, {@link #title} and {@link #description}.
   */
  def mapPage(f: Function[Page, Page]): Transformer = { route => { request =>
      Response(route.apply(request).future.map {
        case page: Page => f.apply(page)
        case x => x
      })
    }
  }

  /** Sets the fullscreen flag of the route's pages. */
  def fullscreen(fullscreenValue: Boolean): Transformer = mapPage(_.withFullscreen(fullscreenValue))

  /** Sets the title of the route's pages. */
  def title(title: String): Transformer = mapPage(_.withTitle(title))

  /** Sets the description of the route's pages. */
  def description(description: String): Transformer = mapPage(_.withDescription(description))

  /** Sets the title and description of the route's pages. */
  def titleAndDescription(title: String, description: String): Transformer =
    mapPage(_.withTitle(title).withDescription(description))

  def errorPage(): Transformer = errorPage((request, ex) => Response.node(new Label("Error: " + ex.getMessage)))
  def errorPage(biFunction: BiFunction[Request, Throwable, Response]): Transformer = {
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

  def notFoundPage(): Transformer = {
    notFoundPage(_ => Response.node(new NotFoundPage()))
  }
  def notFoundPage(function: Route): Transformer = { route =>
    route.and(function)
  }

}
