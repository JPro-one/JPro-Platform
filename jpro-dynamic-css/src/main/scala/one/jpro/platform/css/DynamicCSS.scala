package one.jpro.platform.css

import simplefx.core._
import simplefx.all._
import simplefx.util.Predef._
import java.io.{File, PrintWriter}
import java.net.URL

object DynamicCSS {

  @extension class AddCSSStringParent(parent: Parent) {
    def isShowing = parent.scene != null && parent.scene.window != null && parent.scene.window.showing
    @Bind private var showingCSS = <--((this.isShowing, cssString))
    @Bind var cssString: String = "" <> {
      var previousURL: URL = null
      var previousKey: String = null
      updated(showingCSS --> { x =>
        val (showing: Boolean, cssString: String) = x
        if (previousURL != null) {
          parent.getStylesheets().remove(previousURL.toString)
          val pKey = previousKey
          previousURL = null
          previousKey = null
          runLater {
            InMemoryURL.unregister(pKey)
          }
        }
        if (showing) {
          val r = InMemoryURL.genDynamicContent(cssString)
          previousKey = r._1
          previousURL = r._2
          parent.getStylesheets().add(previousURL.toString)
        }
      })
    }
  }

  @extension class AddCSSStringScene(scene: Scene) {
    def isShowing = scene.window != null && scene.window.showing
    @Bind private var showingCSS = <--((this.isShowing, cssString))
    @Bind var cssString: String = "" <> {
      var previousURL: URL = null
      var previousKey: String = null
      updated(showingCSS --> { x =>
        val (showing: Boolean, cssString: String) = x
        if (previousURL != null) {
          scene.getStylesheets().remove(previousURL.toString)
          val pKey = previousKey
          previousURL = null
          previousKey = null
          runLater {
            InMemoryURL.unregister(pKey)
          }
        }
        if (showing) {
          val r = InMemoryURL.genDynamicContent(cssString)
          previousKey = r._1
          previousURL = r._2
          scene.getStylesheets().add(previousURL.toString)
        }
      })
    }
  }
}

object InMemoryURL {

  private var contentMap: Map[String, String] = Map()
  private var counter = 0

  private val files: File = {
    val f = File.createTempFile("cssfiles", "")
    f.delete()
    assert(f.mkdirs())
    f.deleteOnExit()
    f
  }

  def genDynamicContent(content: String): (String, URL) = synchronized {
    counter += 1
    val key = s"content$counter"
    (key, registerContent(key, content))
  }

  private def getFile(x: String) = new File(files, x + ".css")

  def registerContent(x: String, content: String): URL = synchronized {
    contentMap += (x -> content)
    val f = getFile(x)
    new PrintWriter(f) { write(content); close() }
    f.toURI.toURL
  }

  def unregister(x: String): Unit = synchronized {
    contentMap -= x
    getFile(x).delete()
  }

  def getContent(x: String): String = synchronized {
    contentMap(x)
  }
}
