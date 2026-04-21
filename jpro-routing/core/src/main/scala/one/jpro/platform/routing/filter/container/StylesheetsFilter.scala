package one.jpro.platform.routing.filter.container

import javafx.collections.{FXCollections, ListChangeListener, ObservableList, WeakListChangeListener}
import javafx.scene.Node
import javafx.scene.layout.StackPane

/**
 * A [[ContainerFilter]] that wraps the matched view in a `StackPane` whose
 * `getStylesheets()` mirrors a caller-supplied `ObservableList[String]`.
 * Stylesheets attach to the wrapper Parent (not the Scene), so CSS scoped to
 * one sub-route subtree cannot bleed into siblings. Mutating the source list
 * at runtime — desktop/mobile swap, theme switch — propagates immediately.
 */
class StylesheetsFilter(sheets: ObservableList[String]) extends ContainerFilter {

  /** Convenience constructor for fixed (non-reactive) stylesheet lists. */
  def this(sheets: String*) = this(FXCollections.observableArrayList(sheets: _*))

  override def createNode(): Node = new StylesheetsFilterContainer(sheets)

  override def setContent(container: Node, content: Node): Unit = {
    val stack = container.asInstanceOf[StackPane]
    stack.getChildren.setAll(content)
  }

  override def getContent(container: Node): Node = {
    val children = container.asInstanceOf[StackPane].getChildren
    if (children.isEmpty) null else children.get(0)
  }
}

/** Internal wrapper Node for [[StylesheetsFilter]]. The strong listener field lives
 *  on the container; the source observes it via `WeakListChangeListener`, so
 *  the source list does not pin the container in memory. */
private final class StylesheetsFilterContainer(source: ObservableList[String]) extends StackPane {
  getStylesheets.setAll(source)

  private val sourceListener: ListChangeListener[String] =
    (_: ListChangeListener.Change[_ <: String]) => getStylesheets.setAll(source)

  source.addListener(new WeakListChangeListener[String](sourceListener))
}
