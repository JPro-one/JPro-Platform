package one.jpro.platform.routing

import simplefx.core._
import simplefx.all._

trait NodeView extends View { this: Node =>
  final override def content = this
}
