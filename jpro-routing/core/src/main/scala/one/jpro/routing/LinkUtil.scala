package one.jpro.routing

import com.jpro.webapi.{HTMLView, WebAPI}
import simplefx.core._
import simplefx.all._
import simplefx.experimental._
import simplefx.util.ReflectionUtil._
import simplefx.util.Predef._

import java.net.{URI, URLEncoder}
import javafx.collections.ObservableList
import one.jpro.routing.sessionmanager.SessionManager

object LinkUtil {

  private var openLinkExternalFun: String => Unit = { link =>
    // Open link with awt
    import java.awt.Desktop
    import java.net.URI
    Desktop.getDesktop.browse(new URI(link))
  }
  def setOpenLinkExternalFun(x: String => Unit): Unit = {
    openLinkExternalFun = x
  }

  def isValidLink(x: String): Boolean = {
    try {
      val uri = new URI(x)
      true
    } catch {
      case _: Throwable => false
    }
  }

  def getSessionManager(node: Node): SessionManager = {
    SessionManagerContext.getContext(node)
  }

  def setLink(node: Node, url: String): Unit = {
    setLink(node,url,None)
  }
  def setLink(node: Node, url: String, text: String): Unit = {
    setLink(node,url,Some(text))
  }
  def setLink(node: Node, url: String, text: Option[String] = None, external: Boolean = false): Unit = {
    assert(url != "", s"Empty link: ''")
    assert(isValidLink(url), s"Invalid link: '$url''")
    node.getProperties.put("link",url)
    text.map {desc =>
      node.getProperties.put("description",desc)
    }
    if(url == null || url.startsWith("/") || url.startsWith("./") || url.startsWith("../")) {
      setLinkInternalPush(node,url, text, external)
    } else {
      setLinkInternalNoPush(node,url, text, external)
    }
  }

  def setExternalLink(node: Node, url: String): Unit = {
    setLink(node,url,None, true)
  }
  def setExternalLink(node: Node, url: String, text: String): Unit = {
    setLink(node,url,Some(text), true)
  }
  def setLinkInternalPush(node: Node, url: String, text: Option[String] = None, external: Boolean = false) = {
    node.cursor = javafx.scene.Cursor.HAND
    setLinkSimple(url, text, true, external)(node)
  }
  def setLinkInternalNoPush(node: Node, url: String, text: Option[String] = None, external: Boolean = false) = {
    node.cursor = javafx.scene.Cursor.HAND
    setLinkSimple(url, text, false, external)(node)
  }

  def goBack(node: Node): Unit = {
    SessionManagerContext.getContext(node).goBack()
  }

  def goForward(node: Node): Unit = {
    SessionManagerContext.getContext(node).goForward()
  }
  def gotoPage(node: Node, url: String) = {
    LinkUtil.getSessionManager(node).gotoURL(url)
  }
  def getCurrentPage(node: Node): String = {
    LinkUtil.getSessionManager(node).url
  }
  def refresh(node: Node): Unit = {
    val man = LinkUtil.getSessionManager(node)
    assert(man.url != null, "current url was null")
    man.gotoURL(man.url)
  }

  private object LinkDesktop {
    @extension
    class ExtendNodeWithLink(node: Node) {
      def setNewLink(link: String, text: Option[String], pushState: Boolean, external: Boolean): Unit = {
        if (link != null && !isValidLink(link)) {
          println("Warning, link is not valid: " + link)
        }
        this.pushState = pushState
        this.external = external
        this.link = link
        this.text = text
      }

      @Bind var external: Boolean = true
      @Bind var pushState: Boolean = false
      @Bind var link: String = ""
      @Bind var text: Option[String] = None

      if(!WebAPI.isBrowser) {
        node.onMouseClicked --> { e =>
          def isExternalLink(x: String) = x.startsWith("http") || x.startsWith("mailto")
          if(e.isStillSincePress) {
            if(isExternalLink(link)) {
              openLinkExternalFun(link)
            } else {
              LinkUtil.getSessionManager(node).gotoURL(link)
            }
          }
        }
      }
    }
  }

  private object LinkJPro {

    @extension
    class ExtendNodeWithLink(node: Node) {

      def setNewLink(link: String, text: Option[String], pushState: Boolean, external: Boolean): Unit = {
        if (link != null && !isValidLink(link)) {
          println("Warning, link is not valid: " + link)
        }

        this.pushState = pushState
        this.external = external
        this.link = link
        this.text = text
      }

      @Bind var external: Boolean = true
      @Bind var pushState: Boolean = false
      @Bind var link: String = ""
      @Bind var text: Option[String] = None

      WebAPI.getWebAPI(node, webapi => {

        val aElem = webapi.wrapNode("a", node)

        val divBox = webapi.executeScriptWithVariable("document.createElement('div')");
        webapi.executeScript(
          s"""
             |${aElem.getName}.appendChild(${divBox.getName});
             |${divBox.getName}.style.display = 'block';
             |${divBox.getName}.style.position = 'absolute';
             |""".stripMargin)
        node.labWH --> { wh =>
          // set WH to a
          webapi.executeScript(
            s"""
               |${divBox.getName}.style.width = '${wh._1}px';
               |${divBox.getName}.style.height = '${wh._2}px';
               |""".stripMargin)
        }


        @Bind var linkAndPush = <-- (link, pushState)
        linkAndPush --> { case(link, pushState) =>
          def script = if (pushState) {
            s"""${aElem.getName}.onclick =  function(event) {
               |  if(!event.shiftKey && !event.metaKey) {
               |    jpro.jproGotoURL(\"${link.replace("\"", "\\\"")}\"); event.preventDefault();
               |  }
               |};""".stripMargin
          } else s"${aElem.getName}.onclick = null;"
          webapi.executeScript(
            s"""${aElem.getName}.href = '${link.replace(" ", "%20").replace("'", "\\'")}';
               |$script
               |""".stripMargin)
        }

        external --> { external =>
          if(external) {
            webapi.executeScript(
              s"""${aElem.getName}.target = '_blank';""".stripMargin)
          } else {
            webapi.executeScript(
              s"""${aElem.getName}.removeAttribute("target");""".stripMargin)
          }
        }
        text --> { x =>
          // have to escape ' in text
          if(x.isDefined) {
            val escapedText = x.get.replace("'","\\'")
            webapi.executeScript(
              s"""${aElem.getName}.title = '${escapedText}';""".stripMargin)
          } else {
            webapi.executeScript(
              s"""${aElem.getName}.removeAttribute("title");""".stripMargin)
          }
        }

      })
    }

  }


  private def setLinkSimple(url: String, text: Option[String], pushState: Boolean, external: Boolean)(theNode: Node) = {
    if(!WebAPI.isBrowser) {
      import LinkDesktop._
      theNode.setNewLink(url,text,pushState,
        external)
    } else {
      import LinkJPro._
      theNode.setNewLink(url, text, pushState,
        external)
    }
  }

  def setImageViewDescription(view: ImageView, description: String): Unit = {
    view.setAccessibleRoleDescription(description)
  }
}
