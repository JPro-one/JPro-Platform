package one.jpro.platform.routing

import simplefx.experimental.FXFuture

/**
 * The result of applying a Route to a Request. A response wraps a future
 * ResponseResult — a view, a redirect, or null for "no match" (empty).
 */
case class Response(future: FXFuture[ResponseResult]) {
  assert(future != null, "future must not be null - but it's value can be null")
}
object Response {
  /** Returns the empty response, signaling that the route did not match. */
  def empty(): Response = Response(FXFuture.unit(null))
  /**
   * Returns a response that redirects to another location.
   *
   * @param to the target location, e.g. "/login"
   */
  def redirect(to: String): Response = Response(FXFuture.unit(Redirect(to)))
  /** Returns a response that completes with the given error. */
  def error(ex: Exception): Response = Response(FXFuture.error(ex))

  /** Returns a response showing the given view. */
  def view(view: View): Response = Response(FXFuture.unit(view))
  /** Returns a response showing the given node, wrapped in a View. */
  def node(node: javafx.scene.Node): Response = Response(FXFuture.unit(View.fromNode(node)))
  /** Returns a response that completes once the given future response completes. */
  def fromFuture(future: FXFuture[Response]): Response = Response(future.flatMap(_.future))
  /** Returns a response from an already computed result (a View or Redirect). */
  def fromResult(result: ResponseResult): Response = Response(FXFuture.unit(result))
  /** Returns a response from a future result (a View or Redirect). */
  def fromFutureResult(future: FXFuture[ResponseResult]): Response = Response(future)
}
