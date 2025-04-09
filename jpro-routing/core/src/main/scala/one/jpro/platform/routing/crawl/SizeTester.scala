package one.jpro.platform.routing.crawl

import javafx.stage.Stage
import one.jpro.platform.routing.{Request, Route, RouteNode, View}
import one.jpro.platform.routing.crawl.AppCrawler.{CrawlReportApp, routeToRouteNode}
import one.jpro.platform.routing.crawl.MemoryTester.getClass
import org.slf4j.{Logger, LoggerFactory}

import java.util.function.Supplier
import simplefx.core._
import simplefx.all._

object SizeTester {

  private lazy val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  def testSize(minWidth: Int, maxWidth: Int, report: CrawlReportApp, appFactory: Supplier[Route]): Unit = {
    testSize(minWidth, maxWidth, report.pages, appFactory)
  }

  def testSize(minWidth: Int, maxWidth: Int, pages: java.util.List[String], appFactory: Supplier[Route]): Unit = {
    pages.forEach(pageURL => {
      logger.debug(s"Checking for leak for the url: $pageURL")
      val routeNode: RouteNode = inFX(routeToRouteNode(appFactory.get()))
      assert(routeNode != null, "The routeNode must not return null ")
      val view = inFX(routeNode.getRoute()(Request.fromString(pageURL))).future.await
      inFX(routeNode.scene.root.applyCss())

      assert(view.isInstanceOf[View])
      val node = view.asInstanceOf[View].realContent
      assert(node.minWidth(-1) <= minWidth, s"Node minWidth: ${node.minWidth(-1)} should be less than or equal to $minWidth")
      assert(node.maxWidth(-1) >= maxWidth, s"Node maxWidth: ${node.maxWidth(-1)} should be greater than or equal to $maxWidth")
      inFX(routeNode.scene.window.asInstanceOf[Stage].close())
    })
  }
}
