package one.jpro.platform.routing

import com.jpro.webapi.WebAPI
import javafx.scene.Node
import javafx.scene.control.Tooltip

class SEOUtil {

  def h1(x: Node): Unit = {
    wrapNode("h1", x)
  }
  def h2(x: Node): Unit = {
    wrapNode("h2", x)
  }
  def h3(x: Node): Unit = {
    wrapNode("h3", x)
  }

  def h4(x: Node): Unit = {
    wrapNode("h4", x)
  }
  def h5(x: Node): Unit = {
    wrapNode("h5", x)
  }
  def h6(x: Node): Unit = {
    wrapNode("h6", x)
  }

  def wrap(tag: String, node: Node): Unit = {
    wrapNode(tag, node)
  }

  private def wrapNode(tag: String, node: Node): Unit = {
    WebAPI.getWebAPI(node, webapi => {
        webapi.wrapNode(tag, node);
        webapi.getElement(node)
    })
  }

}
