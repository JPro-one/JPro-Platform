package one.jpro.platform.routing.sessionmanager

import one.jpro.platform.routing.{Response, ResponseResult, RouteNode}

class SessionManagerDummy(val webApp: RouteNode) extends SessionManager {

  override def goBack(): Unit = ()

  override def goForward(): Unit = ()

  override def gotoURL(_url: String, x: ResponseResult, pushState: Boolean): Response = {
    Response.fromResult(x)
  }

  override def start(): Response = Response.empty()
}
