package one.jpro.routing.sessionmanager

import one.jpro.routing.{Response, RouteNode}

class DummySessionManager extends SessionManager {
  override def webApp: RouteNode = null

  override def goBack(): Unit = ()

  override def goForward(): Unit = ()

  override def gotoURL(_url: String, x: Response, pushState: Boolean, track: Boolean): Unit = ()

  override def getView(url: String): _root_.simplefx.experimental.FXFuture[Response] = null

  override def start(): Unit = ()
}
