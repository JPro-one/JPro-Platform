package one.jpro.routing.extensions.linkheader

import collection.JavaConverters._
import simplefx.core._
import simplefx.all._
import simplefx.experimental._
import one.jpro.routing.filter.container.ContainerFilter
import one.jpro.routing.{Filter, LinkUtil, RouteUtils}

object LinkHeaderFilter {
  case class Link(name: String, prefix: String)

  def genLink(name: String, prefix: String) = new Link(name, prefix)
  def create(x: Link*): Filter = create(x.toList)
  def create(x: java.util.List[Link]): Filter = create(x.asScala.toList)
  def create(x: List[Link]): Filter = {
    ContainerFilter.create(new LinkHeaderFilter.LinkHeaderContainer(x))
  }
  private class LinkHeaderContainer(x: List[Link]) extends RouteUtils.SFXContainerFactory {
    override def isContainer(x: Node): Boolean = x.isInstanceOf[MyContainer]
    override def createContainer() = new MyContainer
    class MyContainer extends VBox with Container {
  
      this <++  new HBox {
        styleClass ::= "linkheader-link-hbox"
        x.map { link =>        
          this <++ new Label {
            styleClass ::= "linkheader-link"
            text <-- link.name
            when (request != null && request.path.startsWith(link.prefix)) ==> {
              styleClass <++ "selected"
            }
            LinkUtil.setLink(this, link.prefix, link.name)
          }
        }
      }
      this <++ new StackPane {
        javafx.scene.layout.VBox.setVgrow(this, Priority.ALWAYS)
        children <-- (if(content != null) List(content) else Nil)
      }
    }
  }
}
