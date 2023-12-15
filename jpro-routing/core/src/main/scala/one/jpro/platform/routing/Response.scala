package one.jpro.platform.routing

import simplefx.experimental.FXFuture

trait Response
object Response {
  def empty(): Response = null
  def emptyFuture(): FXFuture[Response] = FXFuture.unit(empty())
  def redirect(to: String): Response = Redirect(to)
  def redirectFuture(to: String): FXFuture[Response] = FXFuture.unit(Redirect(to))
  def fromNode(node: javafx.scene.Node): Response = new Response {
    RouteUtils.viewFromNode(node)
  }
  def fromNodeFuture(node: javafx.scene.Node): FXFuture[Response] = FXFuture.unit(fromNode(node))
}
