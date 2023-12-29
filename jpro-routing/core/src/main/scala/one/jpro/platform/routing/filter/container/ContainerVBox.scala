package one.jpro.platform.routing.filter.container

import javafx.beans.property.{ObjectProperty, SimpleObjectProperty}
import javafx.scene.Node
import javafx.scene.layout.VBox
import one.jpro.platform.routing.Request

class ContainerVBox extends VBox with Container {

  private val content: ObjectProperty[Node] = new SimpleObjectProperty[Node](this, "content", null)
  private val request: ObjectProperty[Request] = new SimpleObjectProperty[Request](this, "request", null)
  override def contentProperty(): ObjectProperty[Node] = content

  override def requestProperty(): ObjectProperty[Request] = request
}
