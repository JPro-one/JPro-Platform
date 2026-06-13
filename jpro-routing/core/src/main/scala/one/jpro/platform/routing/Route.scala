package one.jpro.platform.routing

import javafx.scene.Node
import simplefx.experimental._
import java.util.function.Function
import java.util.function.Predicate


object Route {
  /** Returns a route that matches nothing — every request gets an empty response. */
  def empty(): Route = (r) => Response.empty()

  /**
   * Returns a route that redirects requests for the given path to another location.
   *
   * @param path the exact request path to match
   * @param to   the target location of the redirect
   */
  def redirect(path: String, to: String): Route = get(path, (r) => Response.redirect(to))

  /**
   * Returns a route that calls the handler when the request path matches exactly,
   * and responds empty otherwise.
   *
   * @param path the exact request path to match
   * @param f    the handler producing the response for a matching request
   */
  def get(path: String, f: Function[Request, Response]): Route = (request: Request) => if (request.getPath() == path) f.apply(request) else Response.empty()

}
/**
 * A route maps a Request to a Response. Routes are composable: combine alternatives
 * with {@code and}, scope them with {@code path} or {@code domain}, branch with
 * {@code when}, and wrap them with {@code filter}.
 */
@FunctionalInterface
trait Route {
  def apply(r: Request): Response

  /**
   * Combines this route with a fallback: if this route responds empty for a request,
   * the other route is tried.
   */
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
  /** Adds a route that is only tried when the request matches the given domain. */
  def domain(domain: String, route: Route): Route = and((r: Request) => {
    if(r.getDomain() == domain) {
      route.apply(r)
    } else {
      Response.empty()
    }
  })
  /**
   * Adds a route that is only tried when the request path starts with the given prefix.
   * The nested route sees the request with the prefix stripped from its path, so
   * routes can be written independently of where they are mounted.
   *
   * @param path  the path prefix to match (without a trailing slash)
   * @param route the route handling requests below the prefix
   */
  def path(path: String, route: Route): Route = and((r: Request) => {
    if(r.getPath().startsWith(path + "/")) {
      val r2 = r.copy(path = r.getPath().drop(path.length), directory = r.resolve(path))
      route.apply(r2)
    } else {
      Response.empty()
    }
  })
  /** Returns this route wrapped by the given filter. */
  def filter(filter: Filter): Route = filter(this)
  /**
   * Wraps this route with a filter created per request, but only when the condition holds;
   * otherwise this route is used unchanged.
   *
   * @param cond   decides per request whether the filter applies
   * @param filter creates the filter for a matching request
   */
  def filterWhen(cond: Predicate[Request], filter: (Request) => Filter): Route = { r =>
    if(cond.test(r)) {
      // Wrap the user-supplied lambda so any StatefulFilter constructed
      // inside it is detected and reported as a fail-fast violation.
      val resolved = StatefulFilterContext.runInRequestLambda(filter.apply(r))
      resolved(this).apply(r)
    } else {
      this.apply(r)
    }
  }
  /**
   * Like {@code filterWhen}, but the filter is created asynchronously.
   *
   * @param cond   decides per request whether the filter applies
   * @param filter creates the future filter for a matching request
   */
  def filterWhenFuture(cond: Predicate[Request], filter: (Request) => FXFuture[Filter]): Route = { r =>
    if(cond.test(r)) {
      val future = StatefulFilterContext.runInRequestLambda(filter(r))
      Response(future.flatMap(filter => filter(this).apply(r).future))
    } else {
      this.apply(r)
    }
  }
  /** Adds a route that is only tried when the condition holds for the request. */
  def when(cond: Predicate[Request], _then: Route): Route = and(r => {
    val condResult = cond.test(r)
    if(condResult) _then(r) else Response.empty()
  })
  /** Adds a branch: requests go to {@code _then} when the condition holds, to {@code _else} otherwise. */
  def when(cond: Predicate[Request], _then: Route, _else: Route): Route = and(r => {
    if(cond.test(r)) _then(r) else _else(r)
  })

  /** Like {@code when}, but the condition is evaluated asynchronously. */
  def whenFuture(cond: java.util.function.Function[Request, FXFuture[java.lang.Boolean]], _then: Route): Route = and(r => {
    Response.fromFuture(
      cond.apply(r).map(condResult => if (condResult) _then(r) else Response.empty())
    )
  })

  /** Like {@code when} with an else-branch, but the condition is evaluated asynchronously. */
  def whenFuture(cond: java.util.function.Function[Request, FXFuture[java.lang.Boolean]], _then: Route, _else: Route): Route = and(r => {
    Response.fromFuture(
      cond.apply(r).map(condResult => if (condResult) _then(r) else _else(r))
    )
  })
}
