package one.jpro.platform.routing.sessionmanager

import javafx.scene.control.Label
import one.jpro.platform.routing.{Response, Route, RouteApp}
import org.junit.jupiter.api.Test
import simplefx.core._

class TestSessionManager {
  @Test
  def someTests(): Unit = {
      assert(SessionManager.mergeURLs("http://a/b", "/c") == "http://a/c")
      assert(SessionManager.mergeURLs("http://a.com/b", "/") == "http://a.com/")
      assert(SessionManager.mergeURLs("http://a.com/b/c", "./d") == "http://a.com/b/d")
      assert(SessionManager.mergeURLs("http://a.com/b/c", "../d") == "http://a.com/d")
      assert(SessionManager.mergeURLs("http://a/b", "/") == "http://a/")
  }

  @Test
  def testRedirects(): Unit = {
    val route = Route.empty()
      .and(Route.get("/",r => Response.node(new Label("Empty"))))
      .path("/test", Route.empty()
        .and(Route.redirect("/","./test2"))
        .and(Route.get("/test2", r => Response.node(new Label("Yes"))))
      )

    val app = new RouteApp {
      override def createRoute(): Route = route
    }
    val stage = inFX(new javafx.stage.Stage())
    inFX(app.start(stage))

    val res = inFX(app.getSessionManager().gotoURL("/test/").future).await
    inFX {
      val url = app.getSessionManager().getURL()
      assert(url == "/test/test2")
    }
  }
}
