package one.jpro.platform.routing

import javafx.collections.{FXCollections, ObservableList}
import javafx.scene.control.Label
import one.jpro.platform.routing.filter.container.CssFilter
import simplefx.all
import java.util.function.Function
import java.util.function.BiFunction

object Filters {

  /**
   * Wraps the matched view in a `StackPane` whose stylesheets reflect the
   * given list. Mutating the list at runtime updates the wrapper's
   * stylesheets immediately, so this can be driven by an `isMobile`
   * subscription, a theme switcher, etc.
   *
   * Note: this returns a [[StatefulFilter]] (specifically a [[CssFilter]]).
   * Each call creates a new instance with its own identity. As with any
   * stateful filter, do NOT call this from inside a per-request lambda
   * (`filterWhen`, `filterWhenFuture`) — hoist it to a field on your
   * RouteApp or a Site object instead.
   */
  def css(sheets: ObservableList[String]): Filter = new CssFilter(sheets)

  /** Convenience overload for a fixed (non-reactive) list of stylesheets. */
  def css(sheets: String*): Filter = new CssFilter(FXCollections.observableArrayList(sheets: _*))

  /** Java-friendly varargs overload. */
  def css(sheets: Array[String]): Filter = new CssFilter(FXCollections.observableArrayList(sheets: _*))

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
