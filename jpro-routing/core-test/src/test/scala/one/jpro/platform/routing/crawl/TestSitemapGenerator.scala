package one.jpro.platform.routing.crawl

import one.jpro.platform.routing.Route._
import one.jpro.platform.routing.crawl.TestUtils._
import one.jpro.platform.routing.{Redirect, Response, Route, RouteNode}
import org.junit.jupiter.api.Test
import simplefx.experimental._

class TestSitemapGenerator {
  @Test
  def test(): Unit = {
    def app = new RouteNode(null) {
      setRoute(Route.empty()
        .and(get("/", r => Response.view(new Page1)))
        .and(get("/page2", r => Response.view(new Page2)))
        .and(get("/page4", r => Response.view(new Page2)))
        .and(r => Response.view(new Page1)))
    }
    val result = AppCrawler.crawlApp("http://localhost", () => app)
    val sm = SitemapGenerator.createSitemap("http://localhost", result)
    println("SiteMap: " + sm)
    assert(sm.contains("<loc>http://localhost/page4</loc>"))
    assert(!sm.contains("<loc>http://external/link</loc>"))
    assert(!sm.contains("mailto"))
  }

  @Test
  def testMailToRedirect(): Unit = {
    def app = new RouteNode(null) {
      setRoute(Route.empty()
        .and(get("/", r => Response.view(pageWithLink(List("/page2", "/page3", "mailto:something")))))
        .and(get("/page2", r => Response.redirect("mailto:something-2"))))
    }
    val result = AppCrawler.crawlApp("http://localhost", () => app)
    println("got result: " + result)
    val sm = SitemapGenerator.createSitemap("http://localhost", result)
    println("SiteMap2: " + sm)
    assert(!sm.contains("mailto"), "sitemap contained mailto!")
  }
}
