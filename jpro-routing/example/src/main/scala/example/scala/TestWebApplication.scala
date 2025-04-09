package example.scala

import com.jpro.webapi.{HTMLView, WebAPI}
import one.jpro.jmemorybuddy.JMemoryBuddyLive
import one.jpro.platform.routing.LinkUtil._
import one.jpro.platform.routing.Route._
import one.jpro.platform.routing.sessionmanager.SessionManager
import one.jpro.platform.routing._
import one.jpro.platform.routing.dev.{DevFilter, StatisticsFilter}
import one.jpro.platform.routing.performance.IncrementalLoading
import org.controlsfx.control.PopOver
import simplefx.all._
import simplefx.core._

import scala.collection.JavaConverters.asScalaBufferConverter

class TestWebApplication extends RouteApp {

  override def createRoute(): Route = {
    if(getRouteNode() != null) {
      getRouteNode().stylesheets ::= "test.css"
    }

    Route.empty()
      .and(get("", (r) => Response.view(new MainView)))
      .and(get("/", (r) => Response.view(new MainView)))
      .and(get("/redirect2", r => Response.redirect("https://google.com")))
      .and(get("/main", (r) => Response.view(new MainView)))
      .and(get("/green", (r) => Response.view(new GreenView)))
      .and(get("/sub", (r) => Response.view(new SubView)))
      .and(get("/redirect", r => Response.redirect( "/sub")))
      .and(get("/paralax", (r) => Response.view(new ParalaxPage)))
      .and(get("/pdf", (r) => Response.view(new PDFTest)))
      .and(get("/leak", (r) => Response.view(new LeakingPage)))
      .and(get("/collect", (r) => Response.view(new CollectingPage)))
      .and(get("/incremental", (r) => Response.view(new IncrementalPage)))
      .and(get("/jmemorybuddy", (r) => Response.view(new JMemoryBuddyPage)))
      .and(get("/100", (r) => Response.view(new ManyNodes(100))))
      .and(get("/200", (r) => Response.view(new ManyNodes(200))))
      .and(get("/400", (r) => Response.view(new ManyNodes(400))))
      .and(get("/800", (r) => Response.view(new ManyNodes(800))))
      .and(get("/1600", (r) => Response.view(new ManyNodes(1600))))
      .and(get("/3200", (r) => Response.view(new ManyNodes(3200))))
      .and(get("/6400", (r) => Response.view(new ManyNodes(6400))))
      .and(get("/it's\" tricky", (r) => Response.view(new MainView)))
      .and(get("/it's\" tricky", (r) => Response.view(new MainView)))
      .filter(DevFilter.create)
      .filter(StatisticsFilter.create)
  }


}

class Header(view: View, sessionManager: SessionManager) extends HBox {
  padding = Insets(10)
  spacing = 10
  class HeaderLink(str: String, url: String) extends Label (str) {
    styleClass ::= "header-link"
    if(url.nonEmpty) {
      setLink(this, url, Some(str))
    }
  }
  this <++ new HeaderLink("main"    , "/main")
  this <++ new HeaderLink("subpage" , "/sub" )
  this <++ new HeaderLink("redirect", "/redirect" )
  //this <++ new HeaderLink("tricky!" , "/it's\" tricky" )
  this <++ new HeaderLink("google"  , "http://google.com" )
  this <++ new HeaderLink("paralax" , "/paralax" )
  //this <++ new HeaderLink("dead"    , "/as df" )
  this <++ new HeaderLink("green"   , "/green" )
  this <++ new HeaderLink("pdf"     , "/pdf" )
  this <++ new HeaderLink("100"     , "/100" )
  this <++ new HeaderLink("200"     , "/200" )
  this <++ new HeaderLink("400"     , "/400" )
  this <++ new HeaderLink("800"     , "/800" )
  this <++ new HeaderLink("1600"    , "/1600" )
  this <++ new HeaderLink("3200"    , "/3200" )
  this <++ new HeaderLink("6400"    , "/6400" )
  this <++ new HeaderLink("leak"    , "/leak" )
  this <++ new HeaderLink("incremental"    , "/incremental" )
  this <++ new HeaderLink("collect"    , "/collect" )
  this <++ new HeaderLink("jmemorybuddy"    , "/jmemorybuddy" )
  this <++ new HeaderLink("No Link" , "" ) {
    setLink(this, "/?1", Some("/?1"))
  }
  this <++ new HeaderLink("ManyLinks" , "" ) {
    setLink(this, "/?1", Some("/?1"))
    setLink(this, "/?2", Some("/?2"))
    setLink(this, "/?3", Some("/?3"))
    setLink(this, "/?4", Some("/?4"))
    setLink(this, "/?5", Some("/?5"))
    setLink(this, "/?6", Some("/?6"))
    setLink(this, "/?7", Some("/?7"))
  }
}
class Header2(view: View, sessionManager: SessionManager) extends HBox {
  this <++ new Label(view.url)

