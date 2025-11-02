package one.jpro.platform.routing.sessionmanager

import javafx.scene.control.Label
import javafx.scene.layout.StackPane
import one.jpro.platform.routing.{Filters, Response, Route, RouteApp}
import org.junit.jupiter.api.Test
import simplefx.core._
import simplefx.util.Predef.intercept

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

  @Test
  def testPageCreatedOnce(): Unit = {
    var i = 0
    val route = Route.empty()
      .and(Route.get("/", r => Response.node(new StackPane {
        i += 1
      })))

    val app = new RouteApp {
      override def createRoute(): Route = route
    }
    val stage = inFX(new javafx.stage.Stage())
    inFX(app.startFuture(stage)).future.await
    assert(i == 1)
    inFX(app.getSessionManager().gotoURL("/").future).await
    assert(i == 2)
  }

  @Test
  def testErrorPage(): Unit = {
    val route = Route.empty()
      .and(Route.get("/",r => Response.node(new Label("Empty"))))
      .and(Route.get("/error", r => throw new Exception("Error")))
      .and(Route.get("/error2", r => Response.error(new Exception("Error2"))))
      .filter(Filters.errorPage())

    val app = new RouteApp {
      override def createRoute(): Route = route
    }
    val stage = inFX(new javafx.stage.Stage())
    inFX(app.startFuture(stage)).future.await

    val res1 = inFX(app.getSessionManager().gotoURL("/error").future).await
    inFX {
      val view = app.getSessionManager().view
      assert(view.realContent.asInstanceOf[Label].getText.contains("Error"), view.realContent.asInstanceOf[Label].getText)
    }

    val res2 = inFX(app.getSessionManager().gotoURL("/error2").future).await
    inFX {
      val view = app.getSessionManager().view
      assert(view.realContent.asInstanceOf[Label].getText.contains("Error2"), view.realContent.asInstanceOf[Label].getText)
    }
  }

  @Test
  def testNotFoundPage(): Unit = {
    val route = Route.empty()
      .and(Route.get("/",r => Response.node(new Label("Empty"))))
      .filter(Filters.notFoundPage())
      .filter(Filters.notFoundPage(r => Response.node(new Label("Not Found: " + r.getPath()))))


    val app = new RouteApp {
      override def createRoute(): Route = route
    }
    val stage = inFX(new javafx.stage.Stage())
    inFX(app.startFuture(stage)).future.await

    val res = inFX(app.getSessionManager().gotoURL("/notfound").future).await
    inFX {
      val view = app.getSessionManager().view
      assert(view.realContent.asInstanceOf[Label].getText.contains("Not Found"), view.realContent.asInstanceOf[Label].getText)
      println("Label Text: " + view.realContent.asInstanceOf[Label].getText)
    }
  }

  @Test
  def testLoop(): Unit = {
    val route = Route.empty()
      .and(Route.get("/", r => Response.redirect("/")))

    val app = new RouteApp {
      override def createRoute(): Route = route
    }

    val stage = inFX(new javafx.stage.Stage())
    val error: Exception = intercept[Exception](inFX(app.startFuture(stage)).future.await)
    assert(error.getMessage.contains("Too many redirects"))
  }
}
