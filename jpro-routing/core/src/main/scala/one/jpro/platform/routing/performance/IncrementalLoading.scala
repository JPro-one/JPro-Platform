package one.jpro.platform.routing.performance

import com.jpro.webapi.WebAPI
import javafx.scene.Node
import one.jpro.platform.routing.SessionManagerContext
import one.jpro.platform.routing.sessionmanager.SessionManager
import simplefx.experimental._
import simplefx.core._
import simplefx.all._

object IncrementalLoading {

  /**
   * This should be called, before the node is added to the scene.
   */
  def loadNode(node: Node): Node = {
    node.setVisible(false)
    getContext(node).map { ctx =>
      println("get context finished")
      ctx.enqueueNode(node)
    }
    node
  }


  private class IncrementalLoader(node: Node) {
    var toMakeVisible: List[Node] = Nil

    def enqueueNode(node: Node): Unit = {
      toMakeVisible ::= node
      if(toMakeVisible.size == 1) startIncrementalLoading()
    }
    def startIncrementalLoading(): Unit = {
      if (WebAPI.isBrowser) {
        // We are sure the node is in the scene
        val webAPI = WebAPI.getWebAPI(node.scene)

        def makeNextVisible(): Unit = {
          println(" makeNextVisible " + toMakeVisible.size)
          webAPI.runAfterUpdate(new Runnable {
            override def run(): Unit = {
              if (!toMakeVisible.isEmpty) {
                toMakeVisible.reverse.head.setVisible(true)
                toMakeVisible = toMakeVisible.reverse.tail.reverse
                nextFrame --> {
                  makeNextVisible()
                }
              }
            }
          })
        }

        runLater(makeNextVisible())
      } else {
        toMakeVisible.map(_.setVisible(true))
        toMakeVisible = Nil
      }
    }
  }

  private object IncrementalLoadingKey extends AnyRef

  private def getContext(x: Node): FXFuture[IncrementalLoader] = {
    FXFuture.whenTrue(x.scene != null).map { _ =>
      val webAPI = WebAPI.getWebAPI(x.scene)

      val sm: SessionManager = SessionManagerContext.getContext(x)
      if(sm == null) throw new RuntimeException("No JPro Rotuing SessionManager found")
      val view = sm.view
      val content = view.realContent
      if(!content.getProperties.containsKey(IncrementalLoadingKey)) {
        val loader = new IncrementalLoader(content)
        content.getProperties.put(IncrementalLoadingKey, loader)
      }
      content.getProperties.get(IncrementalLoadingKey).asInstanceOf[IncrementalLoader]

    }
  }

}
