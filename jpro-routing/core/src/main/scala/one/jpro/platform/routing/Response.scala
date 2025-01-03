package one.jpro.platform.routing

import simplefx.experimental.FXFuture

case class Response(future: FXFuture[ResponseResult]) {
  assert(future != null, "future must not be null - but it's value can be null")
}
object Response {
  def empty(): Response = Response(FXFuture.unit(null))
  def redirect(to: String): Response = Response(FXFuture.unit(Redirect(to)))
  def error(ex: Exception): Response = Response(FXFuture.error(ex))

  def view(view: View): Response = Response(FXFuture.unit(view))
  def node(node: javafx.scene.Node): Response = Response(FXFuture.unit(View.fromNode(node)))
  def fromFuture(future: FXFuture[Response]): Response = Response(future.flatMap(_.future))
  def fromResult(result: ResponseResult): Response = Response(FXFuture.unit(result))
  def fromFutureResult(future: FXFuture[ResponseResult]): Response = Response(future)
}
