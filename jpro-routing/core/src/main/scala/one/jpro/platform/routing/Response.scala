package one.jpro.platform.routing

trait Response
object Response {
  def empty(): Response = null
  def redirect(to: String): Response = Redirect(to)
  def fromNode(node: javafx.scene.Node): Response = new Response {
    RouteUtils.viewFromNode(node)
  }
}
