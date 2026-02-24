package one.jpro.platform.routing.context

import simplefx.core._
import simplefx.all._
import simplefx.util.Predef.extension
import javafx.css.Styleable

class ContextManager[A] {

  @extension class ExtWithSession(x: Node) {
    @Bind private[context] var context: A = null

    private def printDebugInfo(): Unit = {
      println(" - ")
      println(s"getContext for node: $x")
      println(s"getParent: ${x.getParent}")
      println(s"getStyleableParent: ${x.getStyleableParent}")
    }

    private[context] def getContext: A = {
      //printDebugInfo()
      if(context != null) context
      else getStyleableParentContext(x).getOrElse(throw new Exception("Couldn't find SessionManager!"))
    }

    private def getStyleableParentContext(styleable: Styleable): Option[A] = {
      styleable.getStyleableParent match {
        case null => None
        case node: Node =>
          val nodeContext = node.getContext
          if(nodeContext != null) Some(nodeContext)
          else getStyleableParentContext(node)
        case _ => getStyleableParentContext(styleable.getStyleableParent)
      }
    }
  }

  def setContext(x: Node, y: A): Unit = x.context = y

  def getContext(x: Node): A = x.getContext
}
