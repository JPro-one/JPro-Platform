package one.jpro.routing.filter.container

import javafx.beans.property.{ObjectProperty, Property}
import one.jpro.routing.Request

trait Container {

  def contentProperty(): ObjectProperty[javafx.scene.Node]
  def getContent(): javafx.scene.Node = contentProperty().get()
  def setContent(x: javafx.scene.Node): Unit = contentProperty().set(x)

  def requestProperty(): ObjectProperty[Request]
  def getRequest(): Request = requestProperty().get()
  def setRequest(x: Request): Unit = requestProperty().set(x)

}
