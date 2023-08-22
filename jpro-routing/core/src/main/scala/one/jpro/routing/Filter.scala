package one.jpro.routing

object Filter {
  def empty(): Filter = route => route
}

@FunctionalInterface
trait Filter {
    def apply(route: Route): Route

    def compose(y: Filter): Filter = r => this.apply(y.apply(r))
}
