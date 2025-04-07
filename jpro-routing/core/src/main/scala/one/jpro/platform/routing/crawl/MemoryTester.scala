package one.jpro.platform.routing.crawl

import one.jpro.jmemorybuddy.JMemoryBuddy
import javafx.stage.Stage
import one.jpro.platform.routing.crawl.AppCrawler.{CrawlReportApp, routeToRouteNode}
import one.jpro.platform.routing.sessionmanager.SessionManagerDesktop
import one.jpro.platform.routing.{Request, Route, RouteApp, RouteNode, View}
import org.slf4j.{Logger, LoggerFactory}
import simplefx.core._
import simplefx.all._
import java.util.function.Supplier

object MemoryTester {

  // 2 modes. Keep Stage. Keep RouteNode
  private lazy val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  def testForLeaks(report: CrawlReportApp, appFactory: Supplier[Route]): Unit = {
    testForLeaks_newStage(report.pages, appFactory)
    testForLeaks2_keepStage(report.pages, appFactory)
  }

  def testForLeaks_newStage(pages: List[String], appFactory: Supplier[Route]): Unit = {
    pages.foreach { pageURL =>
      logger.debug(s"Checking for leak for the url: $pageURL")
      JMemoryBuddy.memoryTest(checker1 => {
        JMemoryBuddy.memoryTest(checker2 => {
          val factory: RouteNode = inFX(routeToRouteNode(appFactory.get()))
          assert(factory != null, "The appFactory must not return null ")
          val view = inFX(factory.getRoute()(Request.fromString(pageURL))).future.await

          checker2.setAsReferenced(factory)
          checker2.assertCollectable(view) // Hm?
          if(view.isInstanceOf[View]) {
            checker2.assertCollectable(inFX(view.asInstanceOf[View].realContent))
          }
          inFX(factory.scene.window.asInstanceOf[Stage].close())
          checker1.assertCollectable(factory)
        })
      })
    }
  }

  def testForLeaks2_keepStage(pages: List[String], appFactory: Supplier[Route]): Unit = {
    JMemoryBuddy.memoryTest(checker1 => {
      val routeNode: RouteNode = inFX(routeToRouteNode(appFactory.get()))
      assert(routeNode != null, "The routeNode must not return null ")
      JMemoryBuddy.memoryTest(checker2 => {
        pages.foreach { pageURL =>
          logger.debug(s"Checking for leak for the url: $pageURL")
          val result = inFX(runScheduler(routeNode.getSessionManager().gotoURL(pageURL))).future.await

          println("Got result: " + result)
          if(result.isInstanceOf[View]) {
            val view = result.asInstanceOf[View]
            // Scene Graph:
            println("Got result: " + view.realContent)
            checker2.assertCollectable(inFX(view.realContent))
            assert(view == routeNode.getSessionManager().getView(), "The view must be the same as in the session manager")
            inFX(routeNode.scene.root.applyCss())
          }
          Thread.sleep(100)
        }
        routeNode.getSessionManager().gotoURL("/").future.await
      })
      inFX(routeNode.scene.window.asInstanceOf[Stage].close())
      checker1.assertCollectable(routeNode)
    })
  }

}
