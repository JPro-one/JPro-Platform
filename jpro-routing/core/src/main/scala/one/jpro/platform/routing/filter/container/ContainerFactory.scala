package one.jpro.platform.routing.filter.container

import javafx.scene.Node
import one.jpro.platform.routing.Request

trait ContainerFactory {
  def isContainer(x: Node): Boolean

  def createContainer(): Node

  def setContent(c: Node, x: Node): Unit

  def getContent(c: Node): Node

  def setRequest(c: Node, r: Request): Unit
}
