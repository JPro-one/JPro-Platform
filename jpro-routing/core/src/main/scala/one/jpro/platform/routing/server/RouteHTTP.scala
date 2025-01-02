package one.jpro.platform.routing.server

import com.jpro.webapi.server.{Response, ServerAPI}
import one.jpro.platform.routing.{Route, RouteApp}
import one.jpro.platform.routing.crawl.{AppCrawler, SitemapGenerator}

object RouteHTTP {
  var initialized = false

  def main(args: Array[String]): Unit = {
  //  System.out.println("Hello, world!");
  //  val route = getRoute();
  //  CrawlReportApp report = AppCrawler.generate(route);
  //  SiteMapGenerator siteMapGenerator = new SiteMapGenerator();
  }
}

abstract class RouteHTTP {

  def start(): Unit = {

    new Thread(() => {
      val prefix = "localhost"

      if(RouteHTTP.initialized) {
        throw new IllegalStateException("RouteHTTP already initialized")
      }

      //val appCrawler = new AppCrawler(prefix, () => AppCrawler.routeToRouteNode(getRoute()))

      val report = AppCrawler.crawlRoute(prefix, () => getRoute())

      ServerAPI.getServerAPI().addRequestHandler(
        r => {
          println("RouteHTTP> request: " + r.getPath())
          r.getPath() match {
            case "/sitemap.xml" =>
              Response.of(SitemapGenerator.createSitemap(prefix, report).getBytes())
            case _ =>
              Response.empty()
          }
        }
      )
    }).start()


  }
  def getRoute(): Route
}
