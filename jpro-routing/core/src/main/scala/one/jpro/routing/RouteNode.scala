package one.jpro.routing

import com.jpro.webapi.WebAPI
import one.jpro.routing.sessionmanager.SessionManager
import simplefx.core._
import simplefx.all._
import simplefx.experimental._

import java.lang.ref.WeakReference

class RouteNode(stage: Stage, route: Route) extends StackPane { THIS =>

  def this(stage: Stage) = {
    this(stage, Route.empty)
  }

  styleClass ::= "jpro-web-app"

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

  lazy val webAPI = if(WebAPI.isBrowser) com.jpro.webapi.WebAPI.getWebAPI(stage) else null

  var newRoute: Route = route
  def getRoute(): Route = newRoute
  def setRoute(x: Route): Unit = newRoute = x

  def route(s: String, oldView: Node) = {
    val oldViewW = new WeakReference(oldView)
    newRoute(Request.fromString(s).copy(oldContent = oldViewW, origOldContent = oldViewW))
  }
  def route = {
    (s: String) => newRoute(Request.fromString(s))
  }


  def start(sessionManager: SessionManager) = {
    SessionManagerContext.setContext(this, sessionManager)
    sessionManager.start()
  }



}
