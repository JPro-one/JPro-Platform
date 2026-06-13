package one.jpro.platform.routing.extensions.linkheader

import one.jpro.platform.routing.filter.container.{ContainerTransformer, ReactiveContainer}
import one.jpro.platform.routing.{Transformer, LinkUtil}

import collection.JavaConverters._
import simplefx.core._
import simplefx.all._
import simplefx.experimental._

object LinkHeaderTransformer {
  case class Link(name: String, prefix: String)

  def genLink(name: String, prefix: String) = new Link(name, prefix)

  def create(x: Link*): Transformer = create(x.toList)
  def create(x: java.util.List[Link]): Transformer = create(x.asScala.toList)
  def create(x: List[Link]): Transformer = ContainerTransformer.fromReactiveContainer(() => new LinkHeaderContainer(x))

  private class LinkHeaderContainer(links: List[Link]) extends VBox with ReactiveContainer {
    this <++ new HBox {
      styleClass ::= "linkheader-link-hbox"
      links.map { link =>
        this <++ new Label {
          styleClass ::= "linkheader-link"
          text <-- link.name
          when(request != null && request.getPath().startsWith(link.prefix)) ==> {
            styleClass <++ "selected"
          }
          LinkUtil.setLink(this, link.prefix, link.name)
        }
      }
    }
    this <++ new StackPane {
      javafx.scene.layout.VBox.setVgrow(this, Priority.ALWAYS)
      children <-- (if (content != null) List(content) else Nil)
    }
  }
}
