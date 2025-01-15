package one.jpro.platform.routing

import javafx.scene.control.Label
import simplefx.all
import java.util.function.Function
import java.util.function.BiFunction

object Filters {
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

      Response(r.future.map {
        case x: View =>
          new View {
            override def title: String = title
            override def description: String = x.description
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
    notFoundPage((request) => Response.node(new Label("Not Found: " + request.getPath())))
  }
  def notFoundPage(function: Route): Filter = { route =>
    route.and(function)
  }

}
