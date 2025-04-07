package one.jpro.platform.routing.crawl

import javafx.scene.control.Label
import one.jpro.platform.routing.crawl.TestUtils.{LeakStage, Page1, Page2}
import one.jpro.platform.routing.{Request, Response, Route, RouteNode}
import org.junit.jupiter.api.Test
import simplefx.cores.default.inFX
import simplefx.util.Predef.intercept

class TestMemoryTester {

  @Test
  def simpleTest(): Unit = {
    def route = Route.empty()
        .and(Route.get("/", r => Response.view(new Page1)))
        .and(Route.get("/page2", r => Response.view(new Page2)))
        .and(Route.get("/page4", r => Response.view(new Page2)))
    val result = AppCrawler.crawlRoute("http://localhost", () => route)
    MemoryTester.testForLeaks(result, () => route)
  }

  @Test
  def simpleFailingTest(): Unit = {
    val page2 = new Page2
    def route = Route.empty()
        .and(Route.get("/", r => Response.view(new Page1)))
        .and(Route.get("/page2", r => Response.view(page2)))
        .and(Route.get("/page4", r => Response.view(new Page2)))
    val result = AppCrawler.crawlRoute("http://localhost", () => route)
    intercept[Throwable](MemoryTester.testForLeaks(result, () => route))
  }

  @Test
  def simpleFailingTest2(): Unit = {
    var node2 = new Label()

    def route = Route.empty()
        .and(Route.get("/", r => Response.view(new Page1)))
        .and(Route.get("/page2", r => Response.node(node2)))
        .and(Route.get("/page4", r => Response.view(new Page2)))

    val result = AppCrawler.crawlRoute("http://localhost", () => route)
    intercept[Throwable](MemoryTester.testForLeaks(result, () => route))
  }


  @Test
  def simpleFailingTest3(): Unit = {

    val route: Route = Route.empty()
      .and(Route.get("/", r => Response.view(new Page1)))
      .and(Route.get("/page2", r => Response.node(new LeakStage)))
      .and(Route.get("/page4", r => Response.view(new Page2)))
    val result = AppCrawler.crawlRoute("http://localhost", () => route)
    intercept[Throwable](MemoryTester.testForLeaks(result, () => route)) // fails because the webapp is not collectable
  }

}
