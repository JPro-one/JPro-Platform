package one.jpro.routing

import com.jpro.webapi.WebAPI
import javafx.scene.input.Clipboard
import simplefx.core._
import simplefx.all._
import simplefx.util.ReflectionUtil._
import simplefx.util.Predef._
object CopyUtil {

  def setCopyOnClick(node: Node, text: String): Unit = {
    if(WebAPI.isBrowser) {
      import CopyJPro._
      node.setCopyOnClick(text)
    } else {
      import CopyDesktop._
      node.setCopyOnClick(text)
    }
    println("setCopyOnClick: " + node + ", " + text)
  }

  private object CopyDesktop {

      @extension
      class ExtendNodeWithCopy(node: Node) {

        @Bind var textToCopy = ""

        onMouseClicked --> {
          val clipboard = Clipboard.getSystemClipboard();
          val content = new ClipboardContent();
          content.putString(textToCopy);
          clipboard.setContent(content);
        }


        def setCopyOnClick(text: String): Unit = {
          textToCopy = text
        }
      }
  }

  private object CopyJPro {

    @extension
    class ExtendNodeWithCopy(node: Node) {
      def setCopyOnClick(text: String): Unit = {
        copyText = text
      }

      @Bind var copyText = ""

      WebAPI.getWebAPI(node, webapi => {
        val jsElem = webapi.getElement(node)
        copyText --> {
          val escapedText = copyText
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
          webapi.executeScript(
            s"""${jsElem.getName}.onmousedown = function(event) {
               |  console.log('copy: ${escapedText}');
               |  navigator.clipboard.writeText('${escapedText}');
               |};
               |""".stripMargin)
        }
      })
    }

  }
}
