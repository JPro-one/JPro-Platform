package one.jpro.platform.routing.crawl

import javafx.application.Platform
import javafx.scene.control.Label
import javafx.scene.layout.StackPane
import one.jpro.platform.routing.crawl.TestUtils.{LeakStage, Page1, Page2}
import one.jpro.platform.routing.{Request, Response, Route, RouteNode}
import org.junit.jupiter.api.{BeforeAll, Test}
import simplefx.cores.default.inFX
import simplefx.util.Predef.intercept


object TestSizeTester {
  @BeforeAll
  def init(): Unit = inFX {
    Platform.setImplicitExit(false)
  }
}

class TestSizeTester {

  @Test
  def positiveTest1(): Unit = {
    def route = Route.empty()
      .and(Route.get("/", r => Response.node(new StackPane)))
    val result = AppCrawler.crawlRoute("http://localhost", () => route)
    SizeTester.testSize(10,2000,result, () => route)
  }

  @Test
  def positiveTest2(): Unit = {
    def route = Route.empty()
      .and(Route.get("/", r => Response.node(new StackPane {{
        setMinWidth(10)
        setMaxWidth(2000)
      }})))
    val result = AppCrawler.crawlRoute("http://localhost", () => route)
    SizeTester.testSize(10,2000,result, () => route)
  }

  @Test
  def negative1(): Unit = {
    def route = Route.empty()
      .and(Route.get("/", r => Response.node(new StackPane {{
        setMinWidth(11)
      }})))
    val result = AppCrawler.crawlRoute("http://localhost", () => route)
    intercept[Throwable](SizeTester.testSize(10,2000,result, () => route))
  }

  @Test
  def negative2(): Unit = {
    def route = Route.empty()
      .and(Route.get("/", r => Response.node(new StackPane {{
        setMaxWidth(1999)
      }})))
    val result = AppCrawler.crawlRoute("http://localhost", () => route)
    intercept[Throwable](SizeTester.testSize(10,2000,result, () => route))
  }


}
