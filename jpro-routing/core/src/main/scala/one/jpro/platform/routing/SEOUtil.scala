package one.jpro.platform.routing

import com.jpro.webapi.WebAPI
import javafx.scene.Node
import javafx.scene.control.Tooltip

object SEOUtil {

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
        val elem = webapi.wrapNode(tag, node)
        // Reset all default browser styles for heading elements to prevent them from affecting rendering.
        // The heading tags are only used for SEO semantics, not visual styling.
        webapi.js().eval(s"${elem.getName}.style.all = 'unset';")
    })
  }

}
