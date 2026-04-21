package one.jpro.platform.routing.filter.container

import javafx.collections.{FXCollections, ListChangeListener, ObservableList, WeakListChangeListener}
import javafx.scene.Node
import javafx.scene.layout.StackPane

/**
 * A [[ContainerFilter]] that wraps the matched view in a `StackPane` whose
 * `getStyleClass()` mirrors a caller-supplied `ObservableList[String]`.
 *
 * The mirror image of [[StylesheetsFilter]] but for style classes instead of
 * stylesheet URLs. CSS rules using these classes (e.g. `.light .h1`) match
 * descendants of the wrapper because JavaFX selector evaluation walks
 * ancestors regardless of which Parent owns the stylesheet.
 *
 * Compose with [[StylesheetsFilter]] when you want both — they stack as
 * siblings in the route filter chain.
 */
class StyleClassFilter(classes: ObservableList[String]) extends ContainerFilter {

  /** Convenience constructor for fixed (non-reactive) style class lists. */
  def this(classes: String*) = this(FXCollections.observableArrayList(classes: _*))

  override def createNode(): Node = new StyleClassContainer(classes)

  override def setContent(container: Node, content: Node): Unit = {
    val stack = container.asInstanceOf[StackPane]
    stack.getChildren.setAll(content)
  }

  override def getContent(container: Node): Node = {
    val children = container.asInstanceOf[StackPane].getChildren
    if (children.isEmpty) null else children.get(0)
  }
}

/** Internal wrapper Node for [[StyleClassFilter]]. Same GC pattern as
 *  [[StylesheetsFilter]] — strong listener field on the container, observed
 *  via `WeakListChangeListener` so the source list doesn't pin the wrapper. */
private final class StyleClassContainer(source: ObservableList[String]) extends StackPane {
  getStyleClass.setAll(source)

  private val sourceListener: ListChangeListener[String] =
    (_: ListChangeListener.Change[_ <: String]) => getStyleClass.setAll(source)

  source.addListener(new WeakListChangeListener[String](sourceListener))
}
