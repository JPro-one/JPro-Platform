package one.jpro.platform.routing.crawl

import one.jpro.platform.routing.{LinkUtil, View}
import simplefx.all._
import simplefx.core._

object TestUtils {

  def pageWithLink(links: List[String]): View = new View {
    def title = "title"
    def description = "desc"
    override def content: Node = new HBox {
      links.map { link =>
        this <++ new Label("Asdf") {
          LinkUtil.setLink(this,link, "desc1")
        }
      }
    }
  }

  class Page1 extends View {
    def title = "title"
    def description = "desc"

    override def content: Node = new HBox {
      this <++ new Label("Asdf") {
        LinkUtil.setLink(this,"/page2", "desc1")
      }
      this <++ new Label("222") {
        LinkUtil.setLink(this,"/page2")
      }
      this <++ new Label("mail") {
        LinkUtil.setLink(this,"mailto:a@b.com")
      }
      this <++ new ImageView() {
        image = new Image("/testfiles/test.jpg")
        this.setAccessibleRoleDescription("The Description")
      }
    }
  }

  class Page2 extends View {
    def title = "title"
    def description = "desc"

    override def content: Node = new HBox {
      this <++ new Label("Asdf") {
        LinkUtil.setLink(this,"/page2", "desc1")
      }
      this <++ new Label("222") {
        LinkUtil.setLink(this,"/page3", "desc2")
      }
      this <++ new Label("222") {
        LinkUtil.setLink(this,"http://localhost/page4", "desc2")
      }
      this <++ new Label("222") {
        LinkUtil.setLink(this,"http://external/link", "desc2")
      }
    }
  }

  class LeakStage() extends StackPane {
    onceWhen(scene != null) --> {
      println("Adding leak to scene.")
      scene.getProperties().put(this,this)
    }
  }
}
