package one.jpro.platform.routing

object Transformer {
  def empty(): Transformer = route => route
}

@FunctionalInterface
trait Transformer {
    def apply(route: Route): Route

    def compose(y: Transformer): Transformer = r => this.apply(y.apply(r))
}
