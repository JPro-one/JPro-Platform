package example.scala

import one.jpro.platform.routing.Route
import one.jpro.platform.routing.server.RouteHTTP


object TestWebApplicationHTTP {
  def main(args: Array[String]): Unit = {
    new TestWebApplicationHTTP().start()
  }
}
class TestWebApplicationHTTP extends RouteHTTP {

  override def getRoute: Route = {
    new TestWebApplication().createRoute()
  }

}
