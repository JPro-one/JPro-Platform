package one.jpro.platform.routing

import javafx.scene.Node
import simplefx.experimental._
import java.util.function.Function
import java.util.function.Predicate


object Route {
  def empty(): Route = (r) => Response.empty()

  def redirect(path: String, to: String): Route = get(path, (r) => Response.redirect(to))

  def get(path: String, f: Function[Request, Response]): Route = (request: Request) => if (request.getPath() == path) f.apply(request) else Response.empty()

}
@FunctionalInterface
trait Route {
  def apply(r: Request): Response

  def and(x: Route): Route = { request =>
    val r = apply(request)
    assert(r != null, "Route returned null: " + this + " for " + request)
    Response(r.future.flatMap{ r =>
      if(r == null) {
        val r2 = x.apply(request)
        assert(r2 != null, "Route returned null: " + x + " for " + request)
        r2.future
      } else FXFuture.unit(r)
    })
  }
  def domain(domain: String, route: Route): Route = and((r: Request) => {
    if(r.getDomain() == domain) {
      route.apply(r)
    } else {
      Response.empty()
    }
  })
  def path(path: String, route: Route): Route = and((r: Request) => {
    if(r.getPath().startsWith(path + "/")) {
      val r2 = r.copy(path = r.getPath().drop(path.length), directory = r.resolve(path))
      route.apply(r2)
    } else {
      Response.empty()
    }
  })
  def filter(filter: Filter): Route = filter(this)
  def filterWhen(cond: Predicate[Request], filter: (Request) => Filter): Route = { r =>
    if(cond.test(r)) {
      filter.apply(r)(this).apply(r)
    } else {
      this.apply(r)
    }
  }
  def filterWhenFuture(cond: Predicate[Request], filter: (Request) => FXFuture[Filter]): Route = { r =>
    if(cond.test(r)) {
      Response(filter(r).flatMap(filter => filter(this).apply(r).future))
    } else {
      this.apply(r)
    }
  }
  def when(cond: Predicate[Request], _then: Route): Route = and(r => {
    val condResult = cond.test(r)
    if(condResult) _then(r) else Response.empty()
  })
  def when(cond: Predicate[Request], _then: Route, _else: Route): Route = and(r => {
    if(cond.test(r)) _then(r) else _else(r)
  })

  def whenFuture(cond: java.util.function.Function[Request, FXFuture[java.lang.Boolean]], _then: Route): Route = and(r => {
    Response.fromFuture(
      cond.apply(r).map(condResult => if (condResult) _then(r) else Response.empty())
    )
  })

  def whenFuture(cond: java.util.function.Function[Request, FXFuture[java.lang.Boolean]], _then: Route, _else: Route): Route = and(r => {
    Response.fromFuture(
      cond.apply(r).map(condResult => if (condResult) _then(r) else _else(r))
    )
  })
}