  this <++ new Button("Backward") {
    disable <-- (!WebAPI.isBrowser && sessionManager.historyBackward.isEmpty)
    onAction --> {
      goBack(this)
    }
  }
  this <++ new Button("Forward") {
    disable <-- (!WebAPI.isBrowser && sessionManager.historyForward.isEmpty)
    onAction --> {
      goForward(this)
    }
  }
  this <++ new Button("Backward2") {
    onAction --> { e =>
      println("view.getSessionManager(): " + view.getSessionManager())
      view.getSessionManager().goBack()
    }
  }
}

class Footer(sessionManager: SessionManager) extends HBox {
  spacing = 10
  this <++ new Label("asd")
  this <++ new Label("url: " + sessionManager.url)
  this <++ new Button("refresh") {
    onAction --> {
      LinkUtil.refresh(this)
    }
  }
}

trait Page extends View { view =>
  override lazy val realContent: VBox = {
    new VBox {
  // Cousing leak? style = "-fx-background-color: white;"
    //  transform = Scale(1.3,1.3)
      spacing = 10
      this <++ new Header(view, getSessionManager)
      this <++ new Header2(view, getSessionManager)
      val theContent = content
      javafx.scene.layout.VBox.setVgrow(theContent,Priority.ALWAYS)
      this <++ theContent
      this <++ new Footer(getSessionManager)
    }
  }

  override def handleRequest(x: Request): Boolean = {
    println("handleRequest called: " + x)
    return false;
  }
}

class UnknownPage(x: String) extends Page {
  def title = "Unknown page: " + x
  def description = "Unknown page: " + x

  override def fullscreen = false

  def content = new Label("UNKNOWN PAGE: " + x) { font = new Font(60)}
}

class GreenView() extends Page {
  def title = "Green Page"
  def description = "desc Main"

  override def fullscreen = false

  def content = new StackPane { style = "-fx-background-color: green;"}
}

class MainView extends Page {
  def title = "Main"
  def description = "desc Main"

  lazy val content = new VBox {
    spacing = 100
    def addGoogle: Node = {
      new StackPane(new Label("GOOGLE EXTERNAL") {
        font = new Font(60);
      }) {
        LinkUtil.setExternalLink(this, "http://google.com")
        /*this <++ new HTMLView {
          setContent(
            """
              |<a style="display: block; width: 100%; height: 100%; background-color: #66666666;" href="http://google.com"></a>
            """.stripMargin
          )
        }*/
      }
    }
    this <++ addGoogle
    this <++ new Label(" label 123") { font = new Font(60)}
    this <++ new Button("Open Popup") { button =>
      onAction --> {
        val content = new VBox { box =>
          this <++ new Label("Im A Link to Google!") {
            setLink(this,"http://google.com")
          }
          this <++ addGoogle
        }
        new PopOver(content) {
        }.show(button)
      }
    }
    this <++ new VBox {
      style = "-fx-background-color: yellow; -fx-padding: 50px;"
      this <++ new Label("Nested Links")
      this <++ new Label("Yellow")
      LinkUtil.setLink(this, "/yellow")
      this <++ new Label("RED") {
        style = "-fx-background-color: red;"
        font = new Font(60)
        LinkUtil.setLink(this, "/red")
      }
    }
    /*
    this <++ new Button("Copy Text") {
      CopyUtil.setCopyOnClick(this, "Hello \"World\"!")
    }
    this <++ new Button("Copy Text2") {
      CopyUtil.setCopyOnClick(this, "Hello \n\n'`\"World\"!")
    }*/
    this <++ new Label(" label 123") { font = new Font(60)}
    this <++ new Label(" label 123") { font = new Font(60)}
    this <++ addGoogle
    this <++ new Label("paralax" ) { font = new Font(60); setLink(this, "/paralax" ) }

  }
}

