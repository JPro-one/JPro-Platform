package one.jpro.platform.css

import javafx.beans.property.StringProperty
import javafx.scene.{Parent, Scene}
import one.jpro.platform.css.DynamicCSS._

/**
 * Java-friendly static API for dynamic CSS on Parent and Scene.
 */
object DynamicCSSUtil {

  def setCssString(scene: Scene, css: String): Unit = {
    scene.cssString = if (css == null) "" else css
  }

  def setCssString(parent: Parent, css: String): Unit = {
    parent.cssString = if (css == null) "" else css
  }

  def getCssString(scene: Scene): String = scene.cssString

  def getCssString(parent: Parent): String = parent.cssString

  def cssStringProperty(scene: Scene): StringProperty = scene.cssStringProperty

  def cssStringProperty(parent: Parent): StringProperty = parent.cssStringProperty
}
