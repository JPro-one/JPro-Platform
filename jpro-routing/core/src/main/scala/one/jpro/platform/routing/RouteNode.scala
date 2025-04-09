package one.jpro.platform.routing

import com.jpro.webapi.WebAPI
import one.jpro.platform.routing.sessionmanager.SessionManager
import simplefx.core._
import simplefx.all._
import simplefx.experimental._

import java.lang.ref.WeakReference

class RouteNode(stage: Stage, route: Route) extends StackPane { THIS =>

  def this(stage: Stage) = {
    this(stage, Route.empty())
  }

  styleClass ::= "jpro-web-app"
  private var sessionManager: SessionManager = null

  def getSessionManager(): SessionManager = sessionManager

  @Bind private var layoutCounter = 0
  def getLayoutCounter(): Int = layoutCounter

  override def layoutChildren(): Unit = {
    layoutCounter += 1
    if ((this.scene ne null) && WebAPI.isBrowser) {
      webAPI.layoutRoot(this.scene)
      super.layoutChildren()
    } else {
      super.layoutChildren()
    }
  }

  lazy val webAPI: WebAPI = if(WebAPI.isBrowser) com.jpro.webapi.WebAPI.getWebAPI(stage) else null

  var newRoute: Route = route
  def getRoute(): Route = newRoute
  def setRoute(x: Route): Unit = newRoute = x


  def start(sessionManager: SessionManager): Response = {
    this.sessionManager = sessionManager
    SessionManagerContext.setContext(this, sessionManager)
    sessionManager.start()
  }



}
