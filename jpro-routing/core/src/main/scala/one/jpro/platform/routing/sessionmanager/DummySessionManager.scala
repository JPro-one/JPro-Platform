package one.jpro.platform.routing.sessionmanager

import one.jpro.platform.routing.{Response, ResponseResult, RouteNode}

class DummySessionManager extends SessionManager {
  override def webApp: RouteNode = null

  override def goBack(): Unit = ()

  override def goForward(): Unit = ()

  override def gotoURL(_url: String, x: ResponseResult, pushState: Boolean, track: Boolean): Unit = ()

  override def getView(url: String): Response = Response.empty()

  override def start(): Unit = ()
}