class ManyNodes(size: Int) extends Page {
  def title = "ManyNodes " + size

  def description = "desc ManyNodes"

  lazy val content = new VBox {

    this <++ new Label("ManyNodes " + size) { font = new Font(60)}

    this <++ new VBox {
      spacing = 10
      this.children = (1 to size).map { i =>
        new Label("Node " + i) { font = new Font(15)}
      }.toList
    }
  }
}

class SubView extends Page {
  def title = "SubView"
  def description = "desc Sub"
  override def fullscreen=false

  lazy val content = new VBox {
    this <++ new Label("SUBVIEW") { font = new Font(60); padding = Insets(100)}
    this <++ new Label("SUBVIEW") { font = new Font(60); padding = Insets(100)}
    this <++ new Label("SUBVIEW") { font = new Font(60); padding = Insets(100)}
    this <++ new Label("SUBVIEW") { font = new Font(60); padding = Insets(100)}
    this <++ new Label("I'm fullscreen!") { font = new Font(60)}
  }
}

class IncrementalPage extends Page {
  def title = "Incremental"
  def description = "desc Incremental"

  val content = new VBox {
    (1 to 100).map { i =>
      val n = new Label("Node " + i)
      IncrementalLoading.loadNode(n)
      this <++ n
    }
  }
}

class PDFTest extends Page {
  def title = "pdf"
  def description = "pdf desc"

  lazy val content = new VBox {
    this <++ new Label("PAGE 1") { font = new Font(60)}
    this <++ new HTMLView("<div style=\"break-after:page\"></div>")
    this <++ new Label("PAGE 2") { font = new Font(60)}
    this <++ new HTMLView("<div style=\"break-after:page\"></div>")
    this <++ new Label("PAGE 3") { font = new Font(60)}
  }
}
object LeakingPage {
  var instances: List[Page] = Nil
}
class LeakingPage extends Page {
  def title = "leak"
  def description = "leaks"

  LeakingPage.instances ::= this

  val content = new VBox {
    this <++ new Label("Leaks") { font = new Font(60)}
  }
}
class CollectingPage extends Page {
  def title = "collect"
  def description = "collect"

  LeakingPage.instances ::= this

  override def onClose(): Unit = {
    println("onClose called!")
    LeakingPage.instances = LeakingPage.instances.filter(_ != this)
  }

  val content = new VBox {
    this <++ new Label("Leaks") { font = new Font(60)}
  }
}
class JMemoryBuddyPage extends Page {
  def title = "buddy"
  def description = "buddy"

  System.gc()

  val content = new VBox {
    this <++ new Label() {
      text = JMemoryBuddyLive.getReport.toString
      wrapText = true
    }
    this <++ new Label() {
      text = JMemoryBuddyLive.getReport.uncollectedEntries.asScala.map(_.name).mkString("\n")
      wrapText = true
    }
  }
}

class ParalaxPage extends Page {
  def title = "Paralax"
  def description = "desc Para"

  //override def saveScrollPosition: Boolean = false

  val img1 = getClass().getResource("/images/img1.jpg")

  val content = new StackPane {
    this <++ new Region {
      maxWidthProp = 5
      style = "-fx-background-color: green;"
    }
    this <++ new VBox {
      spacing = 200

      this <++ new ParallaxView(img1) {
        minWH = (250,300)
        style = "-fx-border-width:1; -fx-border-color:black;"
      }
      this <++ new ParallaxView(img1) {
        minWH = (250,400)
        style = "-fx-border-width:1; -fx-border-color:black;"
      }
      this <++ new ParallaxView(img1) {
        minWH = (250,400)
        style = "-fx-border-width:1; -fx-border-color:black;"
      }
      this <++ new Label("asdf")
      this <++ new Label("asdf")
      this <++ new Label("asdf")
    }
    this <++ new Region {
      maxWidthProp = 5
      style = "-fx-background-color: green;"
    }
  }

}

